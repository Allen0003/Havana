package com.havana.domino.dto;

import com.havana.domino.model.Player;

/**
 * 廣播用玩家資訊。
 * 只包含手牌「數量」（handCount），不暴露牌面內容。
 */
public record PlayerDTO(String id, String name, int handCount, boolean connected) {

    public static PlayerDTO from(Player player) {
        return new PlayerDTO(
                player.getId(),
                player.getName(),
                player.getHand().size(),
                player.isConnected()
        );
    }
}
