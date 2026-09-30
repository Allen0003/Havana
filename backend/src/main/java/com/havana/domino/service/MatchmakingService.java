package com.havana.domino.service;

import com.havana.domino.model.GameRoom;

/**
 * 房間管理服務：建立房間、加入房間。
 * 當第 4 位玩家加入時自動觸發 {@link GameEngineService#initGame(GameRoom)}。
 */
public interface MatchmakingService {

    /**
     * 建立新房間，房主立即成為第一位玩家。
     *
     * @param hostName 房主顯示名稱
     * @return 初始化完成的 GameRoom（status = WAITING）
     */
    GameRoom createRoom(String hostName);

    /**
     * 加入現有房間。
     *
     * @param roomId     目標房間 ID
     * @param playerName 玩家顯示名稱
     * @return 更新後的 GameRoom（若 4 人滿員則 status = IN_PROGRESS）
     * @throws com.havana.domino.exception.RoomNotFoundException 房間不存在
     * @throws com.havana.domino.exception.RoomFullException    房間已滿
     */
    GameRoom joinRoom(String roomId, String playerName);

    /**
     * 取得房間狀態；若不存在拋出 {@link com.havana.domino.exception.RoomNotFoundException}。
     */
    GameRoom getRoom(String roomId);
}
