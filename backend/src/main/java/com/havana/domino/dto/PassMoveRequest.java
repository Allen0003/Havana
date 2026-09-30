package com.havana.domino.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * WebSocket /app/game.pass 過牌請求 payload。
 */
public record PassMoveRequest(
        @NotBlank(message = "roomId must not be blank")
        String roomId,

        @NotBlank(message = "playerId must not be blank")
        String playerId
) {}
