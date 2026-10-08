package com.havana.domino.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 玩家領域模型。
 * hand 為伺服器完整持有的手牌清單，不對外廣播牌面。
 */
public class Player {

    /** 玩家唯一識別碼（UUID） */
    private String id;

    /** 玩家顯示名稱 */
    private String name;

    /** 玩家手牌（僅伺服器端持有完整內容） */
    private List<Tile> hand = new ArrayList<>();

    /** WebSocket 連線狀態 */
    private boolean connected = true;

    /** 是否為 AI 玩家（Bot） */
    private boolean ai = false;

    // ── 無參建構子 ────────────────────────────────────────────────────────────

    public Player() {}

    // ── 全參建構子（供 Builder 使用） ─────────────────────────────────────────

    private Player(String id, String name, List<Tile> hand, boolean connected, boolean ai) {
        this.id        = id;
        this.name      = name;
        this.hand      = hand;
        this.connected = connected;
        this.ai        = ai;
    }

    // ── 靜態 Builder ──────────────────────────────────────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String     id;
        private String     name;
        private List<Tile> hand      = new ArrayList<>();
        private boolean    connected = true;
        private boolean    ai        = false;

        public Builder id(String id)               { this.id = id;             return this; }
        public Builder name(String name)           { this.name = name;         return this; }
        public Builder hand(List<Tile> hand)       { this.hand = hand;         return this; }
        public Builder connected(boolean connected){ this.connected = connected; return this; }
        public Builder ai(boolean ai)              { this.ai = ai;             return this; }

        public Player build() {
            return new Player(id, name, hand, connected, ai);
        }
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getId()                { return id; }
    public void   setId(String id)       { this.id = id; }

    public String getName()              { return name; }
    public void   setName(String name)   { this.name = name; }

    public List<Tile> getHand()              { return hand; }
    public void       setHand(List<Tile> h)  { this.hand = h; }

    public boolean isConnected()                  { return connected; }
    public void    setConnected(boolean connected) { this.connected = connected; }

    public boolean isAi()            { return ai; }
    public void    setAi(boolean ai) { this.ai = ai; }

    // ── 商業邏輯方法 ──────────────────────────────────────────────────────────

    /** 取得手牌中剩餘點數總和（用於死局計分） */
    public int handPipTotal() {
        return hand.stream().mapToInt(Tile::totalPips).sum();
    }

    // ── equals / hashCode / toString ─────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return connected == player.connected
                && ai == player.ai
                && Objects.equals(id, player.id)
                && Objects.equals(name, player.name)
                && Objects.equals(hand, player.hand);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, hand, connected, ai);
    }

    @Override
    public String toString() {
        return "Player{id='" + id + "', name='" + name
                + "', handSize=" + hand.size()
                + ", connected=" + connected
                + ", ai=" + ai + "}";
    }
}
