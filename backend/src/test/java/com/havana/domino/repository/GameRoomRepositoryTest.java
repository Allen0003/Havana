package com.havana.domino.repository;

import com.havana.domino.model.GameRoom;
import com.havana.domino.model.GameStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DisplayName("GameRoomRepository")
class GameRoomRepositoryTest {

    private GameRoomRepository repo;

    @BeforeEach
    void setUp() {
        repo = new GameRoomRepository();
    }

    // ── 輔助 ──────────────────────────────────────────────────────────────────

    private GameRoom buildRoom(String roomId) {
        return GameRoom.builder()
                .roomId(roomId)
                .status(GameStatus.WAITING)
                .build();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // save()
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("save()")
    class Save {

        @Test @DisplayName("儲存後 count() = 1")
        void saveSingleRoom() {
            repo.save(buildRoom("room-1"));
            assertThat(repo.count()).isEqualTo(1);
        }

        @Test @DisplayName("相同 roomId 再次儲存 → 覆蓋，count 不增加")
        void saveOverwritesExisting() {
            repo.save(buildRoom("room-1"));
            GameRoom updated = buildRoom("room-1");
            updated.setStatus(GameStatus.IN_PROGRESS);
            repo.save(updated);

            assertThat(repo.count()).isEqualTo(1);
            assertThat(repo.findById("room-1"))
                    .isPresent()
                    .hasValueSatisfying(r ->
                            assertThat(r.getStatus()).isEqualTo(GameStatus.IN_PROGRESS));
        }

        @Test @DisplayName("roomId 為 null → 拋出 IllegalArgumentException")
        void saveNullRoomId_throws() {
            GameRoom room = GameRoom.builder().build();
            assertThatThrownBy(() -> repo.save(room))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test @DisplayName("save() 回傳被儲存的房間物件")
        void saveReturnsSameRoom() {
            GameRoom room = buildRoom("room-x");
            assertThat(repo.save(room)).isSameAs(room);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // findById()
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test @DisplayName("存在的 roomId → 回傳 Optional 包含房間")
        void existingRoom_found() {
            repo.save(buildRoom("room-2"));
            Optional<GameRoom> result = repo.findById("room-2");
            assertThat(result).isPresent();
            assertThat(result.get().getRoomId()).isEqualTo("room-2");
        }

        @Test @DisplayName("不存在的 roomId → 回傳 empty")
        void missingRoom_empty() {
            assertThat(repo.findById("no-such-room")).isEmpty();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // findAll()
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test @DisplayName("空倉庫 → 回傳空集合")
        void empty_returnsEmptyCollection() {
            assertThat(repo.findAll()).isEmpty();
        }

        @Test @DisplayName("儲存 3 個房間 → 回傳 3 個")
        void threeRooms_returnsThree() {
            repo.save(buildRoom("r1"));
            repo.save(buildRoom("r2"));
            repo.save(buildRoom("r3"));
            assertThat(repo.findAll()).hasSize(3);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // deleteById()
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("deleteById()")
    class DeleteById {

        @Test @DisplayName("刪除已存在的房間 → 找不到該房間")
        void deleteExisting_removed() {
            repo.save(buildRoom("room-3"));
            repo.deleteById("room-3");
            assertThat(repo.findById("room-3")).isEmpty();
        }

        @Test @DisplayName("刪除不存在的 roomId → 靜默成功，count 不變")
        void deleteNonExistent_noop() {
            repo.save(buildRoom("room-4"));
            repo.deleteById("no-such");
            assertThat(repo.count()).isEqualTo(1);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // count() & clear()
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("count() & clear()")
    class CountAndClear {

        @Test @DisplayName("初始 count = 0")
        void initialCountIsZero() {
            assertThat(repo.count()).isEqualTo(0);
        }

        @Test @DisplayName("clear() 後 count = 0，findAll() 為空")
        void clearResetsState() {
            repo.save(buildRoom("r1"));
            repo.save(buildRoom("r2"));
            repo.clear();
            assertThat(repo.count()).isEqualTo(0);
            assertThat(repo.findAll()).isEmpty();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 並發安全（基本煙霧測試）
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("ConcurrentHashMap：多執行緒並行 save 不拋出例外")
    void concurrentSave_noException() throws InterruptedException {
        int threadCount = 20;
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final String id = "room-" + i;
            threads[i] = new Thread(() -> repo.save(buildRoom(id)));
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        assertThat(repo.count()).isEqualTo(threadCount);
    }
}
