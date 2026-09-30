package com.havana.domino.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * POST /api/rooms 請求 body。
 */
public record CreateRoomRequest(
        @NotBlank(message = "hostName must not be blank")
        @Size(min = 1, max = 20, message = "hostName must be 1–20 characters")
        String hostName
) {}
