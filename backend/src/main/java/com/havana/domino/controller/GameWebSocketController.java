package com.havana.domino.controller;

import com.havana.domino.dto.GameRoomDTO;
import com.havana.domino.dto.PassMoveRequest;
import com.havana.domino.dto.PlayMoveRequest;
import com.havana.domino.model.GameRoom;
import com.havana.domino.model.GameStatus;
import com.havana.domino.model.Player;
import com.havana.domino.service.AiPlayerService;
import com.havana.domino.service.GameEngineService;
import com.havana.domino.service.MatchmakingService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * WebSocket 遊戲操作控制器。
 *
 * <pre>
 * /app/game.play    出牌
 * /app/game.pass    過牌
 * </pre>
 *
 * 所有操作完成後廣播更新至 /topic/room/{roomId}。
 * 包含勝利與死局（Trancado）判定。
 */
@Controller
public class GameWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(GameWebSocketController.class);

    /** AI 連鎖行動之間的短暫延遲（毫秒），讓前端有時間渲染更新 */
    private static final long AI_MOVE_DELAY_MS = 600;

    private final GameEngineService      gameEngineService;
    private final MatchmakingService     matchmakingService;
    private final SimpMessagingTemplate  messagingTemplate;
    private final AiPlayerService        aiPlayerService;

    public GameWebSocketController(GameEngineService gameEngineService,
                                   MatchmakingService matchmakingService,
                                   SimpMessagingTemplate messagingTemplate,
                                   AiPlayerService aiPlayerService) {
        this.gameEngineService  = gameEngineService;
        this.matchmakingService = matchmakingService;
        this.messagingTemplate  = messagingTemplate;
        this.aiPlayerService    = aiPlayerService;
    }

    // ── WebSocket 訊息處理 ────────────────────────────────────────────────────

    /**
     * 處理出牌請求。
     * 人類出牌後若輪到 AI，自動連鎖執行直到輪回人類或遊戲結束。
     */
    @MessageMapping("/game.play")
    public void playTile(@Valid PlayMoveRequest request) {
        try {
            log.info("收到出牌請求：roomId={}, playerId={}, tileId={}, targetEnd={}",
                    request.roomId(), request.playerId(), request.tileId(), request.targetEnd());

            GameRoom room = matchmakingService.getRoom(request.roomId());

            if (room.getStatus() != GameStatus.IN_PROGRESS) {
                sendError(request.roomId(), "遊戲尚未開始或已結束");
                return;
            }

            GameRoom updated = gameEngineService.playTile(
                    room, request.playerId(), request.tileId(), request.targetEnd());

            updated = resolveEndCondition(updated);
            broadcastRoomUpdate(updated,
                    updated.isTrancado() ? gameEngineService.calculateTrancadoScores(updated) : null);

            if (updated.getStatus() == GameStatus.IN_PROGRESS) {
                triggerAiChain(updated);
            }

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("出牌失敗：{}", e.getMessage());
            sendError(request.roomId(), e.getMessage());
        } catch (Exception e) {
            log.error("處理出牌時發生錯誤", e);
            sendError(request.roomId(), "伺服器內部錯誤");
        }
    }

    /**
     * 處理過牌請求。
     * 人類過牌後若輪到 AI，自動連鎖執行直到輪回人類或遊戲結束。
     */
    @MessageMapping("/game.pass")
    public void passTurn(@Valid PassMoveRequest request) {
        try {
            log.info("收到過牌請求：roomId={}, playerId={}", request.roomId(), request.playerId());

            GameRoom room = matchmakingService.getRoom(request.roomId());

            if (room.getStatus() != GameStatus.IN_PROGRESS) {
                sendError(request.roomId(), "遊戲尚未開始或已結束");
                return;
            }

            GameRoom updated = gameEngineService.passTurn(room, request.playerId());

            updated = resolveEndCondition(updated);
            broadcastRoomUpdate(updated,
                    updated.isTrancado() ? gameEngineService.calculateTrancadoScores(updated) : null);

            if (updated.getStatus() == GameStatus.IN_PROGRESS) {
                triggerAiChain(updated);
            }

        } catch (IllegalStateException | IllegalArgumentException e) {
            log.warn("過牌失敗：{}", e.getMessage());
            sendError(request.roomId(), e.getMessage());
        } catch (Exception e) {
            log.error("處理過牌時發生錯誤", e);
            sendError(request.roomId(), "伺服器內部錯誤");
        }
    }

    // ── 私有輔助方法 ──────────────────────────────────────────────────────────

    /** 檢查勝利和死局，更新 GameRoom 狀態後回傳。 */
    private GameRoom resolveEndCondition(GameRoom room) {
        gameEngineService.checkWinner(room).ifPresent(winner -> {
            room.setWinnerId(winner.getId());
            room.setStatus(GameStatus.FINISHED);
            log.info("遊戲結束：玩家 {} 獲勝！", winner.getName());
        });

        if (room.getStatus() == GameStatus.FINISHED) {
            return room;
        }

        if (gameEngineService.isTrancado(room)) {
            Map<String, Integer> scores = gameEngineService.calculateTrancadoScores(room);
            room.setTrancado(true);
            room.setStatus(GameStatus.FINISHED);
            String trancadoWinnerId = scores.entrySet().stream()
                    .min(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
            room.setWinnerId(trancadoWinnerId);
            log.info("死局（Trancado）發生，分數：{}", scores);
        }
        return room;
    }

    /**
     * AI 連鎖行動：若目前輪到 AI 玩家，連續執行直到輪回人類或遊戲結束。
     * 防死循環保護：最多連續執行 players.size() 次。
     */
    private void triggerAiChain(GameRoom room) {
        int maxChain = room.getPlayers().size();

        for (int i = 0; i < maxChain; i++) {
            if (room.getStatus() != GameStatus.IN_PROGRESS) break;

            Player current = room.currentPlayer();
            if (!current.isAi()) break;

            try {
                Thread.sleep(AI_MOVE_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            log.info("AI [{}] 開始行動（roomId={}）", current.getName(), room.getRoomId());
            try {
                room = aiPlayerService.executeAction(room, current.getId());
                room = resolveEndCondition(room);

                Map<String, Integer> scores = room.isTrancado()
                        ? gameEngineService.calculateTrancadoScores(room) : null;
                broadcastRoomUpdate(room, scores);

            } catch (Exception e) {
                log.error("AI 行動發生錯誤（playerId={}）", current.getId(), e);
                sendError(room.getRoomId(), "AI 行動發生錯誤");
                break;
            }
        }
    }

    /** 廣播房間狀態更新至所有訂閱者。 */
    private void broadcastRoomUpdate(GameRoom room, Map<String, Integer> trancadoScores) {
        GameRoomDTO dto = GameRoomDTO.from(room, trancadoScores);
        String topic = "/topic/room/" + room.getRoomId();
        messagingTemplate.convertAndSend(topic, dto);
        log.debug("廣播房間更新至 {}", topic);
    }

    /** 發送錯誤訊息至特定房間訂閱者。 */
    private void sendError(String roomId, String errorMessage) {
        Map<String, String> error = Map.of(
                "error", "OPERATION_FAILED",
                "message", errorMessage
        );
        messagingTemplate.convertAndSend("/topic/room/" + roomId, error);
    }
}
