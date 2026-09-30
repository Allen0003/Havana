package com.havana.domino.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.havana.domino.dto.CreateRoomRequest;
import com.havana.domino.dto.JoinRoomRequest;
import com.havana.domino.exception.RoomFullException;
import com.havana.domino.exception.RoomNotFoundException;
import com.havana.domino.model.*;
import com.havana.domino.service.MatchmakingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.havana.domino.exception.GlobalExceptionHandler;

@WebMvcTest(RoomController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("RoomController (MockMvc)")
class RoomControllerTest {

    @Autowired MockMvc       mockMvc;
    @Autowired ObjectMapper  objectMapper;
    @MockBean MatchmakingService matchmakingService;

    // ── 輔助：建立測試房間物件 ────────────────────────────────────────────────

    private GameRoom buildWaitingRoom(String roomId, String hostId, String hostName) {
        Player host = Player.builder().id(hostId).name(hostName).build();
        GameRoom room = GameRoom.builder()
                .roomId(roomId)
                .players(new ArrayList<>(List.of(host)))
                .status(GameStatus.WAITING)
                .build();
        return room;
    }

    private GameRoom buildRoomWith(String roomId, int playerCount, GameStatus status) {
        GameRoom room = GameRoom.builder()
                .roomId(roomId)
                .players(new ArrayList<>())
                .status(status)
                .build();
        for (int i = 0; i < playerCount; i++) {
            room.getPlayers().add(
                    Player.builder().id("p" + i).name("Player" + i).build());
        }
        return room;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // POST /api/rooms
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/rooms")
    class CreateRoom {

        @Test
        @DisplayName("正常建立 → 201 Created，回傳 roomId 與 WAITING 狀態")
        void createRoom_success() throws Exception {
            GameRoom room = buildWaitingRoom("room-abc", "host-1", "Alice");
            when(matchmakingService.createRoom("Alice")).thenReturn(room);

            mockMvc.perform(post("/api/rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CreateRoomRequest("Alice"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.roomId").value("room-abc"))
                    .andExpect(jsonPath("$.status").value("WAITING"))
                    .andExpect(jsonPath("$.hostPlayerId").value("host-1"));
        }

        @Test
        @DisplayName("hostName 為空字串 → 400 Bad Request")
        void createRoom_blankName_returns400() throws Exception {
            mockMvc.perform(post("/api/rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CreateRoomRequest(""))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Body 缺少必要欄位 → 400 Bad Request")
        void createRoom_missingBody_returns400() throws Exception {
            mockMvc.perform(post("/api/rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // POST /api/rooms/{roomId}/join
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/rooms/{roomId}/join")
    class JoinRoom {

        @Test
        @DisplayName("正常加入 → 200 OK，回傳 playerId 與 players 列表")
        void joinRoom_success() throws Exception {
            GameRoom room = buildRoomWith("room-abc", 2, GameStatus.WAITING);
            when(matchmakingService.joinRoom(eq("room-abc"), eq("Bob")))
                    .thenReturn(room);

            mockMvc.perform(post("/api/rooms/room-abc/join")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new JoinRoomRequest("Bob"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.playerId").isNotEmpty())
                    .andExpect(jsonPath("$.players").isArray())
                    .andExpect(jsonPath("$.players", hasSize(2)));
        }

        @Test
        @DisplayName("房間不存在 → 404 Not Found")
        void joinRoom_notFound_returns404() throws Exception {
            when(matchmakingService.joinRoom(eq("bad-id"), anyString()))
                    .thenThrow(new RoomNotFoundException("bad-id"));

            mockMvc.perform(post("/api/rooms/bad-id/join")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new JoinRoomRequest("Bob"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("ROOM_NOT_FOUND"));
        }

        @Test
        @DisplayName("房間已滿 → 409 Conflict")
        void joinRoom_full_returns409() throws Exception {
            when(matchmakingService.joinRoom(eq("room-full"), anyString()))
                    .thenThrow(new RoomFullException("room-full"));

            mockMvc.perform(post("/api/rooms/room-full/join")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new JoinRoomRequest("Eve"))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("ROOM_FULL"));
        }

        @Test
        @DisplayName("playerName 為空白 → 400 Bad Request")
        void joinRoom_blankName_returns400() throws Exception {
            mockMvc.perform(post("/api/rooms/room-abc/join")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new JoinRoomRequest("   "))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GET /api/rooms/{roomId}
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/rooms/{roomId}")
    class GetRoom {

        @Test
        @DisplayName("存在的房間 → 200 OK，回傳 GameRoomDTO（不含手牌）")
        void getRoom_success() throws Exception {
            GameRoom room = buildRoomWith("room-abc", 2, GameStatus.WAITING);
            when(matchmakingService.getRoom("room-abc")).thenReturn(room);

            mockMvc.perform(get("/api/rooms/room-abc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.roomId").value("room-abc"))
                    .andExpect(jsonPath("$.status").value("WAITING"))
                    .andExpect(jsonPath("$.players").isArray())
                    .andExpect(jsonPath("$.players", hasSize(2)))
                    // 確認 DTO 不含手牌牌面，只有 handCount
                    .andExpect(jsonPath("$.players[0].handCount").isNumber())
                    .andExpect(jsonPath("$.players[0].hand").doesNotExist())
                    .andExpect(jsonPath("$.boneyardCount").isNumber())
                    .andExpect(jsonPath("$.trancado").value(false));
        }

        @Test
        @DisplayName("不存在的房間 → 404 Not Found")
        void getRoom_notFound_returns404() throws Exception {
            when(matchmakingService.getRoom("no-room"))
                    .thenThrow(new RoomNotFoundException("no-room"));

            mockMvc.perform(get("/api/rooms/no-room"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("ROOM_NOT_FOUND"))
                    .andExpect(jsonPath("$.detail").value(containsString("no-room")));
        }

        @Test
        @DisplayName("進行中的房間包含 currentTurnPlayerId")
        void getRoom_inProgress_hasCurrentTurnPlayerId() throws Exception {
            GameRoom room = buildRoomWith("room-abc", 4, GameStatus.IN_PROGRESS);
            when(matchmakingService.getRoom("room-abc")).thenReturn(room);

            mockMvc.perform(get("/api/rooms/room-abc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                    .andExpect(jsonPath("$.currentTurnPlayerId").isNotEmpty());
        }
    }
}
