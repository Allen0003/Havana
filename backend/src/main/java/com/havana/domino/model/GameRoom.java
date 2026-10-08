package com.havana.domino.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 遊戲房間聚合根。
 * 持有遊戲所需的完整狀態，包含玩家列表、桌面、備用庫與回合資訊。
 */
public class GameRoom {

    /** 房間唯一識別碼 */
    private String roomId;

    /** 玩家列表（順序即為出牌順序） */
    private List<Player> players = new ArrayList<>();

    /** 桌面狀態 */
    private Board board = new Board();

    /** 備用庫（未發出的牌） */
    private List<Tile> boneyard = new ArrayList<>();

    /** 房間目前狀態 */
    private GameStatus status = GameStatus.WAITING;

    /** 當前輪次玩家在 players 列表中的索引 */
    private int currentTurnIndex = 0;

    /** 勝者 playerId；null 表示尚未結束 */
    private String winnerId;

    /** 是否宣告死局（Trancado） */
    private boolean trancado = false;

    // ── 無參建構子 ────────────────────────────────────────────────────────────

    public GameRoom() {}

    // ── 全參建構子（供 Builder 使用） ─────────────────────────────────────────

    private GameRoom(String roomId, List<Player> players, Board board,
                     List<Tile> boneyard, GameStatus status,
                     int currentTurnIndex, String winnerId, boolean trancado) {
        this.roomId           = roomId;
        this.players          = players;
        this.board            = board;
        this.boneyard         = boneyard;
        this.status           = status;
        this.currentTurnIndex = currentTurnIndex;
        this.winnerId         = winnerId;
        this.trancado         = trancado;
    }

    // ── 靜態 Builder ──────────────────────────────────────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String     roomId;
        private List<Player> players        = new ArrayList<>();
        private Board      board            = new Board();
        private List<Tile> boneyard         = new ArrayList<>();
        private GameStatus status           = GameStatus.WAITING;
        private int        currentTurnIndex = 0;
        private String     winnerId;
        private boolean    trancado         = false;

        public Builder roomId(String roomId)                    { this.roomId = roomId;                     return this; }
        public Builder players(List<Player> players)            { this.players = players;                   return this; }
        public Builder board(Board board)                       { this.board = board;                       return this; }
        public Builder boneyard(List<Tile> boneyard)            { this.boneyard = boneyard;                 return this; }
        public Builder status(GameStatus status)                { this.status = status;                     return this; }
        public Builder currentTurnIndex(int currentTurnIndex)   { this.currentTurnIndex = currentTurnIndex; return this; }
        public Builder winnerId(String winnerId)                { this.winnerId = winnerId;                 return this; }
        public Builder trancado(boolean trancado)               { this.trancado = trancado;                 return this; }

        public GameRoom build() {
            return new GameRoom(roomId, players, board, boneyard,
                                status, currentTurnIndex, winnerId, trancado);
        }
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getRoomId()                        { return roomId; }
    public void   setRoomId(String roomId)           { this.roomId = roomId; }

    public List<Player> getPlayers()                        { return players; }
    public void         setPlayers(List<Player> players)    { this.players = players; }

    public Board getBoard()               { return board; }
    public void  setBoard(Board board)    { this.board = board; }

    public List<Tile> getBoneyard()                   { return boneyard; }
    public void       setBoneyard(List<Tile> boneyard){ this.boneyard = boneyard; }

    public GameStatus getStatus()                  { return status; }
    public void       setStatus(GameStatus status) { this.status = status; }

    public int  getCurrentTurnIndex()                        { return currentTurnIndex; }
    public void setCurrentTurnIndex(int currentTurnIndex)    { this.currentTurnIndex = currentTurnIndex; }

    public String getWinnerId()                    { return winnerId; }
    public void   setWinnerId(String winnerId)     { this.winnerId = winnerId; }

    public boolean isTrancado()                { return trancado; }
    public void    setTrancado(boolean trancado){ this.trancado = trancado; }

    // ── 便利方法 ──────────────────────────────────────────────────────────────

    /** 取得當前輪次玩家 */
    public Player currentPlayer() {
        return players.get(currentTurnIndex);
    }

    /** 推進至下一位玩家（循環） */
    public void advanceTurn() {
        currentTurnIndex = (currentTurnIndex + 1) % players.size();
    }

    /** 根據 playerId 尋找玩家 */
    public Player findPlayer(String playerId) {
        return players.stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId));
    }

    // ── equals / hashCode / toString ─────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameRoom gameRoom = (GameRoom) o;
        return currentTurnIndex == gameRoom.currentTurnIndex
                && trancado == gameRoom.trancado
                && Objects.equals(roomId, gameRoom.roomId)
                && Objects.equals(players, gameRoom.players)
                && Objects.equals(board, gameRoom.board)
                && Objects.equals(boneyard, gameRoom.boneyard)
                && status == gameRoom.status
                && Objects.equals(winnerId, gameRoom.winnerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomId, players, board, boneyard,
                            status, currentTurnIndex, winnerId, trancado);
    }

    @Override
    public String toString() {
        return "GameRoom{roomId='" + roomId + "', status=" + status
                + ", players=" + players.size()
                + ", currentTurnIndex=" + currentTurnIndex
                + ", trancado=" + trancado + "}";
    }
}
