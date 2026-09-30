package com.havana.domino.model;

/** 遊戲房間狀態機。 */
public enum GameStatus {
    /** 等待玩家加入 */
    WAITING,
    /** 遊戲進行中 */
    IN_PROGRESS,
    /** 遊戲已結束 */
    FINISHED
}
