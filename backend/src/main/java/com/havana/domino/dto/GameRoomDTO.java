package com.havana.domino.dto;

import com.havana.domino.model.GameRoom;
import com.havana.domino.model.GameStatus;

import java.util.List;
import java.util.Map;

/**
 * 廣播用房間快照 DTO。
 *
 * <p><strong>安全保證</strong>：players 欄位僅含 handCount，不含任何手牌牌面，
 * 因此此 DTO 可安全地廣播給所有訂閱者。
 */
public record GameRoomDTO(
        String roomId,
        GameStatus status,
        List<PlayerDTO> players,
        List<TileDTO> boardTiles,
        Integer leftEnd,
        Integer rightEnd,
        int boneyardCount,
        String currentTurnPlayerId,
        String winnerId,
        boolean trancado,
        Map<String, Integer> trancadoScores
) {
    public static GameRoomDTO from(GameRoom room) {
        return from(room, null);
    }

    public static GameRoomDTO from(GameRoom room, Map<String, Integer> trancadoScores) {
        List<PlayerDTO> playerDTOs = room.getPlayers().stream()
                .map(PlayerDTO::from)
                .toList();

        List<TileDTO> boardTiles = room.getBoard().getTileList().stream()
                .map(TileDTO::from)
                .toList();

        String currentTurnPlayerId = room.getPlayers().isEmpty()
                ? null
                : room.currentPlayer().getId();

        return new GameRoomDTO(
                room.getRoomId(),
                room.getStatus(),
                playerDTOs,
                boardTiles,
                room.getBoard().getLeftEnd(),
                room.getBoard().getRightEnd(),
                room.getBoneyard().size(),
                currentTurnPlayerId,
                room.getWinnerId(),
                room.isTrancado(),
                trancadoScores
        );
    }
}
