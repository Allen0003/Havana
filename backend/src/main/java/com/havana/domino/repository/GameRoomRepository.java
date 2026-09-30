package com.havana.domino.repository;

import com.havana.domino.model.GameRoom;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 記憶體 GameRoom 儲存庫。
 *
 * <p>MVP 階段以 {@link ConcurrentHashMap} 作為後端；
 * 未來可無縫抽換為 Redis 實作（只需提供相同介面的 Bean）。
 */
@Repository
public class GameRoomRepository {

    private final ConcurrentHashMap<String, GameRoom> store = new ConcurrentHashMap<>();

    /**
     * 儲存（新增或更新）一個房間。
     *
     * @param room 要儲存的房間（roomId 不可為 null）
     * @return 儲存後的房間（同一物件）
     */
    public GameRoom save(GameRoom room) {
        if (room.getRoomId() == null) {
            throw new IllegalArgumentException("GameRoom.roomId must not be null");
        }
        store.put(room.getRoomId(), room);
        return room;
    }

    /**
     * 依 roomId 查詢房間。
     *
     * @param roomId 房間識別碼
     * @return 包含房間的 Optional；找不到時回傳 {@link Optional#empty()}
     */
    public Optional<GameRoom> findById(String roomId) {
        return Optional.ofNullable(store.get(roomId));
    }

    /**
     * 取得所有房間（快照，非 live view）。
     */
    public Collection<GameRoom> findAll() {
        return store.values();
    }

    /**
     * 刪除指定房間。若房間不存在，靜默成功。
     *
     * @param roomId 要刪除的房間 id
     */
    public void deleteById(String roomId) {
        store.remove(roomId);
    }

    /**
     * 查詢目前儲存的房間數量。
     */
    public int count() {
        return store.size();
    }

    /**
     * 清空所有房間（主要用於測試重置）。
     */
    public void clear() {
        store.clear();
    }
}
