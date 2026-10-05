package com.havana.domino.controller;

import com.havana.domino.dto.*;
import com.havana.domino.model.GameRoom;
import com.havana.domino.model.Player;
import com.havana.domino.service.MatchmakingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
     * 建立單人對戰房間（1 人 + 3 AI），立即開始遊戲。
     * 回傳 201 Created 與 {@link CreateRoomResponse}。
     */
    @PostMapping("/solo")
    public ResponseEntity<CreateRoomResponse> createSoloRoom(
            @Valid @RequestBody CreateRoomRequest request) {

        GameRoom room = matchmakingService.createSoloRoom(request.hostName());
        String hostPlayerId = room.getPlayers().stream()
                .filter(p -> !p.isAi())
                .findFirst()
                .map(p -> p.getId())
                .orElseThrow();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new CreateRoomResponse(room.getRoomId(), room.getStatus(), hostPlayerId));
    }

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

    /**
     * 取得指定玩家的個人手牌（僅限本人查詢）。
     *
     * <p>廣播 DTO 不含手牌牌面，此端點讓前端在進入遊戲後
     * 以及每次出牌後取得最新手牌。
     *
     * @param roomId   房間 ID
     * @param playerId 查詢者的玩家 ID
     * @return 該玩家目前的手牌列表；房間或玩家不存在回 404
     */
    @GetMapping("/{roomId}/hand")
    public ResponseEntity<List<TileDTO>> getHand(
            @PathVariable String roomId,
            @RequestParam String playerId) {

        GameRoom room = matchmakingService.getRoom(roomId);
        Player player = room.findPlayer(playerId);
        List<TileDTO> hand = player.getHand().stream()
                .map(TileDTO::from)
                .toList();
        return ResponseEntity.ok(hand);
    }
}
