# Havana — 開發任務清單 (Tasks)

> 版本：1.0 | 對應文件：requirements.md、design.md

任務分為五個里程碑（Milestone），每個里程碑產出可獨立驗收的功能切片。

---

## Milestone 0 — 專案骨架與基礎設定

| # | 任務 | 負責端 | 優先 | 完成條件 |
|---|------|--------|------|---------|
| M0-1 | 建立 Spring Boot 3 Maven 專案（`pom.xml`，引入 Web / WebSocket / Lombok） | Backend | P0 | `mvn clean compile` 成功 |
| M0-2 | 設定 `WebSocketConfig`（SockJS endpoint、STOMP broker） | Backend | P0 | WebSocket 端點可連線 |
| M0-3 | 建立 Angular 17+ 專案（`ng new`，Standalone Components） | Frontend | P0 | `ng serve` 無錯誤啟動 |
| M0-4 | 安裝前端依賴：`@stomp/stompjs`、`sockjs-client`、Tailwind CSS | Frontend | P0 | 套件出現於 `package.json` |
| M0-5 | 設定 `environments/` 的 API base URL 與 WS endpoint | Frontend | P0 | 環境變數可正確讀取 |
| M0-6 | 建立 CI 工作流程骨架（GitHub Actions：後端 Maven build、前端 ng build） | DevOps | P1 | CI pipeline 可執行 |

---

## Milestone 1 — 領域模型與核心遊戲引擎

| # | 任務 | 負責端 | 優先 | 完成條件 |
|---|------|--------|------|---------|
| M1-1 | 實作 `Tile` record（`id`, `left`, `right`, `isDouble`, `canMatch`, `totalPips`） | Backend | P0 | 單元測試通過 |
| M1-2 | 實作 `Player`、`Board`、`GameRoom` model | Backend | P0 | 編譯通過，無警告 |
| M1-3 | 實作 `GameEngineService.generateTileSet()`（55 張牌驗證） | Backend | P0 | 單元測試：牌組長度 = 55，無重複 |
| M1-4 | 實作 `GameEngineService.initGame()`（洗牌、發牌、設定首出玩家） | Backend | P0 | 單元測試：每人 10 張、boneyard 15 張、首出玩家持有 9\|9 |
| M1-5 | 實作 `GameEngineService.playTile()`（出牌驗證與桌面更新） | Backend | P0 | 單元測試：合法出牌、非法出牌（牌不匹配、非本人回合）均有測試 |
| M1-6 | 實作 `GameEngineService.passTurn()`（過牌驗證） | Backend | P0 | 單元測試：有牌可出時拒絕過牌 |
| M1-7 | 實作 `GameEngineService.checkWinner()`（手牌清空判斷） | Backend | P0 | 單元測試通過 |
| M1-8 | 實作 `GameEngineService.isTrancado()` 與 `calculateTrancadoScores()` | Backend | P0 | 單元測試：模擬死局情境，分數計算正確 |
| M1-9 | 實作 `GameRoomRepository`（`ConcurrentHashMap` CRUD） | Backend | P0 | 編譯通過 |

---

## Milestone 2 — REST API 與房間管理

| # | 任務 | 負責端 | 優先 | 完成條件 |
|---|------|--------|------|---------|
| M2-1 | 實作 `MatchmakingService`（建立房間、加入房間） | Backend | P0 | 單元測試通過 |
| M2-2 | 實作 `RoomController`：`POST /api/rooms` | Backend | P0 | Postman/curl 可建立房間並取得 `roomId` |
| M2-3 | 實作 `RoomController`：`POST /api/rooms/{roomId}/join` | Backend | P0 | 第 4 位玩家加入後，房間狀態自動轉為 `IN_PROGRESS` 且觸發 `initGame` |
| M2-4 | 實作 `RoomController`：`GET /api/rooms/{roomId}` | Backend | P0 | 回傳 `GameRoomDTO`（不含他人手牌） |
| M2-5 | 實作 `GameApiService`（Angular HTTP Client，呼叫 M2-2 ~ M2-4） | Frontend | P0 | 服務方法有正確型別定義 |
| M2-6 | 建立 Lobby 頁面（建立 / 加入房間 UI） | Frontend | P1 | 玩家可建立房間並分享 `roomId` |

