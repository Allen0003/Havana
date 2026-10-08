package com.havana.domino.service;

import com.havana.domino.exception.RoomFullException;
import com.havana.domino.exception.RoomNotFoundException;
import com.havana.domino.model.GameRoom;
import com.havana.domino.model.GameStatus;
import com.havana.domino.model.Player;
import com.havana.domino.repository.GameRoomRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * {@link MatchmakingService} 實作。
 *
 * <p>執行緒安全：由 {@link GameRoomRepository}（ConcurrentHashMap）保護持久狀態；
 * joinRoom 的「讀取 → 修改 → 寫入」序列以 synchronized(room) 確保同一房間無競態。
 */
@Service
public class MatchmakingServiceImpl implements MatchmakingService {

    private static final int MAX_PLAYERS = 4;

    private final GameRoomRepository roomRepository;
    private final GameEngineService  gameEngine;

    public MatchmakingServiceImpl(GameRoomRepository roomRepository, GameEngineService gameEngine) {
        this.roomRepository = roomRepository;
        this.gameEngine     = gameEngine;
    }

    // ── createSoloRoom() ─────────────────────────────────────────────────────

    @Override
    public GameRoom createSoloRoom(String hostName) {
        String roomId = UUID.randomUUID().toString();
        String hostId = UUID.randomUUID().toString();

        Player host = Player.builder()
                .id(hostId)
                .name(hostName)
                .ai(false)
                .build();

        Player ai1 = Player.builder()
                .id(UUID.randomUUID().toString())
                .name("AI-Havana")
                .ai(true)
                .build();
        Player ai2 = Player.builder()
                .id(UUID.randomUUID().toString())
                .name("AI-Cuba")
                .ai(true)
                .build();
        Player ai3 = Player.builder()
                .id(UUID.randomUUID().toString())
                .name("AI-Domino")
                .ai(true)
                .build();

        GameRoom room = GameRoom.builder()
                .roomId(roomId)
                .players(new ArrayList<>(List.of(host, ai1, ai2, ai3)))
                .status(GameStatus.WAITING)
                .build();

        gameEngine.initGame(room);
        return roomRepository.save(room);
    }

    // ── createRoom() ─────────────────────────────────────────────────────────

    @Override
    public GameRoom createRoom(String hostName) {
        String roomId = UUID.randomUUID().toString();
        String hostId = UUID.randomUUID().toString();

        Player host = Player.builder()
                .id(hostId)
                .name(hostName)
                .build();

        GameRoom room = GameRoom.builder()
                .roomId(roomId)
                .players(new ArrayList<>())
                .status(GameStatus.WAITING)
                .build();

        room.getPlayers().add(host);
        return roomRepository.save(room);
    }

    // ── joinRoom() ───────────────────────────────────────────────────────────

    @Override
    public GameRoom joinRoom(String roomId, String playerName) {
        GameRoom room = getRoom(roomId);

        synchronized (room) {
            if (room.getPlayers().size() >= MAX_PLAYERS) {
                throw new RoomFullException(roomId);
            }
            if (room.getStatus() != GameStatus.WAITING) {
                throw new RoomFullException(roomId);
            }

            Player newPlayer = Player.builder()
                    .id(UUID.randomUUID().toString())
                    .name(playerName)
                    .build();

            room.getPlayers().add(newPlayer);

            if (room.getPlayers().size() == MAX_PLAYERS) {
                gameEngine.initGame(room);
            }

            roomRepository.save(room);
        }

        return room;
    }

    // ── getRoom() ────────────────────────────────────────────────────────────

    @Override
    public GameRoom getRoom(String roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException(roomId));
    }
}
