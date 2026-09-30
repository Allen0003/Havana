package com.havana.domino.dto;

import java.util.List;

/**
 * POST /api/rooms/{roomId}/join 回應 body。
 */
public record JoinRoomResponse(boolean success, String playerId, List<PlayerDTO> players) {}
