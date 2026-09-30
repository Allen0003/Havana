package com.havana.domino.service;

import com.havana.domino.exception.RoomFullException;
import com.havana.domino.exception.RoomNotFoundException;
import com.havana.domino.model.GameRoom;
import com.havana.domino.model.GameStatus;
import com.havana.domino.model.Player;
import com.havana.domino.repository.GameRoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MatchmakingService")
class MatchmakingServiceTest {

    @Mock  GameRoomRepository roomRepository;
    @Mock  GameEngineService  gameEngine;
    @InjectMocks MatchmakingServiceImpl service;

    // save() 預設：直接回傳傳入的 room
    @BeforeEach
    void stubSave() {
        when(roomRepository.save(any(GameRoom.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ─── createRoom() ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createRoom()")
    class CreateRoom {

        @Test
        @DisplayName("回傳 WAITING 狀態的房間")
        void returnsWaitingRoom() {
            GameRoom room = service.createRoom("Alice");
            assertThat(room.getStatus()).isEqualTo(GameStatus.WAITING);
        }

        @Test
        @DisplayName("房主被加入為第一位玩家")
        void hostIsFirstPlayer() {
            GameRoom room = service.createRoom("Alice");
            assertThat(room.getPlayers()).hasSize(1);
            assertThat(room.getPlayers().get(0).getName()).isEqualTo("Alice");
        }

        @Test
        @DisplayName("roomId 與 playerId 均為非空 UUID 字串")
        void idsAreNonBlank() {
            GameRoom room = service.createRoom("Bob");
            assertThat(room.getRoomId()).isNotBlank();
            assertThat(room.getPlayers().get(0).getId()).isNotBlank();
        }

        @Test
        @DisplayName("兩次 createRoom 的 roomId 不同")
        void roomIdsAreUnique() {
            GameRoom r1 = service.createRoom("Alice");
            GameRoom r2 = service.createRoom("Bob");
            assertThat(r1.getRoomId()).isNotEqualTo(r2.getRoomId());
        }

        @Test
        @DisplayName("呼叫 repository.save() 一次")
        void savesOnce() {
            service.createRoom("Alice");
            verify(roomRepository, times(1)).save(any(GameRoom.class));
        }
    }

    // ─── joinRoom() ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("joinRoom()")
    class JoinRoom {

        private GameRoom stubRoom(int currentPlayers) {
            GameRoom room = GameRoom.builder()
                    .roomId("room-1")
                    .players(new ArrayList<>())
                    .status(GameStatus.WAITING)
                    .build();
            for (int i = 0; i < currentPlayers; i++) {
                room.getPlayers().add(
                        Player.builder().id("p" + i).name("Player" + i).build());
            }
            when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));
            return room;
        }

        @Test
        @DisplayName("加入成功後玩家列表增加一人")
        void playerAdded() {
            stubRoom(1);
            GameRoom room = service.joinRoom("room-1", "Bob");
            assertThat(room.getPlayers()).hasSize(2);
            assertThat(room.getPlayers().get(1).getName()).isEqualTo("Bob");
        }

        @Test
        @DisplayName("加入玩家擁有唯一 UUID id")
        void newPlayerHasId() {
            stubRoom(1);
            GameRoom room = service.joinRoom("room-1", "Bob");
            assertThat(room.getPlayers().get(1).getId()).isNotBlank();
        }

        @Test
        @DisplayName("第 4 位玩家加入後觸發 initGame()")
        void fourthPlayerTriggersInitGame() {
            GameRoom room = stubRoom(3);
            when(gameEngine.initGame(room)).thenReturn(room);

            service.joinRoom("room-1", "Dave");

            verify(gameEngine, times(1)).initGame(room);
        }

        @Test
        @DisplayName("第 3 位玩家加入後不觸發 initGame()")
        void thirdPlayerDoesNotTriggerInitGame() {
            stubRoom(2);
            service.joinRoom("room-1", "Carol");
            verify(gameEngine, never()).initGame(any());
        }

        @Test
        @DisplayName("房間不存在 → 拋出 RoomNotFoundException")
        void roomNotFound_throws() {
            when(roomRepository.findById("bad-id")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.joinRoom("bad-id", "X"))
                    .isInstanceOf(RoomNotFoundException.class)
                    .hasMessageContaining("bad-id");
        }

        @Test
        @DisplayName("房間已有 4 人 → 拋出 RoomFullException")
        void roomFull_throws() {
            stubRoom(4);
            assertThatThrownBy(() -> service.joinRoom("room-1", "Eve"))
                    .isInstanceOf(RoomFullException.class)
                    .hasMessageContaining("room-1");
        }

        @Test
        @DisplayName("房間狀態非 WAITING → 拋出 RoomFullException")
        void roomNotWaiting_throws() {
            GameRoom room = GameRoom.builder()
                    .roomId("room-1")
                    .players(new ArrayList<>())
                    .status(GameStatus.IN_PROGRESS)
                    .build();
            room.getPlayers().add(Player.builder().id("p0").name("Alice").build());
            when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));

            assertThatThrownBy(() -> service.joinRoom("room-1", "Bob"))
                    .isInstanceOf(RoomFullException.class);
        }
    }

    // ─── getRoom() ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getRoom()")
    class GetRoom {

        @Test
        @DisplayName("存在的 roomId → 回傳房間")
        void found() {
            GameRoom room = GameRoom.builder().roomId("r1").build();
            when(roomRepository.findById("r1")).thenReturn(Optional.of(room));
            assertThat(service.getRoom("r1")).isSameAs(room);
        }

        @Test
        @DisplayName("不存在的 roomId → 拋出 RoomNotFoundException")
        void notFound_throws() {
            when(roomRepository.findById("nope")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.getRoom("nope"))
                    .isInstanceOf(RoomNotFoundException.class);
        }
    }
}
