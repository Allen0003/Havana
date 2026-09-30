package com.havana.domino.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Board model")
class BoardTest {

    @Test
    @DisplayName("初始化：空桌，leftEnd/rightEnd 均為 null")
    void initialState_isEmpty() {
        Board board = new Board();
        assertThat(board.isEmpty()).isTrue();
        assertThat(board.getLeftEnd()).isNull();
        assertThat(board.getRightEnd()).isNull();
        assertThat(board.getTileList()).isEmpty();
    }

    @Test
    @DisplayName("addFirst() 後 getTileList() 回傳正確順序")
    void addFirst_thenGetTileList() {
        Board board = new Board();
        Tile t1 = new Tile(54, 9, 9);
        Tile t2 = new Tile(44, 8, 9);

        board.getTiles().addLast(t1);
        board.getTiles().addFirst(t2);

        assertThat(board.getTileList()).containsExactly(t2, t1);
        assertThat(board.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("setLeftEnd / setRightEnd 可正確讀取")
    void setEnds() {
        Board board = new Board();
        board.setLeftEnd(4);
        board.setRightEnd(7);
        assertThat(board.getLeftEnd()).isEqualTo(4);
        assertThat(board.getRightEnd()).isEqualTo(7);
    }
}
