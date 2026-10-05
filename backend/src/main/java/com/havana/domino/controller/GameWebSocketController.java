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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.Optional;
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
@RequiredArgsConstructor
@Slf4j
public class GameWebSocketController {

    private final GameEngineService gameEngineService;
    private final MatchmakingService matchmakingService;
    private final SimpMessagingTemplate messagingTemplate;
    private final AiPlayerService aiPlayerService;

    /** AI 連鎖行動之間的短暫延遲（毫秒），讓前端有時間渲染更新 */
    private static final long AI_MOVE_DELAY_MS = 600;

    /**
     * 處理出牌請求。
     * <p>M3-1 + M3-3：驗證並執行出牌，檢查勝利/死局，廣播更新。</p>
     * <p>若輪到 AI 玩家，自動連鎖執行直到輪回人類或遊戲結束。</p>
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

            GameRoom updatedRoom = gameEngineService.playTile(
                    room, request.playerId(), request.tileId(), request.targetEnd());

            updatedRoom = resolveEndCondition(updatedRoom);
            broadcastRoomUpdate(updatedRoom, updatedRoom.isTrancado()
                    ? gameEngineService.calculateTrancadoScores(updatedRoom) : null);

            // 若遊戲繼續且輪到 AI，觸發 AI 連鎖行動
            if (updatedRoom.getStatus() == GameStatus.IN_PROGRESS) {
                triggerAiChain(updatedRoom);
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
     * <p>M3-2 + M3-3：驗證並執行過牌，檢查死局，廣播更新。</p>
     * <p>若輪到 AI 玩家，自動連鎖執行直到輪回人類或遊戲結束。</p>
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

            GameRoom updatedRoom = gameEngineService.passTurn(room, request.playerId());

            updatedRoom = resolveEndCondition(updatedRoom);
            broadcastRoomUpdate(updatedRoom, updatedRoom.isTrancado()
                    ? gameEngineService.calculateTrancadoScores(updatedRoom) : null);

            if (updatedRoom.getStatus() == GameStatus.IN_PROGRESS) {
                triggerAiChain(updatedRoom);
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

    /**
     * 檢查勝利和死局，更新 GameRoom 狀態。
     * 回傳更新後的 room（若已結束則 status = FINISHED）。
     */
    private GameRoom resolveEndCondition(GameRoom room) {
        // 檢查勝利
        gameEngineService.checkWinner(room).ifPresent(winner -> {
            room.setWinnerId(winner.getId());
            room.setStatus(GameStatus.FINISHED);
            log.info("遊戲結束：玩家 {} 獲勝！", winner.getName());
        });

        if (room.getStatus() == GameStatus.FINISHED) {
            return room;
        }

        // 檢查死局
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
     * 每次 AI 行動後都廣播一次更新，並加入短暫延遲讓前端有時間渲染。
     *
     * <p>防死循環保護：最多連續執行 players.size() 次，避免全 AI 房間無限迴圈。</p>
     */
    private void triggerAiChain(GameRoom room) {
        int maxChain = room.getPlayers().size();

        for (int i = 0; i < maxChain; i++) {
            if (room.getStatus() != GameStatus.IN_PROGRESS) break;

            Player current = room.currentPlayer();
            if (!current.isAi()) break; // 輪到人類，停止

            // AI 行動前短暫等待，讓前端感受到流程
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

                // 每次 AI 行動後立即廣播
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

    /**
     * 廣播房間狀態更新至所有訂閱者。
     */
    private void broadcastRoomUpdate(GameRoom room, Map<String, Integer> trancadoScores) {
        GameRoomDTO dto = GameRoomDTO.from(room, trancadoScores);
        String topic = "/topic/room/" + room.getRoomId();
        messagingTemplate.convertAndSend(topic, dto);
        log.debug("廣播房間更新至 {}", topic);
    }

    /**
     * 發送錯誤訊息至特定房間訂閱者。
     */
    private void sendError(String roomId, String errorMessage) {
        Map<String, String> error = Map.of(
                "error", "OPERATION_FAILED",
                "message", errorMessage
        );
        messagingTemplate.convertAndSend("/topic/room/" + roomId, error);
    }
}
