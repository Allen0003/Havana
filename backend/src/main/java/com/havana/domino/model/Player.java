package com.havana.domino.model;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 玩家領域模型。
 * hand 為伺服器完整持有的手牌清單，不對外廣播牌面。
 */
@Data
@Builder
public class Player {

    /** 玩家唯一識別碼（UUID） */
    private String id;

    /** 玩家顯示名稱 */
    private String name;

    /** 玩家手牌（僅伺服器端持有完整內容） */
    @Builder.Default
    private List<Tile> hand = new ArrayList<>();

    /** WebSocket 連線狀態 */
    @Builder.Default
    private boolean connected = true;

    /** 取得手牌中剩餘點數總和（用於死局計分） */
    public int handPipTotal() {
        return hand.stream().mapToInt(Tile::totalPips).sum();
    }
}
