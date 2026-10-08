package com.havana.domino.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * 桌面狀態。
 * tiles 以雙端佇列儲存，leftEnd / rightEnd 記錄當前可接牌的點數。
 */
public class Board {

    /**
     * 桌面骨牌排列（由左至右）。
     * 使用 ArrayDeque 方便兩端新增。
     */
    private Deque<Tile> tiles = new ArrayDeque<>();

    /** 桌面左端可接點數；null 表示空桌 */
    private Integer leftEnd;

    /** 桌面右端可接點數；null 表示空桌 */
    private Integer rightEnd;

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Deque<Tile> getTiles() {
        return tiles;
    }

    public void setTiles(Deque<Tile> tiles) {
        this.tiles = tiles;
    }

    public Integer getLeftEnd() {
        return leftEnd;
    }

    public void setLeftEnd(Integer leftEnd) {
        this.leftEnd = leftEnd;
    }

    public Integer getRightEnd() {
        return rightEnd;
    }

    public void setRightEnd(Integer rightEnd) {
        this.rightEnd = rightEnd;
    }

    // ── 便利方法 ──────────────────────────────────────────────────────────────

    /** 是否為空桌 */
    public boolean isEmpty() {
        return tiles.isEmpty();
    }

    /**
     * 取得桌面骨牌的有序清單（供 DTO 序列化）。
     */
    public List<Tile> getTileList() {
        return new ArrayList<>(tiles);
    }

    // ── equals / hashCode / toString ─────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Board board = (Board) o;
        return Objects.equals(tiles, board.tiles)
                && Objects.equals(leftEnd, board.leftEnd)
                && Objects.equals(rightEnd, board.rightEnd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tiles, leftEnd, rightEnd);
    }

    @Override
    public String toString() {
        return "Board{tiles=" + tiles + ", leftEnd=" + leftEnd + ", rightEnd=" + rightEnd + "}";
    }
}
