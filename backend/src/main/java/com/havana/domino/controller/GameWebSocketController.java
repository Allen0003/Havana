package com.havana.domino.controller;

import com.havana.domino.dto.GameRoomDTO;
import com.havana.domino.dto.PassMoveRequest;
import com.havana.domino.dto.PlayMoveRequest;
import com.havana.domino.model.GameRoom;
import com.havana.domino.model.GameStatus;
import com.havana.domino.model.Player;
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

    /**
     * 處理出牌請求。
     * <p>M3-1 + M3-3：驗證並執行出牌，檢查勝利/死局，廣播更新。</p>
     */
    @MessageMapping("/game.play")
    public void playTile(@Valid PlayMoveRequest request) {
        try {
            log.info("收到出牌請求：roomId={}, playerId={}, tileId={}, targetEnd={}",
                    request.roomId(), request.playerId(), request.tileId(), request.targetEnd());

            // 1. 取得房間
            GameRoom room = matchmakingService.getRoom(request.roomId());

            // 2. 驗證遊戲狀態
            if (room.getStatus() != GameStatus.IN_PROGRESS) {
                sendError(request.roomId(), "遊戲尚未開始或已結束");
                return;
            }

            // 3. 執行出牌
            GameRoom updatedRoom = gameEngineService.playTile(
                    room,
                    request.playerId(),
                    request.tileId(),
                    request.targetEnd()
            );

            // 4. 檢查勝利條件
            Optional<Player> winner = gameEngineService.checkWinner(updatedRoom);
            if (winner.isPresent()) {
                updatedRoom.setWinnerId(winner.get().getId());
                updatedRoom.setStatus(GameStatus.FINISHED);
                log.info("遊戲結束：玩家 {} 獲勝！", winner.get().getName());
            } else {
                // 5. 檢查死局（Trancado）
                if (gameEngineService.isTrancado(updatedRoom)) {
                    Map<String, Integer> scores = gameEngineService.calculateTrancadoScores(updatedRoom);
                    updatedRoom.setTrancado(true);
                    updatedRoom.setStatus(GameStatus.FINISHED);

                    // 找出最低分數的玩家作為勝者
                    String trancadoWinnerId = scores.entrySet().stream()
                            .min(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey)
                            .orElse(null);
                    updatedRoom.setWinnerId(trancadoWinnerId);

                    log.info("死局（Trancado）發生，分數：{}", scores);
                    broadcastRoomUpdate(updatedRoom, scores);
                    return;
                }
            }

            // 6. 廣播更新
            broadcastRoomUpdate(updatedRoom, null);

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
     */
    @MessageMapping("/game.pass")
    public void passTurn(@Valid PassMoveRequest request) {
        try {
            log.info("收到過牌請求：roomId={}, playerId={}", request.roomId(), request.playerId());

            // 1. 取得房間
            GameRoom room = matchmakingService.getRoom(request.roomId());

            // 2. 驗證遊戲狀態
            if (room.getStatus() != GameStatus.IN_PROGRESS) {
                sendError(request.roomId(), "遊戲尚未開始或已結束");
                return;
            }

            // 3. 執行過牌
            GameRoom updatedRoom = gameEngineService.passTurn(room, request.playerId());

            // 4. 檢查死局（過牌後可能觸發死局）
            if (gameEngineService.isTrancado(updatedRoom)) {
                Map<String, Integer> scores = gameEngineService.calculateTrancadoScores(updatedRoom);
                updatedRoom.setTrancado(true);
                updatedRoom.setStatus(GameStatus.FINISHED);

                // 找出最低分數的玩家作為勝者
                String trancadoWinnerId = scores.entrySet().stream()
                        .min(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse(null);
                updatedRoom.setWinnerId(trancadoWinnerId);

                log.info("死局（Trancado）發生，分數：{}", scores);
                broadcastRoomUpdate(updatedRoom, scores);
                return;
            }

            // 5. 廣播更新
            broadcastRoomUpdate(updatedRoom, null);

        } catch (IllegalStateException | IllegalArgumentException e) {
            log.warn("過牌失敗：{}", e.getMessage());
            sendError(request.roomId(), e.getMessage());
        } catch (Exception e) {
            log.error("處理過牌時發生錯誤", e);
            sendError(request.roomId(), "伺服器內部錯誤");
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
