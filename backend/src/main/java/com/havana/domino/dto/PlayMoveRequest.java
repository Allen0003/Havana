package com.havana.domino.dto;

import com.havana.domino.model.BoardEnd;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * WebSocket /app/game.play 出牌請求 payload。
 */
public record PlayMoveRequest(
        @NotBlank(message = "roomId must not be blank")
        String roomId,

        @NotBlank(message = "playerId must not be blank")
        String playerId,

        @NotNull(message = "tileId must not be null")
        Integer tileId,

        @NotNull(message = "targetEnd must not be null")
        BoardEnd targetEnd
) {}
