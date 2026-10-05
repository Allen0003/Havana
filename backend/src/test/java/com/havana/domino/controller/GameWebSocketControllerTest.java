package com.havana.domino.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.havana.domino.dto.GameRoomDTO;
import com.havana.domino.dto.PassMoveRequest;
import com.havana.domino.dto.PlayMoveRequest;
import com.havana.domino.model.BoardEnd;
import com.havana.domino.model.GameStatus;
import com.havana.domino.model.Tile;
import com.havana.domino.service.GameEngineService;
import com.havana.domino.service.MatchmakingService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Assumptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

/**
 * GameWebSocketController 整合測試（M4-1）。
 *
 * <p>使用 @SpringBootTest 啟動真實 Tomcat，透過 WebSocketStompClient
 * 建立 SockJS/STOMP 連線，驗證完整遊戲流程：
 * <ul>
 *   <li>出牌後廣播更新 GameRoomDTO</li>
 *   <li>非法出牌回傳錯誤訊息</li>
 *   <li>過牌後廣播更新</li>
 *   <li>手牌清空後廣播勝利狀態</li>
 *   <li>所有玩家無法出牌後廣播死局（Trancado）狀態</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("GameWebSocketController 整合測試 (M4-1)")
class GameWebSocketControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private MatchmakingService matchmakingService;

    @Autowired
    private GameEngineService gameEngineService;

    @Autowired
    private ObjectMapper objectMapper;

    private WebSocketStompClient stompClient;

    private static final long TIMEOUT_SEC = 5;

    @BeforeEach
    void setUp() {
        stompClient = new WebSocketStompClient(
                new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient())))
        );
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    @AfterEach
    void tearDown() {
        stompClient.stop();
    }

    // ── 輔助方法 ─────────────────────────────────────────────────────────────

    /** 建立 STOMP 連線，回傳 session */
    private StompSession connect() throws Exception {
        String url = "ws://localhost:" + port + "/ws-havana";
        CompletableFuture<StompSession> future = new CompletableFuture<>();
        stompClient.connectAsync(url, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                future.complete(session);
            }
            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                future.completeExceptionally(exception);
            }
        });
        return future.get(TIMEOUT_SEC, TimeUnit.SECONDS);
    }

    /** 訂閱房間 topic，回傳收到第一筆訊息的 CompletableFuture */
    private CompletableFuture<Map<String, Object>> subscribeRoom(
            StompSession session, String roomId) {

        CompletableFuture<Map<String, Object>> result = new CompletableFuture<>();
        session.subscribe("/topic/room/" + roomId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }
            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                result.complete((Map<String, Object>) payload);
            }
        });
        return result;
    }

    /** 發送出牌請求 */
    private void sendPlay(StompSession session, String roomId, String playerId,
                          int tileId, BoardEnd end) throws Exception {
        PlayMoveRequest req = new PlayMoveRequest(roomId, playerId, tileId, end);
        session.send("/app/game.play", req);
    }

    /** 發送過牌請求 */
    private void sendPass(StompSession session, String roomId, String playerId) throws Exception {
        PassMoveRequest req = new PassMoveRequest(roomId, playerId);
        session.send("/app/game.pass", req);
    }

    /** 建立 4 人滿員房間並取得各玩家 ID */
    private String[] createFullRoom() {
        var room = matchmakingService.createRoom("Alice");
        String roomId = room.getRoomId();
        matchmakingService.joinRoom(roomId, "Bob");
        matchmakingService.joinRoom(roomId, "Carol");
        matchmakingService.joinRoom(roomId, "Dave");

        // 取得更新後的房間
        room = matchmakingService.getRoom(roomId);
        String[] ids = new String[5]; // [0]=roomId, [1..4]=playerIds
        ids[0] = roomId;
        for (int i = 0; i < 4; i++) {
            ids[i + 1] = room.getPlayers().get(i).getId();
        }
        return ids;
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("出牌（/app/game.play）")
    class PlayTile {

        @Test
        @DisplayName("合法出牌 → 廣播更新的 GameRoomDTO，boardTiles 長度增加")
        void playTile_valid_broadcastsUpdate() throws Exception {
            String[] ctx = createFullRoom();
            String roomId = ctx[0];

            StompSession session = connect();
            CompletableFuture<Map<String, Object>> future = subscribeRoom(session, roomId);

            // 找到當前輪次玩家和他的 [9|9]
            var room = matchmakingService.getRoom(roomId);
            String firstPlayerId = room.currentPlayer().getId();
            int doubleNineTileId = room.currentPlayer().getHand().stream()
                    .filter(t -> t.left() == 9 && t.right() == 9)
                    .findFirst()
                    .map(Tile::id)
                    .orElseThrow();

            sendPlay(session, roomId, firstPlayerId, doubleNineTileId, BoardEnd.LEFT);

            Map<String, Object> payload = future.get(TIMEOUT_SEC, TimeUnit.SECONDS);

            assertThat(payload.get("roomId")).isEqualTo(roomId);
            assertThat(payload.get("status")).isEqualTo("IN_PROGRESS");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> boardTiles = (List<Map<String, Object>>) payload.get("boardTiles");
            assertThat(boardTiles).hasSize(1);
            assertThat(payload.get("leftEnd")).isEqualTo(9);
            assertThat(payload.get("rightEnd")).isEqualTo(9);
        }

        @Test
        @DisplayName("非法出牌（非本人回合）→ 廣播錯誤訊息")
        void playTile_wrongTurn_broadcastsError() throws Exception {
            String[] ctx = createFullRoom();
            String roomId = ctx[0];

            StompSession session = connect();
            CompletableFuture<Map<String, Object>> future = subscribeRoom(session, roomId);

            // 找出「非當前輪次」的玩家
            var room = matchmakingService.getRoom(roomId);
            int turnIdx = room.getCurrentTurnIndex();
            int wrongIdx = (turnIdx + 1) % 4;
            String wrongPlayerId = room.getPlayers().get(wrongIdx).getId();
            int tileId = room.getPlayers().get(wrongIdx).getHand().get(0).id();

            sendPlay(session, roomId, wrongPlayerId, tileId, BoardEnd.LEFT);

            Map<String, Object> payload = future.get(TIMEOUT_SEC, TimeUnit.SECONDS);

            assertThat(payload).containsKey("error");
            assertThat(payload.get("error").toString()).isEqualTo("OPERATION_FAILED");
        }

        @Test
        @DisplayName("非法出牌（牌不匹配）→ 廣播錯誤訊息")
        void playTile_tileNotMatch_broadcastsError() throws Exception {
            String[] ctx = createFullRoom();
            String roomId = ctx[0];

            // 先讓第一個玩家出 [9|9]
            var room = matchmakingService.getRoom(roomId);
            String firstId = room.currentPlayer().getId();
            int doubleNineTileId = room.currentPlayer().getHand().stream()
                    .filter(t -> t.left() == 9 && t.right() == 9)
                    .map(Tile::id).findFirst().orElseThrow();

            StompSession session = connect();

            // 第一次出牌
            CompletableFuture<Map<String, Object>> firstFuture = subscribeRoom(session, roomId);
            sendPlay(session, roomId, firstId, doubleNineTileId, BoardEnd.LEFT);
            firstFuture.get(TIMEOUT_SEC, TimeUnit.SECONDS); // 等待廣播

            // 重新訂閱接第二次廣播（錯誤）
            CompletableFuture<Map<String, Object>> errorFuture = subscribeRoom(session, roomId);

            // 第二個玩家出一張無法匹配的牌（點數不是 9）
            room = matchmakingService.getRoom(roomId);
            String secondId = room.currentPlayer().getId();
            Tile nonMatchTile = room.currentPlayer().getHand().stream()
                    .filter(t -> !t.canMatch(9))
                    .findFirst()
                    .orElse(null);

            // 若所有手牌都能匹配 9，則跳過此測試（機率極低）
            Assumptions.assumeTrue(nonMatchTile != null, "Skip: all tiles can match 9");

            sendPlay(session, roomId, secondId, nonMatchTile.id(), BoardEnd.LEFT);

            Map<String, Object> payload = errorFuture.get(TIMEOUT_SEC, TimeUnit.SECONDS);
            assertThat(payload).containsKey("error");
        }
    }

    @Nested
    @DisplayName("過牌（/app/game.pass）")
    class PassTurn {

        @Test
        @DisplayName("合法過牌（手牌無法匹配）→ 廣播更新，回合推進")
        void passTurn_valid_broadcastsUpdate() throws Exception {
            String[] ctx = createFullRoom();
            String roomId = ctx[0];

            // 先出 [9|9]
            var room = matchmakingService.getRoom(roomId);
            String firstId = room.currentPlayer().getId();
            int doubleNineId = room.currentPlayer().getHand().stream()
                    .filter(t -> t.left() == 9 && t.right() == 9)
                    .map(Tile::id).findFirst().orElseThrow();

            StompSession session = connect();

            CompletableFuture<Map<String, Object>> playFuture = subscribeRoom(session, roomId);
            sendPlay(session, roomId, firstId, doubleNineId, BoardEnd.LEFT);
            playFuture.get(TIMEOUT_SEC, TimeUnit.SECONDS);

            // 找第二個玩家，確認他沒有可匹配 9 的牌才能過牌
            room = matchmakingService.getRoom(roomId);
            String secondId = room.currentPlayer().getId();
            boolean hasMatch = room.currentPlayer().getHand().stream()
                    .anyMatch(t -> t.canMatch(9));

            // 若有牌可出則跳過此測試
            Assumptions.assumeFalse(hasMatch, "Skip: player has a matching tile");

            CompletableFuture<Map<String, Object>> passFuture = subscribeRoom(session, roomId);
            sendPass(session, roomId, secondId);

            Map<String, Object> payload = passFuture.get(TIMEOUT_SEC, TimeUnit.SECONDS);

            assertThat(payload.get("roomId")).isEqualTo(roomId);
            // 回合已推進，currentTurnPlayerId 不再是 secondId
            assertThat(payload.get("currentTurnPlayerId")).isNotEqualTo(secondId);
        }

        @Test
        @DisplayName("非法過牌（有牌可出）→ 廣播錯誤訊息")
        void passTurn_canPlay_broadcastsError() throws Exception {
            String[] ctx = createFullRoom();
            String roomId = ctx[0];

            // 先出 [9|9]
            var room = matchmakingService.getRoom(roomId);
            String firstId = room.currentPlayer().getId();
            int doubleNineId = room.currentPlayer().getHand().stream()
                    .filter(t -> t.left() == 9 && t.right() == 9)
                    .map(Tile::id).findFirst().orElseThrow();

            StompSession session = connect();
            CompletableFuture<Map<String, Object>> playFuture = subscribeRoom(session, roomId);
            sendPlay(session, roomId, firstId, doubleNineId, BoardEnd.LEFT);
            playFuture.get(TIMEOUT_SEC, TimeUnit.SECONDS);

            // 第二個玩家嘗試過牌，但他有牌可出
            room = matchmakingService.getRoom(roomId);
            String secondId = room.currentPlayer().getId();
            boolean hasMatch = room.currentPlayer().getHand().stream()
                    .anyMatch(t -> t.canMatch(9));

            Assumptions.assumeTrue(hasMatch, "Skip: player has no matching tile");

            CompletableFuture<Map<String, Object>> errorFuture = subscribeRoom(session, roomId);
            sendPass(session, roomId, secondId);

            Map<String, Object> payload = errorFuture.get(TIMEOUT_SEC, TimeUnit.SECONDS);
            assertThat(payload).containsKey("error");
        }
    }

    @Nested
    @DisplayName("勝利條件")
    class Victory {

        @Test
        @DisplayName("手牌清空後 → 廣播 FINISHED 狀態且包含 winnerId")
        void winner_broadcastsFinishedWithWinnerId() throws Exception {
            String[] ctx = createFullRoom();
            String roomId = ctx[0];

            var room = matchmakingService.getRoom(roomId);

            // 找到目前輪次玩家
            String firstId = room.currentPlayer().getId();
            int doubleNineId = room.currentPlayer().getHand().stream()
                    .filter(t -> t.left() == 9 && t.right() == 9)
                    .map(Tile::id).findFirst().orElseThrow();

            // 把除了 [9|9] 以外的所有手牌清空，模擬只剩一張牌即將獲勝
            room.currentPlayer().getHand().removeIf(t -> t.id() != doubleNineId);

            StompSession session = connect();
            CompletableFuture<Map<String, Object>> future = subscribeRoom(session, roomId);
            sendPlay(session, roomId, firstId, doubleNineId, BoardEnd.LEFT);

            Map<String, Object> payload = future.get(TIMEOUT_SEC, TimeUnit.SECONDS);

            assertThat(payload.get("status")).isEqualTo("FINISHED");
            assertThat(payload.get("winnerId")).isEqualTo(firstId);
            assertThat(payload.get("trancado")).isEqualTo(false);
        }
    }

    @Nested
    @DisplayName("死局（Trancado）")
    class Trancado {

        @Test
        @DisplayName("所有玩家無法出牌後 → 廣播 FINISHED 含 trancado=true 與 trancadoScores")
        void trancado_broadcastsFinishedWithScores() throws Exception {
            String[] ctx = createFullRoom();
            String roomId = ctx[0];

            var room = matchmakingService.getRoom(roomId);

            // 找到首出玩家（持有 [9|9]）
            String firstId = room.currentPlayer().getId();
            int doubleNineId = room.currentPlayer().getHand().stream()
                    .filter(t -> t.left() == 9 && t.right() == 9)
                    .map(Tile::id).findFirst().orElseThrow();

            // 首出玩家只留 [9|9]（出完後手牌清空 → 但我們要測死局，所以首出玩家多保留一張不可出的牌）
            // 把首出玩家的手牌改成：[9|9] + [0|1]（出 [9|9] 後 [0|1] 無法匹配桌面 9）
            var allTiles = gameEngineService.generateTileSet();
            Tile blockingTile01 = allTiles.stream()
                    .filter(t -> t.left() == 0 && t.right() == 1)
                    .findFirst().orElseThrow();
            Tile doubleNineTile = allTiles.stream()
                    .filter(t -> t.left() == 9 && t.right() == 9)
                    .findFirst().orElseThrow();

            room.currentPlayer().getHand().clear();
            room.currentPlayer().getHand().add(doubleNineTile);
            room.currentPlayer().getHand().add(blockingTile01);

            // 其他三位玩家各自給一張不同的、無法匹配 9 的牌
            Tile block02 = allTiles.stream().filter(t -> t.left() == 0 && t.right() == 2).findFirst().orElseThrow();
            Tile block03 = allTiles.stream().filter(t -> t.left() == 0 && t.right() == 3).findFirst().orElseThrow();
            Tile block04 = allTiles.stream().filter(t -> t.left() == 0 && t.right() == 4).findFirst().orElseThrow();

            List<Tile> blockingTiles = List.of(block02, block03, block04);
            int blockIdx = 0;
            for (var p : room.getPlayers()) {
                if (!p.getId().equals(firstId)) {
                    p.getHand().clear();
                    p.getHand().add(blockingTiles.get(blockIdx++));
                }
            }

            StompSession session = connect();
            CompletableFuture<Map<String, Object>> future = subscribeRoom(session, roomId);
            sendPlay(session, roomId, firstId, doubleNineId, BoardEnd.LEFT);

            Map<String, Object> payload = future.get(TIMEOUT_SEC, TimeUnit.SECONDS);

            assertThat(payload.get("status")).isEqualTo("FINISHED");
            assertThat(payload.get("trancado")).isEqualTo(true);
            assertThat(payload.get("trancadoScores")).isNotNull();
            assertThat(payload.get("winnerId")).isNotNull();
        }
    }

    @Nested
    @DisplayName("遊戲狀態保護")
    class GameStateGuard {

        @Test
        @DisplayName("遊戲尚未開始（WAITING 狀態）出牌 → 廣播錯誤")
        void playTile_waitingRoom_broadcastsError() throws Exception {
            // 建立只有 1 人的房間（狀態 = WAITING）
            var room = matchmakingService.createRoom("Alice");
            String roomId = room.getRoomId();
            String playerId = room.getPlayers().get(0).getId();

            StompSession session = connect();
            CompletableFuture<Map<String, Object>> future = subscribeRoom(session, roomId);

            // 傳送一個假的 PlayMoveRequest
            PlayMoveRequest req = new PlayMoveRequest(roomId, playerId, 54, BoardEnd.LEFT);
            session.send("/app/game.play", req);

            Map<String, Object> payload = future.get(TIMEOUT_SEC, TimeUnit.SECONDS);
            assertThat(payload).containsKey("error");
        }
    }
}
