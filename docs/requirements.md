# Havana — 需求規格書 (Requirements)

> 版本：1.0 | 基準：spec.md

---

## 1. 專案目標

打造一個支援多人即時對戰的古巴骨牌（Domino Cubano）線上平台。玩家可透過瀏覽器建立或加入房間、進行完整的 4 人制（或 2v2 組隊制）骨牌對局，系統負責裁判所有牌面規則並即時廣播盤面狀態。

---

## 2. 功能性需求 (Functional Requirements)

### FR-01 牌組管理
- 系統須使用雙九制（Double-Nine）55 張骨牌（點數組合 0-0 至 9-9）。
- 每張骨牌具備唯一 `id`、`left`、`right` 點數與 `isDouble` 標誌。

### FR-02 房間管理
- **FR-02-1**：玩家可透過 `POST /api/rooms` 建立新房間，系統回傳唯一 `roomId`，初始狀態為 `WAITING`。
- **FR-02-2**：玩家可透過 `POST /api/rooms/{roomId}/join` 加入現有房間，房間最多容納 4 名玩家。
- **FR-02-3**：可透過 `GET /api/rooms/{roomId}` 查詢房間完整狀態（`GameRoomDTO`）。
- **FR-02-4**：房間狀態機：`WAITING` → `IN_PROGRESS` → `FINISHED`。

### FR-03 發牌
- 4 名玩家就緒後，系統自動洗牌並發牌：每人 10 張，剩餘 15 張置入備用庫（Boneyard）。
- 發牌結果僅對各玩家本人可見（伺服器不在廣播中暴露他人手牌）。

### FR-04 出牌邏輯
- **FR-04-1**：首局由手持雙九（`9|9`）的玩家先出，出牌必須為 `9|9`。
- **FR-04-2**：後續出牌須匹配桌面左端或右端的點數；玩家需指定 `targetEnd`（`LEFT` / `RIGHT`）。
- **FR-04-3**：系統須驗證出牌合法性，非法出牌回傳錯誤訊息且不改變盤面。

### FR-05 過牌（Pass / Knock）
- 當玩家手牌中沒有任何可匹配桌面兩端點數的骨牌時，玩家可透過 `/app/game.pass` 宣告過牌。
- 系統須驗證玩家確實無牌可出才允許過牌，否則拒絕。

### FR-06 死局判定（Trancado）
- 當所有玩家（含手牌非空）均無法出牌時，系統自動宣告死局。
- 系統計算每位玩家剩餘手牌點數總和，點數最小者獲勝；同分時共同獲勝。
- 死局結果須廣播至房間所有玩家。

### FR-07 勝利條件
- 玩家手牌清空時，該玩家/隊伍獲勝，系統廣播勝利結果，房間狀態轉為 `FINISHED`。

### FR-08 即時狀態同步
- 每次合法出牌或死局後，系統透過 `/topic/room/{roomId}` 廣播最新 `GameRoomDTO`，包含：
  - 桌面兩端點數（`leftEnd`、`rightEnd`）
  - 桌面骨牌排列（`boardTiles`）
  - 各玩家手牌數量（不暴露牌面）
  - 備用庫剩餘數量（`boneyardCount`）
  - 當前輪到的玩家 `currentTurnPlayerId`
  - 房間狀態 `status`

### FR-09 WebSocket 連線管理
- 客戶端透過 SockJS + STOMP 連線至 `/ws-havana`。
- 客戶端可訂閱 `/topic/room/{roomId}` 接收廣播。
- 斷線重連後可重新訂閱並取得最新狀態。

---

## 3. 非功能性需求 (Non-Functional Requirements)

### NFR-01 效能
- WebSocket 訊息廣播延遲目標：P95 < 100ms（單一遊戲伺服器，4 名玩家）。
- REST API 回應時間目標：P95 < 200ms。

### NFR-02 可靠性
- 遊戲狀態存放於記憶體（MVP 階段）；後期可抽換為 Redis。
- 伺服器重啟後，進行中的房間狀態不保留（MVP 接受此限制）。

### NFR-03 安全性
- 每位玩家僅能操作自己的手牌，後端須驗證 `playerId` 與當前輪次匹配。
- 不得在廣播訊息中暴露其他玩家的手牌牌面。

### NFR-04 可維護性
- 後端遵循 Spring Boot 分層架構（Controller → Service → Repository）。
- 前端遵循 Angular 模組化設計，使用 Signals 作為狀態管理。
- 核心遊戲邏輯（`GameEngineService`）須有單元測試覆蓋。

### NFR-05 相容性
- 後端：Java 17 + Spring Boot 3.x + Maven。
- 前端：Angular 17+，支援現代瀏覽器（Chrome、Firefox、Safari、Edge 最新兩版）。

---

## 4. 系統邊界與假設

| 項目 | 假設 / 限制 |
| :--- | :--- |
| 玩家數量 | 固定 4 人（MVP），2v2 組隊為後續擴充 |
| 認證 | MVP 無身份驗證，以 `playerName` 辨識玩家 |
| 持久化 | 記憶體儲存（`ConcurrentHashMap`），無資料庫 |
| 國際化 | 介面以繁體中文為主，後續可擴充 |
| 行動裝置 | 響應式設計為優先考量，但非 MVP 硬需求 |
