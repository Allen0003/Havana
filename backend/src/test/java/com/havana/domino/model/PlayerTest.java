package com.havana.domino.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Player model")
class PlayerTest {

    @Test
    @DisplayName("Builder 預設值：hand 空列表、connected = true")
    void defaultValues() {
        Player p = Player.builder().id("p1").name("Alice").build();
        assertThat(p.getHand()).isEmpty();
        assertThat(p.isConnected()).isTrue();
    }

    @Test
    @DisplayName("handPipTotal() 加總手牌所有點數")
    void handPipTotal_sumsAllTiles() {
        Player p = Player.builder()
                .id("p1").name("Alice")
                .hand(List.of(
                        new Tile(1, 3, 5),  // 8
                        new Tile(2, 2, 7),  // 9
                        new Tile(3, 0, 0)   // 0
                ))
                .build();
        assertThat(p.handPipTotal()).isEqualTo(17);
    }

    @Test
    @DisplayName("handPipTotal() 空手牌 → 0")
    void handPipTotal_emptyHand() {
        Player p = Player.builder().id("p2").name("Bob").build();
        assertThat(p.handPipTotal()).isEqualTo(0);
    }
}
