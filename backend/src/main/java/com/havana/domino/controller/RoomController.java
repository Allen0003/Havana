package com.havana.domino.controller;

import com.havana.domino.dto.*;
import com.havana.domino.model.GameRoom;
import com.havana.domino.service.MatchmakingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API：遊戲房間管理。
 *
 * <pre>
 * POST  /api/rooms               建立房間
 * POST  /api/rooms/{roomId}/join 加入房間
 * GET   /api/rooms/{roomId}      查詢房間狀態
 * </pre>
 */
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final MatchmakingService matchmakingService;

    /**
     * 建立新遊戲房間。
     * 回傳 201 Created 與 {@link CreateRoomResponse}。
     */
    @PostMapping
    public ResponseEntity<CreateRoomResponse> createRoom(
            @Valid @RequestBody CreateRoomRequest request) {

        GameRoom room = matchmakingService.createRoom(request.hostName());
        String hostPlayerId = room.getPlayers().get(0).getId();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new CreateRoomResponse(room.getRoomId(), room.getStatus(), hostPlayerId));
    }

    /**
     * 加入現有房間。
     * 回傳 200 OK 與 {@link JoinRoomResponse}。
     * 若房間不存在回 404；若房間已滿回 409。
     */
    @PostMapping("/{roomId}/join")
    public ResponseEntity<JoinRoomResponse> joinRoom(
            @PathVariable String roomId,
            @Valid @RequestBody JoinRoomRequest request) {

        GameRoom room = matchmakingService.joinRoom(roomId, request.playerName());

        // 找到剛加入的玩家（最後一位）
        String newPlayerId = room.getPlayers()
                .get(room.getPlayers().size() - 1).getId();

        return ResponseEntity.ok(new JoinRoomResponse(
                true,
                newPlayerId,
                room.getPlayers().stream().map(PlayerDTO::from).toList()
        ));
    }

    /**
     * 查詢房間目前狀態（廣播安全的 GameRoomDTO）。
     * 若房間不存在回 404。
     */
    @GetMapping("/{roomId}")
    public ResponseEntity<GameRoomDTO> getRoom(@PathVariable String roomId) {
        GameRoom room = matchmakingService.getRoom(roomId);
        return ResponseEntity.ok(GameRoomDTO.from(room));
    }
}
