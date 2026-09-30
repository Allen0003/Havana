package com.havana.domino.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * POST /api/rooms/{roomId}/join 請求 body。
 */
public record JoinRoomRequest(
        @NotBlank(message = "playerName must not be blank")
        @Size(min = 1, max = 20, message = "playerName must be 1–20 characters")
        String playerName
) {}
