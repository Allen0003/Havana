package com.havana.domino.service;

import com.havana.domino.model.BoardEnd;
import com.havana.domino.model.GameRoom;
import com.havana.domino.model.Player;
import com.havana.domino.model.Tile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 核心遊戲規則引擎介面。
 * 所有狀態變更均以 GameRoom 作為輸入/輸出，維持不可變風格。
 */
public interface GameEngineService {

    /**
     * 產生雙九制完整牌組（55 張）。
     * 點數組合從 [0|0] 到 [9|9]，id 由 0 開始遞增。
     */
    List<Tile> generateTileSet();

    /**
     * 洗牌並發牌，設定首出玩家（持有 [9|9] 者），回傳就緒的 GameRoom。
     * 前置條件：room 中已有 4 名玩家，status 為 WAITING。
     */
    GameRoom initGame(GameRoom room);

    /**
     * 驗證並執行出牌，更新桌面與手牌，推進回合。
     * 若出牌不合法，拋出 {@link IllegalArgumentException}。
     */
    GameRoom playTile(GameRoom room, String playerId, int tileId, BoardEnd targetEnd);

    /**
     * 驗證並執行過牌（Pass/Knock）。
     * 若玩家手中仍有合法可出的牌，拋出 {@link IllegalStateException}。
     */
    GameRoom passTurn(GameRoom room, String playerId);

    /**
     * 檢查是否有玩家獲勝（手牌清空）。
     * 回傳勝者；若無人獲勝回傳 empty。
     */
    Optional<Player> checkWinner(GameRoom room);

    /**
     * 檢查是否發生死局（Trancado）：所有玩家均無法合法出牌。
     */
    boolean isTrancado(GameRoom room);

    /**
     * 計算死局分數，回傳各 playerId → 剩餘手牌點數總和的 Map。
     */
    Map<String, Integer> calculateTrancadoScores(GameRoom room);
}
