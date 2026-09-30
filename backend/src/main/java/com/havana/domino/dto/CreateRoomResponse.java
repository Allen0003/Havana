package com.havana.domino.dto;

import com.havana.domino.model.GameStatus;

/**
 * POST /api/rooms 回應 body。
 */
public record CreateRoomResponse(String roomId, GameStatus status, String hostPlayerId) {}
