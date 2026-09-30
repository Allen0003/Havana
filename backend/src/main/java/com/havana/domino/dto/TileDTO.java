package com.havana.domino.dto;

import com.havana.domino.model.Tile;

/**
 * 骨牌的公開表示（廣播安全，僅包含已知資訊）。
 */
public record TileDTO(int id, int left, int right, boolean isDouble) {

    public static TileDTO from(Tile tile) {
        return new TileDTO(tile.id(), tile.left(), tile.right(), tile.isDouble());
    }
}
