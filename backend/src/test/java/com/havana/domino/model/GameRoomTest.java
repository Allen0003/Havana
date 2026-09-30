package com.havana.domino.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("GameRoom model")
class GameRoomTest {

    private Player alice;
    private Player bob;
    private Player carol;
    private Player dave;
    private GameRoom room;

    @BeforeEach
    void setUp() {
        alice = Player.builder().id("p1").name("Alice").build();
        bob   = Player.builder().id("p2").name("Bob").build();
        carol = Player.builder().id("p3").name("Carol").build();
        dave  = Player.builder().id("p4").name("Dave").build();

        room = GameRoom.builder()
                .roomId("room-001")
                .players(List.of(alice, bob, carol, dave))
                .build();
    }

    @Test
    @DisplayName("Builder 預設值：status=WAITING, turnIndex=0, trancado=false")
    void defaultValues() {
        assertThat(room.getStatus()).isEqualTo(GameStatus.WAITING);
        assertThat(room.getCurrentTurnIndex()).isEqualTo(0);
        assertThat(room.isTrancado()).isFalse();
        assertThat(room.getWinnerId()).isNull();
    }

    @Test
    @DisplayName("currentPlayer() 回傳 index 對應的玩家")
    void currentPlayer_returnsCorrectPlayer() {
        assertThat(room.currentPlayer()).isEqualTo(alice);
    }

    @Test
    @DisplayName("advanceTurn() 依序遞增，超過邊界後循環回 0")
    void advanceTurn_cycles() {
        room.advanceTurn(); // 1
        assertThat(room.currentPlayer()).isEqualTo(bob);
        room.advanceTurn(); // 2
        assertThat(room.currentPlayer()).isEqualTo(carol);
        room.advanceTurn(); // 3
        assertThat(room.currentPlayer()).isEqualTo(dave);
        room.advanceTurn(); // 4 → 0
        assertThat(room.currentPlayer()).isEqualTo(alice);
    }

    @Test
    @DisplayName("findPlayer() 以 id 找到正確玩家")
    void findPlayer_found() {
        assertThat(room.findPlayer("p3")).isEqualTo(carol);
    }

    @Test
    @DisplayName("findPlayer() 找不到時拋出 IllegalArgumentException")
    void findPlayer_notFound_throws() {
        assertThatThrownBy(() -> room.findPlayer("p99"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Player not found");
    }
}
