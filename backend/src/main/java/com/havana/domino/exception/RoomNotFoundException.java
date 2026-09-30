package com.havana.domino.exception;

/**
 * 房間不存在（對應 HTTP 404）。
 */
public class RoomNotFoundException extends RuntimeException {
    public RoomNotFoundException(String roomId) {
        super("Room not found: " + roomId);
    }
}
