package com.havana.domino.service;

import com.havana.domino.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

@DisplayName("GameEngineService")
class GameEngineServiceTest {

    private GameEngineService engine;

    @BeforeEach
    void setUp() {
        engine = new GameEngineServiceImpl();
    }

    // ── 測試輔助 ──────────────────────────────────────────────────────────────

    /** 4 位玩家、WAITING 狀態的空房間 */
    private GameRoom buildWaitingRoom() {
        List<Player> players = List.of(
                Player.builder().id("p1").name("Alice").build(),
                Player.builder().id("p2").name("Bob").build(),
                Player.builder().id("p3").name("Carol").build(),
                Player.builder().id("p4").name("Dave").build()
        );
        return GameRoom.builder()
                .roomId("test-room")
                .players(new ArrayList<>(players))
                .build();
    }

    /** 完成 initGame 後的可用房間 */
    private GameRoom buildInProgressRoom() {
        return engine.initGame(buildWaitingRoom());
    }

    /**
     * 建立一個「桌面已有 [9|9]，leftEnd=rightEnd=9」的進行中房間。
     * 首出玩家已出過第一張 [9|9]，輪到下一個玩家。
     */
    private GameRoom roomAfterFirstPlay() {
        GameRoom room = buildInProgressRoom();
        Player first = room.currentPlayer();
        // 找出 [9|9]
        Tile doubleNine = first.getHand().stream()
                .filter(t -> t.left() == 9 && t.right() == 9)
                .findFirst().orElseThrow();
        engine.playTile(room, first.getId(), doubleNine.id(), BoardEnd.LEFT);
        return room;
    }

