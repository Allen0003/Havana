package com.havana.domino.model;

import lombok.Data;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 桌面狀態。
 * tiles 以雙端佇列儲存，leftEnd / rightEnd 記錄當前可接牌的點數。
 */
@Data
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
}
