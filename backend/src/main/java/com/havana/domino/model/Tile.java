package com.havana.domino.model;

/**
 * 骨牌（不可變 record）。
 * id 唯一識別每張牌，left / right 為兩端點數（0–9）。
 */
public record Tile(int id, int left, int right) {

    /** 是否為對子（兩端相同） */
    public boolean isDouble() {
        return left == right;
    }

    /** 此牌任一端是否可匹配指定點數 */
    public boolean canMatch(int value) {
        return left == value || right == value;
    }

    /**
     * 給定一端點數，回傳另一端點數（用於計算桌面延伸後的新端點）。
     * 若兩端相同（對子），回傳相同值。
     */
    public int matchingEnd(int value) {
        if (left == value) return right;
        if (right == value) return left;
        throw new IllegalArgumentException(
            "Tile [%d|%d] cannot match value %d".formatted(left, right, value));
    }

    /** 兩端點數總和（用於死局計分） */
    public int totalPips() {
        return left + right;
    }

    @Override
    public String toString() {
        return "[%d|%d]".formatted(left, right);
    }
}
