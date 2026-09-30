package com.havana.domino.model;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 遊戲房間聚合根。
 * 持有遊戲所需的完整狀態，包含玩家列表、桌面、備用庫與回合資訊。
 */
@Data
@Builder
public class GameRoom {

    /** 房間唯一識別碼 */
    private String roomId;

    /** 玩家列表（順序即為出牌順序） */
    @Builder.Default
    private List<Player> players = new ArrayList<>();

    /** 桌面狀態 */
    @Builder.Default
    private Board board = new Board();

    /** 備用庫（未發出的牌） */
    @Builder.Default
    private List<Tile> boneyard = new ArrayList<>();

    /** 房間目前狀態 */
    @Builder.Default
    private GameStatus status = GameStatus.WAITING;

    /** 當前輪次玩家在 players 列表中的索引 */
    @Builder.Default
    private int currentTurnIndex = 0;

    /** 勝者 playerId；null 表示尚未結束 */
    private String winnerId;

    /** 是否宣告死局（Trancado） */
    @Builder.Default
    private boolean trancado = false;

    // ── 便利方法 ──────────────────────────────────────────

    /** 取得當前輪次玩家 */
    public Player currentPlayer() {
        return players.get(currentTurnIndex);
    }

    /** 推進至下一位玩家（循環） */
    public void advanceTurn() {
        currentTurnIndex = (currentTurnIndex + 1) % players.size();
    }

    /** 根據 playerId 尋找玩家 */
    public Player findPlayer(String playerId) {
        return players.stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId));
    }
}
