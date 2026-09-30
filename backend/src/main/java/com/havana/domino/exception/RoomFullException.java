package com.havana.domino.exception;

/**
 * 房間已達 4 人上限（對應 HTTP 409）。
 */
public class RoomFullException extends RuntimeException {
    public RoomFullException(String roomId) {
        super("Room is full: " + roomId);
    }
}