    /**
     * 直接在 board 上設定已知桌面（用於精準控制測試情境）。
     * 重設桌面：清空後放入 firstTile，並設定 leftEnd / rightEnd。
     */
    private void setupBoard(Board board, Tile firstTile, int leftEnd, int rightEnd) {
        board.getTiles().clear();
        board.getTiles().addLast(firstTile);
        board.setLeftEnd(leftEnd);
        board.setRightEnd(rightEnd);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // M1-3：generateTileSet()
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("generateTileSet()")
    class GenerateTileSet {

        @Test @DisplayName("必須產生恰好 55 張牌")
        void produces55Tiles() {
            assertThat(engine.generateTileSet()).hasSize(55);
        }

        @Test @DisplayName("所有 id 唯一，且從 0 連續編號到 54")
        void uniqueSequentialIds() {
            List<Integer> ids = engine.generateTileSet().stream()
                    .map(Tile::id).sorted().toList();
            for (int i = 0; i < 55; i++) assertThat(ids.get(i)).isEqualTo(i);
        }

        @Test @DisplayName("所有牌面組合唯一（無重複）")
        void noDuplicateCombinations() {
            Set<String> combos = engine.generateTileSet().stream()
                    .map(t -> t.left() + "-" + t.right())
                    .collect(Collectors.toSet());
            assertThat(combos).hasSize(55);
        }

        @Test @DisplayName("所有 left <= right")
        void leftAlwaysLessOrEqualRight() {
            engine.generateTileSet().forEach(t ->
                    assertThat(t.left()).isLessThanOrEqualTo(t.right()));
        }

        @Test @DisplayName("包含所有對子（0-0 到 9-9），共 10 張")
        void containsAllDoubles() {
            assertThat(engine.generateTileSet().stream().filter(Tile::isDouble).count())
                    .isEqualTo(10);
        }

        @Test @DisplayName("包含 [9|9]（雙九）")
        void containsDoubleNine() {
            assertThat(engine.generateTileSet().stream()
                    .anyMatch(t -> t.left() == 9 && t.right() == 9)).isTrue();
        }

        @Test @DisplayName("所有點數在 0–9 之間")
        void allPipsInRange() {
            engine.generateTileSet().forEach(t -> {
                assertThat(t.left()).isBetween(0, 9);
                assertThat(t.right()).isBetween(0, 9);
            });
        }

        @Test @DisplayName("牌組為不可變 List")
        void tileSetIsImmutable() {
            assertThatThrownBy(() -> engine.generateTileSet().add(new Tile(99, 0, 0)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // M1-4：initGame()
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("initGame()")
    class InitGame {

        @Test @DisplayName("每位玩家恰好持有 10 張手牌")
        void eachPlayerHas10Tiles() {
            GameRoom room = buildInProgressRoom();
            room.getPlayers().forEach(p ->
                    assertThat(p.getHand()).as(p.getName()).hasSize(10));
        }

        @Test @DisplayName("備用庫（boneyard）恰好剩 15 張")
        void boneyardHas15Tiles() {
            assertThat(buildInProgressRoom().getBoneyard()).hasSize(15);
        }

        @Test @DisplayName("手牌 + boneyard 總數 = 55")
        void totalTilesIs55() {
            GameRoom room = buildInProgressRoom();
            long handTotal = room.getPlayers().stream().mapToLong(p -> p.getHand().size()).sum();
            assertThat(handTotal + room.getBoneyard().size()).isEqualTo(55);
        }

        @Test @DisplayName("全場牌 id 唯一，無重複發牌")
        void noDuplicateTileIds() {
            GameRoom room = buildInProgressRoom();
            List<Integer> allIds = new ArrayList<>();
            room.getPlayers().forEach(p -> p.getHand().forEach(t -> allIds.add(t.id())));
            room.getBoneyard().forEach(t -> allIds.add(t.id()));
            assertThat(Set.copyOf(allIds)).hasSize(55);
        }

        @Test @DisplayName("首出玩家手中持有 [9|9]")
        void firstPlayerHasDoubleNine() {
            GameRoom room = buildInProgressRoom();
            boolean has99 = room.currentPlayer().getHand().stream()
                    .anyMatch(t -> t.left() == 9 && t.right() == 9);
            assertThat(has99).isTrue();
        }

        @Test @DisplayName("狀態更新為 IN_PROGRESS")
        void statusBecomesInProgress() {
            assertThat(buildInProgressRoom().getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
        }

        @Test @DisplayName("桌面初始為空")
        void boardStartsEmpty() {
            GameRoom room = buildInProgressRoom();
            assertThat(room.getBoard().isEmpty()).isTrue();
            assertThat(room.getBoard().getLeftEnd()).isNull();
            assertThat(room.getBoard().getRightEnd()).isNull();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // M1-5：playTile()
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("playTile()")
    class PlayTile {

        // ── 首張出牌（空桌）────────────────────────────────────────────────

        @Test @DisplayName("空桌：出 [9|9] 成功，桌面兩端均為 9")
        void firstPlay_doubleNine_succeeds() {
            GameRoom room = buildInProgressRoom();
            Player first  = room.currentPlayer();
            Tile d9 = first.getHand().stream()
                    .filter(t -> t.left() == 9 && t.right() == 9).findFirst().orElseThrow();

            engine.playTile(room, first.getId(), d9.id(), BoardEnd.LEFT);

            assertThat(room.getBoard().isEmpty()).isFalse();
            assertThat(room.getBoard().getLeftEnd()).isEqualTo(9);
            assertThat(room.getBoard().getRightEnd()).isEqualTo(9);
            assertThat(first.getHand()).hasSize(9);
        }

        @Test @DisplayName("空桌：出非 [9|9] 拋出 INVALID_FIRST_MOVE")
        void firstPlay_notDoubleNine_throws() {
            GameRoom room = buildInProgressRoom();
            Player first  = room.currentPlayer();
            Tile nonD9 = first.getHand().stream()
                    .filter(t -> !(t.left() == 9 && t.right() == 9)).findFirst().orElseThrow();

            assertThatThrownBy(() ->
                    engine.playTile(room, first.getId(), nonD9.id(), BoardEnd.LEFT))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("INVALID_FIRST_MOVE");
        }

        // ── 非輪次玩家 ──────────────────────────────────────────────────────

        @Test @DisplayName("非輪次玩家出牌拋出 NOT_YOUR_TURN")
        void notYourTurn_throws() {
            GameRoom room = buildInProgressRoom();
            // 找到非 currentPlayer 的玩家
            Player other = room.getPlayers().stream()
                    .filter(p -> !p.getId().equals(room.currentPlayer().getId()))
                    .findFirst().orElseThrow();
            Tile any = other.getHand().get(0);

            assertThatThrownBy(() ->
                    engine.playTile(room, other.getId(), any.id(), BoardEnd.LEFT))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("NOT_YOUR_TURN");
        }

        // ── 手牌中不存在的牌 ────────────────────────────────────────────────

        @Test @DisplayName("出手牌中不存在的 tileId 拋出 TILE_NOT_IN_HAND")
        void tileNotInHand_throws() {
            GameRoom room = buildInProgressRoom();
            Player first  = room.currentPlayer();

            assertThatThrownBy(() ->
                    engine.playTile(room, first.getId(), 9999, BoardEnd.LEFT))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("TILE_NOT_IN_HAND");
        }

        // ── 非空桌：正確匹配 ────────────────────────────────────────────────

        @Test @DisplayName("非空桌：出可匹配右端的牌，rightEnd 正確更新")
        void matchRight_updatesRightEnd() {
            GameRoom room = buildInProgressRoom();

            // 手動建立簡單盤面：桌面 [3|5]，leftEnd=3，rightEnd=5
            Tile boardTile = new Tile(100, 3, 5);
            setupBoard(room.getBoard(), boardTile, 3, 5);

            // 給當前玩家一張 [5|7]（可接右端 5）
            Tile playable = new Tile(101, 5, 7);
            room.currentPlayer().getHand().add(playable);

            engine.playTile(room, room.currentPlayer().getId(), playable.id(), BoardEnd.RIGHT);

            assertThat(room.getBoard().getRightEnd()).isEqualTo(7);
            assertThat(room.getBoard().getTileList()).hasSize(2);
        }

        @Test @DisplayName("非空桌：出可匹配左端的牌，leftEnd 正確更新")
        void matchLeft_updatesLeftEnd() {
            GameRoom room = buildInProgressRoom();

            Tile boardTile = new Tile(100, 3, 5);
            setupBoard(room.getBoard(), boardTile, 3, 5);

            // 給當前玩家一張 [1|3]（可接左端 3）
            Tile playable = new Tile(102, 1, 3);
            room.currentPlayer().getHand().add(playable);

            engine.playTile(room, room.currentPlayer().getId(), playable.id(), BoardEnd.LEFT);

            assertThat(room.getBoard().getLeftEnd()).isEqualTo(1);
            assertThat(room.getBoard().getTileList()).hasSize(2);
        }

        @Test @DisplayName("非空桌：出對子，對應端點數不變（對子兩端相同）")
        void matchDouble_endStaysSame() {
            GameRoom room = buildInProgressRoom();

            Tile boardTile = new Tile(100, 5, 7);
            setupBoard(room.getBoard(), boardTile, 5, 7);

            // 給當前玩家一張 [7|7]（接右端 7，對子，另一端還是 7）
            Tile doubleSevent = new Tile(103, 7, 7);
            room.currentPlayer().getHand().add(doubleSevent);

            engine.playTile(room, room.currentPlayer().getId(), doubleSevent.id(), BoardEnd.RIGHT);

            assertThat(room.getBoard().getRightEnd()).isEqualTo(7);
        }

        @Test @DisplayName("非空桌：點數不匹配拋出 INVALID_MOVE")
        void mismatch_throws() {
            GameRoom room = buildInProgressRoom();

            Tile boardTile = new Tile(100, 3, 5);
            setupBoard(room.getBoard(), boardTile, 3, 5);

            // 給當前玩家一張 [8|9]（無法接 3 也無法接 5）
            Tile noMatch = new Tile(104, 8, 9);
            room.currentPlayer().getHand().add(noMatch);

            assertThatThrownBy(() ->
                    engine.playTile(room, room.currentPlayer().getId(), noMatch.id(), BoardEnd.RIGHT))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("INVALID_MOVE");
        }

        // ── 出牌後推進回合 ──────────────────────────────────────────────────

        @Test @DisplayName("合法出牌後，輪次推進到下一位玩家")
        void turnAdvancesAfterPlay() {
            GameRoom room = buildInProgressRoom();
            Player first  = room.currentPlayer();
            String firstId = first.getId();

            Tile d9 = first.getHand().stream()
                    .filter(t -> t.left() == 9 && t.right() == 9).findFirst().orElseThrow();
            engine.playTile(room, firstId, d9.id(), BoardEnd.LEFT);

            assertThat(room.currentPlayer().getId()).isNotEqualTo(firstId);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // M1-6：passTurn()
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("passTurn()")
    class PassTurn {

        @Test @DisplayName("玩家手牌全部無法匹配桌面 → 過牌成功，輪次推進")
        void cannotPlay_passSucceeds() {
            GameRoom room = roomAfterFirstPlay(); // 桌面 leftEnd=rightEnd=9
            Player cur = room.currentPlayer();

            // 清空手牌，重新給完全不含 9 的牌（點數 0~6）
            cur.setHand(new ArrayList<>(List.of(
                    new Tile(200, 0, 1),
                    new Tile(201, 2, 3),
                    new Tile(202, 4, 6)
            )));

            String beforeId = cur.getId();
            engine.passTurn(room, cur.getId());

            assertThat(room.currentPlayer().getId()).isNotEqualTo(beforeId);
        }

        @Test @DisplayName("玩家有可出的牌 → 拋出 CAN_PLAY")
        void canPlay_throws() {
            GameRoom room = roomAfterFirstPlay(); // 桌面 leftEnd=rightEnd=9
            Player cur = room.currentPlayer();

            // 給玩家一張 [9|6]（可接 9）
            cur.getHand().add(new Tile(210, 6, 9));

            assertThatThrownBy(() -> engine.passTurn(room, cur.getId()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("CAN_PLAY");
        }

        @Test @DisplayName("非輪次玩家嘗試過牌 → 拋出 NOT_YOUR_TURN")
        void notYourTurn_throws() {
            GameRoom room = roomAfterFirstPlay();
            Player other = room.getPlayers().stream()
                    .filter(p -> !p.getId().equals(room.currentPlayer().getId()))
                    .findFirst().orElseThrow();

            assertThatThrownBy(() -> engine.passTurn(room, other.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("NOT_YOUR_TURN");
        }

        @Test @DisplayName("空桌時，非 [9|9] 持有者無法出牌，可以過牌")
        void emptyBoard_noDoubleNine_canPass() {
            GameRoom room = buildInProgressRoom();
            // 把首出玩家以外的某位玩家手牌換成不含 9 的牌，
            // 然後讓首出玩家先出 [9|9]，再測下一位玩家可否過牌（若沒有可接 9 的牌）
            GameRoom afterFirst = roomAfterFirstPlay();
            Player cur = afterFirst.currentPlayer();
            cur.setHand(new ArrayList<>(List.of(
                    new Tile(300, 0, 2),
                    new Tile(301, 1, 3)
            )));
            // 此時桌面 9|9，玩家沒有含 9 的牌 → 可以過牌
            assertThatNoException().isThrownBy(
                    () -> engine.passTurn(afterFirst, cur.getId()));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // M1-7：checkWinner()
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("checkWinner()")
    class CheckWinner {

        @Test @DisplayName("所有玩家手牌非空 → 回傳 empty")
        void noWinner_returnsEmpty() {
            assertThat(engine.checkWinner(buildInProgressRoom())).isEmpty();
        }

        @Test @DisplayName("一位玩家手牌清空 → 回傳該玩家")
        void playerClearsHand_returnsWinner() {
            GameRoom room = buildInProgressRoom();
            Player winner = room.getPlayers().get(2);
            winner.setHand(new ArrayList<>());     // 模擬手牌清空

            assertThat(engine.checkWinner(room))
                    .isPresent()
                    .hasValueSatisfying(p -> assertThat(p.getId()).isEqualTo(winner.getId()));
        }

        @Test @DisplayName("第一個手牌清空的玩家勝出（多人同時清空取第一位）")
        void firstEmptyHandWins() {
            GameRoom room = buildInProgressRoom();
            room.getPlayers().get(0).setHand(new ArrayList<>());
            room.getPlayers().get(2).setHand(new ArrayList<>());

            assertThat(engine.checkWinner(room))
                    .isPresent()
                    .hasValueSatisfying(p -> assertThat(p.getId()).isEqualTo("p1"));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // M1-8：isTrancado()
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("isTrancado()")
    class IsTrancado {

        @Test @DisplayName("桌面為空 → 非死局")
        void emptyBoard_notTrancado() {
            assertThat(engine.isTrancado(buildInProgressRoom())).isFalse();
        }

        @Test @DisplayName("有玩家手牌已空（獲勝）→ 非死局")
        void playerWon_notTrancado() {
            GameRoom room = roomAfterFirstPlay();
            room.currentPlayer().setHand(new ArrayList<>());
            assertThat(engine.isTrancado(room)).isFalse();
        }

        @Test @DisplayName("所有玩家都無法接牌 → 死局")
        void allPlayersStuck_isTrancado() {
            GameRoom room = roomAfterFirstPlay(); // board leftEnd=rightEnd=9
            // 給每位玩家一組完全不含 9 的手牌
            room.getPlayers().forEach(p -> p.setHand(new ArrayList<>(List.of(
                    new Tile(400 + room.getPlayers().indexOf(p), 0, 1)
            ))));

            assertThat(engine.isTrancado(room)).isTrue();
        }

        @Test @DisplayName("至少一位玩家有牌可出 → 非死局")
        void onePlayerCanPlay_notTrancado() {
            GameRoom room = roomAfterFirstPlay(); // board leftEnd=rightEnd=9
            // p1 有 [9|3] 可接
            room.getPlayers().get(0).setHand(new ArrayList<>(List.of(new Tile(500, 3, 9))));
            // 其他人給不含 9 的牌
            for (int i = 1; i < room.getPlayers().size(); i++) {
                room.getPlayers().get(i).setHand(new ArrayList<>(List.of(new Tile(501 + i, 0, 1))));
            }

            assertThat(engine.isTrancado(room)).isFalse();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // M1-8：calculateTrancadoScores()
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("calculateTrancadoScores()")
    class CalculateTrancadoScores {

        @Test @DisplayName("每位玩家分數 = 剩餘手牌點數總和")
        void scoresMatchHandPipTotals() {
            GameRoom room = buildInProgressRoom();
            // 給每位玩家已知手牌以驗算
            room.getPlayers().get(0).setHand(List.of(new Tile(0, 2, 3)));  // 5
            room.getPlayers().get(1).setHand(List.of(new Tile(1, 4, 5)));  // 9
            room.getPlayers().get(2).setHand(List.of(new Tile(2, 0, 0)));  // 0
            room.getPlayers().get(3).setHand(List.of(new Tile(3, 7, 8)));  // 15

            Map<String, Integer> scores = engine.calculateTrancadoScores(room);

            assertThat(scores.get("p1")).isEqualTo(5);
            assertThat(scores.get("p2")).isEqualTo(9);
            assertThat(scores.get("p3")).isEqualTo(0);
            assertThat(scores.get("p4")).isEqualTo(15);
        }

        @Test @DisplayName("所有玩家都在分數 Map 中")
        void allPlayersPresent() {
            GameRoom room = buildInProgressRoom();
            Map<String, Integer> scores = engine.calculateTrancadoScores(room);
            assertThat(scores.keySet()).containsExactlyInAnyOrder("p1", "p2", "p3", "p4");
        }

        @Test @DisplayName("手牌空的玩家分數為 0")
        void emptyHand_scoreIsZero() {
            GameRoom room = buildInProgressRoom();
            room.getPlayers().get(0).setHand(new ArrayList<>());
            assertThat(engine.calculateTrancadoScores(room).get("p1")).isEqualTo(0);
        }

        @Test @DisplayName("分數最低的玩家可透過 min 取得（輔助驗證勝者判斷邏輯）")
        void lowestScorePlayerIsWinner() {
            GameRoom room = buildInProgressRoom();
            room.getPlayers().get(0).setHand(List.of(new Tile(0, 1, 2)));  // 3
            room.getPlayers().get(1).setHand(List.of(new Tile(1, 5, 6)));  // 11
            room.getPlayers().get(2).setHand(List.of(new Tile(2, 3, 4)));  // 7
            room.getPlayers().get(3).setHand(List.of(new Tile(3, 8, 9)));  // 17

            Map<String, Integer> scores = engine.calculateTrancadoScores(room);
            String winner = scores.entrySet().stream()
                    .min(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey).orElseThrow();

            assertThat(winner).isEqualTo("p1");
        }
    }
}
