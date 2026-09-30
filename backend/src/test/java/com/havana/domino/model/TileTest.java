package com.havana.domino.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Tile record")
class TileTest {

    // ── isDouble ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isDouble()")
    class IsDouble {

        @Test
        @DisplayName("兩端相同 → true")
        void sameEnds_returnsTrue() {
            assertThat(new Tile(0, 9, 9).isDouble()).isTrue();
            assertThat(new Tile(1, 0, 0).isDouble()).isTrue();
        }

        @Test
        @DisplayName("兩端不同 → false")
        void differentEnds_returnsFalse() {
            assertThat(new Tile(2, 3, 7).isDouble()).isFalse();
        }
    }

    // ── canMatch ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("canMatch()")
    class CanMatch {

        @Test
        @DisplayName("值等於 left → true")
        void matchesLeft() {
            assertThat(new Tile(3, 5, 9).canMatch(5)).isTrue();
        }

        @Test
        @DisplayName("值等於 right → true")
        void matchesRight() {
            assertThat(new Tile(4, 5, 9).canMatch(9)).isTrue();
        }

        @Test
        @DisplayName("值不匹配任一端 → false")
        void noMatch() {
            assertThat(new Tile(5, 5, 9).canMatch(3)).isFalse();
        }

        @Test
        @DisplayName("對子：值等於兩端 → true")
        void doubleCanMatch() {
            assertThat(new Tile(6, 7, 7).canMatch(7)).isTrue();
        }
    }

    // ── matchingEnd ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("matchingEnd()")
    class MatchingEnd {

        @Test
        @DisplayName("給定 left 端點數 → 回傳 right")
        void givenLeft_returnsRight() {
            assertThat(new Tile(7, 3, 8).matchingEnd(3)).isEqualTo(8);
        }

        @Test
        @DisplayName("給定 right 端點數 → 回傳 left")
        void givenRight_returnsLeft() {
            assertThat(new Tile(8, 3, 8).matchingEnd(8)).isEqualTo(3);
        }

        @Test
        @DisplayName("對子：兩端相同 → 回傳相同值")
        void doubleReturnsItself() {
            assertThat(new Tile(9, 6, 6).matchingEnd(6)).isEqualTo(6);
        }

        @Test
        @DisplayName("不匹配的值 → 丟出 IllegalArgumentException")
        void noMatchThrows() {
            Tile tile = new Tile(10, 2, 5);
            assertThatThrownBy(() -> tile.matchingEnd(9))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot match");
        }
    }

    // ── totalPips ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("totalPips()")
    class TotalPips {

        @Test
        @DisplayName("兩端點數之和")
        void sumsBothEnds() {
            assertThat(new Tile(11, 4, 6).totalPips()).isEqualTo(10);
        }

        @Test
        @DisplayName("對子 [9|9] → 18")
        void doubleNine() {
            assertThat(new Tile(54, 9, 9).totalPips()).isEqualTo(18);
        }

        @Test
        @DisplayName("[0|0] → 0")
        void blankDouble() {
            assertThat(new Tile(0, 0, 0).totalPips()).isEqualTo(0);
        }
    }

    // ── toString ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("toString() 格式為 [left|right]")
    void toStringFormat() {
        assertThat(new Tile(12, 3, 7).toString()).isEqualTo("[3|7]");
    }
}