---

## Milestone 3 — WebSocket 即時對戰

| # | 任務 | 負責端 | 優先 | 完成條件 |
|---|------|--------|------|---------|
| M3-1 | 實作 `GameWebSocketController`：`/app/game.play` | Backend | P0 | 合法出牌後廣播更新的 `GameRoomDTO` |
| M3-2 | 實作 `GameWebSocketController`：`/app/game.pass` | Backend | P0 | 過牌後廣播並推進回合 |
| M3-3 | 整合死局 / 勝利判斷至 WebSocket 處理流程 | Backend | P0 | 勝利 / 死局時廣播含 `winnerId` / `trancado` 的 DTO |
| M3-4 | 實作 `WebSocketService`（Angular）：建立 STOMP 連線、訂閱 topic | Frontend | P0 | 連線後可接收廣播訊息 |
| M3-5 | 實作 `WebSocketService.playTile()` 與 `passTurn()` 方法 | Frontend | P0 | 點擊出牌後訊息可送達後端 |
| M3-6 | 建立 `game.store.ts`（Signals：`gameRoomSignal`, `myHandSignal`, `isMyTurn`） | Frontend | P0 | Signal 變更後 UI 自動更新 |
| M3-7 | 實作 `board/` component（渲染桌面骨牌序列） | Frontend | P0 | 桌面骨牌正確顯示左右兩端點數 |
| M3-8 | 實作 `hand/` component（渲染玩家手牌，可點擊出牌） | Frontend | P0 | 非本人回合時，手牌禁用點擊 |
| M3-9 | 實作 `score/` component（玩家手牌數、備用庫數、當前輪次） | Frontend | P1 | 數字隨廣播即時更新 |
| M3-10 | 加入 WS 斷線重連機制（指數退避，最多 5 次） | Frontend | P1 | 手動斷線後可自動重連 |

---

## Milestone 4 — 收尾、測試與優化

| # | 任務 | 負責端 | 優先 | 完成條件 |
|---|------|--------|------|---------|
| M4-1 | 後端整合測試：完整一局 4 人遊戲流程（含死局） | Backend | P0 | 所有情境測試通過 |
| M4-2 | 前端 E2E 測試骨架（Cypress 或 Playwright） | Frontend | P1 | 至少有 Lobby → 出牌 → 勝利 基本流程 |
| M4-3 | 錯誤訊息 UI 顯示（非法出牌、連線失敗提示） | Frontend | P1 | 錯誤 toast/alert 正確顯示 |
| M4-4 | 遊戲結果頁面（Result component，顯示勝者或死局分數） | Frontend | P1 | 遊戲結束後自動跳轉 |
| M4-5 | 效能驗證：4 人同時出牌，P95 廣播延遲 < 100ms | Backend | P1 | 壓測結果符合 NFR-01 |
| M4-6 | 安全性審查：確認廣播 DTO 不暴露他人手牌 | Backend | P0 | Code review 確認 `GameRoomDTO` 僅含 `handCount` |
| M4-7 | README.md 補充本地端啟動說明（後端 `mvn spring-boot:run`、前端 `ng serve`） | Both | P2 | 新成員可依文件在 10 分鐘內啟動專案 |

---

## 附錄：任務依賴關係

```
M0-1 → M1-* → M2-1 → M2-2/M2-3/M2-4 → M3-1/M3-2/M3-3
M0-3 → M0-4 → M2-5/M2-6 → M3-4 → M3-5 → M3-6 → M3-7/M3-8/M3-9
M1-5, M1-6, M1-7, M1-8 → M3-1/M3-2/M3-3 → M4-1
```
