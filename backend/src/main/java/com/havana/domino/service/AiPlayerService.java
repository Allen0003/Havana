package com.havana.domino.service;

import com.havana.domino.model.BoardEnd;
import com.havana.domino.model.GameRoom;
import com.havana.domino.model.Player;

import java.util.Optional;

/**
 * AI 玩家決策服務介面。
 *
 * <p>負責為 AI 玩家選擇最佳出牌或決定是否過牌。
 * 只在 AI 玩家的回合被呼叫。
 */
public interface AiPlayerService {

    /**
     * AI 出牌決策：從目前手牌中選出最佳的一張牌及放置端。
     *
     * @param room   目前房間狀態
     * @param player AI 玩家
     * @return 若有合法出牌，回傳 {@link AiMove}；否則回傳 empty（需要過牌）
     */
    Optional<AiMove> selectMove(GameRoom room, Player player);

    /**
     * AI 一次完整行動：自動出牌或過牌，並回傳更新後的 GameRoom。
     *
     * @param room     目前房間狀態
     * @param playerId AI 玩家的 ID
     * @return 執行動作後的 GameRoom
     */
    GameRoom executeAction(GameRoom room, String playerId);

    /**
     * AI 出牌決策的結果，包含選定的牌 ID 和目標端。
     */
    record AiMove(int tileId, BoardEnd targetEnd) {}
}
