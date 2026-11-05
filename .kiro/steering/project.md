---
inclusion: always
---

# Havana 專案開發規範

> 本規範適用於 Havana 古巴骨牌（Domino Cubano）線上即時對戰平台的所有開發工作。
> 每次對話自動套用，無需手動引入。

---

## 1. 技術架構

### 1.1 後端（Backend）

| 項目 | 規格 |
|---|---|
| 語言 | Java 17 |
| 框架 | Spring Boot 3.2.5 |
| 建置工具 | Maven 3.6+ |
| WebSocket | Spring WebSocket (STOMP) over SockJS，端點 `/ws-havana` |
| 驗證 | `spring-boot-starter-validation`（Jakarta Bean Validation） |
| 測試 | JUnit 5、AssertJ、Mockito、Spring MockMvc、`WebSocketStompClient` |
| 日誌 | SLF4J + `LoggerFactory.getLogger()`（**不使用 Lombok**） |

> **重要**：專案已移除 Lombok。所有 model 類別必須手寫 getter/setter、constructor、Builder（如需要）、`equals`/`hashCode`/`toString`。不得在新程式碼中引入 `import lombok.*`。

### 1.2 前端（Frontend）

| 項目 | 規格 |
|---|---|
| 框架 | Angular 20（`@angular/core` ^20.1.0） |
| 語言 | TypeScript ~5.8.2 |
| 元件架構 | Standalone Components（無 NgModule） |
| 狀態管理 | Angular Signals（`signal`、`computed`）— 不使用 NgRx |
| 樣式 | Tailwind CSS 3.4 |
| WebSocket | `@stomp/stompjs` ^7.0.0 + `sockjs-client` ^1.6.1 |
| 單元測試 | Karma + Jasmine |
| E2E 測試 | Playwright ^1.49.1 |
| 套件管理 | npm 10+（Node.js 22+） |

> **重要**：`main.ts` 最頂端必須保留 `(window as any)['global'] = window;`，這是 sockjs-client 的必要 polyfill。

### 1.3 REST API 與 WebSocket 的分工

```
REST API（HTTP）               WebSocket（STOMP/SockJS）
────────────────────           ──────────────────────────────────
房間生命週期管理                即時遊戲操作與狀態廣播
  POST /api/rooms              Client → Server：
  POST /api/rooms/solo           /app/game.play  （出牌）
  POST /api/rooms/{id}/join      /app/game.pass  （過牌）
  GET  /api/rooms/{id}         Server → Client：
  GET  /api/rooms/{id}/hand      /topic/room/{roomId}（廣播 GameRoomDTO）
```

- **REST** 負責：建立房間、加入房間、查詢房間快照、取得個人手牌。
- **WebSocket** 負責：出牌、過牌、盤面即時廣播。
- 兩者互補，不重疊：遊戲中的每一步操作走 WebSocket，房間初始化與手牌拉取走 REST。

### 1.4 開發代理設定

- 前端開發環境透過 `proxy.conf.json` 將 `/api` 和 `/ws-havana` 代理到 `http://localhost:8080`。
- `environment.ts` 使用相對路徑（`/api`、`/ws-havana`），**不直接寫死** `http://localhost:8080`。

---

## 2. 程式架構

### 2.1 後端分層規範

```
com.havana.domino
├── config/         WebSocket 設定（WebSocketConfig）
├── controller/     接收請求 → 呼叫 Service → 回傳回應 / 廣播
│   ├── RoomController.java          REST
│   └── GameWebSocketController.java WebSocket
├── service/        業務邏輯（interface + impl 分離）
│   ├── GameEngineService / Impl     核心遊戲規則
│   ├── MatchmakingService / Impl    房間管理
│   └── AiPlayerService / Impl      AI 決策
├── model/          領域物件（GameRoom、Player、Board、Tile、BoardEnd、GameStatus）
├── dto/            API 資料傳輸（Request / Response / DTO）
├── repository/     資料存取（GameRoomRepository，ConcurrentHashMap）
└── exception/      自訂例外（RoomNotFoundException、RoomFullException）
```

**各層職責邊界：**

| 層 | 可以做 | 不可以做 |
|---|---|---|
| Controller | 接收 HTTP/WS 請求、基本參數驗證、呼叫 Service、回傳 DTO | 直接操作 Repository、包含業務邏輯 |
| Service | 業務邏輯、呼叫 Repository、跨 Service 協作 | 直接回傳 HTTP 狀態碼、依賴 Controller |
| Repository | CRUD 操作、資料一致性保護 | 業務邏輯、依賴 Service |
| Model | 領域狀態與便利方法（`currentPlayer()`、`advanceTurn()`、`findPlayer()`） | 依賴任何其他層 |
| DTO | 資料結構定義、`from()` 工廠方法轉換 | 業務邏輯 |

**Service 介面設計**：所有 Service 以 interface + Impl 分離，測試時可 Mock interface。

### 2.2 前端架構規範

```
src/app/
├── core/
│   ├── models/     TypeScript interface（對應後端 DTO）
│   ├── services/   WebSocketService（STOMP 連線管理）
│   └── store/      Angular Signals 全域狀態（game.store.ts）
└── features/
    ├── lobby/      大廳頁面（建立/加入房間）
    ├── game/       遊戲頁面
    │   ├── board/  桌面骨牌渲染
    │   ├── hand/   玩家手牌
    │   └── score/  計分看板
    └── result/     遊戲結果頁
```

**Angular 架構規則：**

- **Standalone Components**：所有元件使用 `standalone: true`，不建立 NgModule。
- **Signals 狀態**：全域狀態統一放在 `core/store/game.store.ts`，以 `signal()` 和 `computed()` 管理，不直接在元件內持有共享狀態。
- **HTTP 呼叫**：在 `GameComponent.ngOnInit()` 中呼叫 REST 取得初始房間狀態及個人手牌，不依賴 WebSocket 廣播作為初始資料來源。
- **Lazy Loading**：`/` 和 `/game` 路由使用 lazy load（`loadComponent`），保持初始 bundle 精簡。
- **無 NgRx**：不引入 NgRx，維持 Angular Signals 的輕量狀態方案。

---

## 3. 程式碼品質

### 3.1 測試規範

**後端：**

- 新增功能時，必須同步新增對應的 Unit Test 或 Integration Test。
- Unit Test 位置：`src/test/java/...`，測試類別名稱以 `Test` 結尾。
- 現有測試分佈：

  | 測試類別 | 位置 | 類型 |
  |---|---|---|
  | Model 測試（Tile/Player/Board/GameRoom） | `test/.../model/` | Unit |
  | GameEngineService 測試 | `test/.../service/` | Unit |
  | GameRoomRepository 測試 | `test/.../repository/` | Unit |
  | MatchmakingService 測試 | `test/.../service/` | Unit |
  | RoomController 測試 | `test/.../controller/` | MockMvc Integration |
  | GameWebSocketController 測試 | `test/.../controller/` | `@SpringBootTest` Integration |

- 執行：`cd backend && mvn test`。修改任何後端程式碼後必須確認測試全過。
- **不得為了讓測試通過而刪除有意義的測試案例**。若現有測試因設計變更而失敗，應修正程式碼或更新測試，並說明原因。

**前端：**

- 單元測試：Karma + Jasmine，執行 `npm test -- --watch=false --browsers=ChromeHeadless`。
- E2E 測試：Playwright，測試骨架位於 `e2e/lobby.spec.ts` 和 `e2e/game.spec.ts`，需要後端在 `localhost:8080` 運行。
- 執行 `ng build` 後必須確認零 Error、零 Warning。

### 3.2 設計原則

- **簡單優先**：優先採用可讀性高、維護成本低的設計，避免過度抽象。
- **不要預先最佳化**：效能調整應以實際數據為依據（例如 P95 廣播延遲），不要在沒有瓶頸的地方做優化。
- **修改範圍最小化**：每次修改只動必要的程式碼，不順手重構無關模組。
- **Record vs Class**：後端 DTO 使用 Java record（不可變），Model 使用 class（可變狀態）。
- **前端 inline styles**：Component 的 `styles` 陣列總大小不超過 8KB（Angular budget 上限）。

---

## 4. 安全性規範

### 4.1 手牌隱私保護（最重要）

- `GameRoomDTO`（廣播給所有訂閱者的 DTO）的 `players` 欄位**只能包含 `handCount`（手牌數量）**，絕對不能包含手牌內容（`List<Tile> hand`）。
- 個人手牌必須透過專屬 REST 端點 `GET /api/rooms/{roomId}/hand?playerId={id}` 取得，且只回傳查詢者自己的手牌。
- `PlayerDTO` 僅包含：`id`、`name`、`handCount`、`connected`、`ai`，不得新增手牌相關欄位。

### 4.2 遊戲操作驗證

**後端必須驗證（不可省略）：**

1. **身分驗證**：`playerId` 必須存在於該房間的 `players` 列表中（`room.findPlayer(playerId)` 會拋出 `IllegalArgumentException`）。
2. **回合驗證**：只有 `currentTurnPlayerId` 對應的玩家才能出牌或過牌（`GameEngineService` 的 `playTile`/`passTurn` 方法內部驗證）。
3. **狀態驗證**：`GameWebSocketController` 在執行操作前必須確認 `room.getStatus() == IN_PROGRESS`。
4. **出牌合法性**：牌必須匹配桌面左端或右端點數（`GameEngineService.playTile()` 內部驗證）。
5. **過牌合法性**：只有當手牌中無任何合法出牌時才能過牌（`GameEngineService.passTurn()` 內部驗證）。

**前端驗證僅為 UX 輔助：**
- 前端的出牌合法性判斷（`canPlay()`）和回合判斷（`isMyTurn`）只用來改善使用者體驗（禁用按鈕）。
- **所有安全驗證必須在後端完成**，前端的驗證不可作為唯一防線。

### 4.3 錯誤回應

- WebSocket 操作失敗時，廣播 `{ "error": "OPERATION_FAILED", "message": "..." }` 至 `/topic/room/{roomId}`。
- REST 操作失敗透過 `GlobalExceptionHandler` 統一處理，回傳對應 HTTP 狀態碼（404/409/400）。

---

## 5. AI 開發行為規範

### 5.1 修改前的準備工作

- **先理解再動手**：修改任何模組前，先閱讀相關的 interface、impl、DTO、測試，確認現有行為。
- **確認影響範圍**：修改 model（如 `Player`、`GameRoom`）時，必須確認所有依賴該 model 的 Service、Controller、DTO、測試是否需要同步更新。
- **不要任意重構**：只修改任務要求的模組，不順手調整風格、命名、或重組無關程式碼。

### 5.2 需求不明確時的處理方式

- **先詢問，不猜測**：若需求描述模糊（例如：「改善 UI」或「優化效能」），先確認具體目標、範圍、成功條件，再開始實作。
- **不自行擴充功能**：除非明確要求，不在實作任務時額外加入未被要求的功能。
- **不自行刪除功能**：若某功能看起來「多餘」，先詢問是否要移除，不主動刪除。

### 5.3 發現文件與程式碼不一致時

若發現 `spec.md`、`docs/design.md`、`docs/tasks.md`、`docs/requirements.md` 與實際程式碼有出入：

1. **列出差異清單**，格式為：
   ```
   [差異] 文件說 X，但程式碼實際做 Y，位於 Z 檔案第 N 行。
   ```
2. **不要直接修改**，等待使用者確認要以文件為準還是程式碼為準。
3. 常見已知差異（不需每次提報）：
   - `README.md` 的測試數量顯示「102 個」，實際因持續新增而更多。
   - `README.md` 記錄的 `Milestone 3` 狀態為「In Progress」，實際已完成。

### 5.4 實作完成後的回報格式

每次完成實作任務後，必須說明：

1. **修改了哪些檔案**（路徑列表）
2. **設計決策說明**：為什麼這樣做，有沒有其他選項
3. **測試結果**：後端 `mvn test` 的通過數量，前端 `ng build` 是否零 Warning
4. **尚未解決的問題**（如果有）：已知限制、技術債、下一步建議

---

## 6. 遊戲領域知識速查

### 6.1 Cuba Domino 規則摘要

| 規則 | 細節 |
|---|---|
| 牌組 | 雙九制（Double-Nine），共 55 張，id 0-54 |
| 發牌 | 4 人各 10 張，剩 15 張入 Boneyard |
| 首出 | 持有 [9\|9] 的玩家先出，第一張必須是 [9\|9] |
| 出牌 | 必須匹配桌面 `leftEnd` 或 `rightEnd` |
| 過牌 | 手牌中無任何合法出牌時才可過牌 |
| 勝利 | 手牌最先清空者獲勝 |
| 死局 | 所有玩家均無法出牌（Trancado），剩餘點數最少者獲勝 |

### 6.2 關鍵資料結構

```
GameRoom                    // 聚合根，持有完整遊戲狀態
  ├── roomId: String         // UUID
  ├── players: List<Player>  // 有序（出牌順序）
  ├── board: Board           // 桌面（Deque<Tile>、leftEnd、rightEnd）
  ├── boneyard: List<Tile>   // 備用庫
  ├── status: GameStatus     // WAITING / IN_PROGRESS / FINISHED
  ├── currentTurnIndex: int  // 指向 players 的索引
  ├── winnerId: String       // null 表示未結束
  └── trancado: boolean      // 是否死局

Player
  ├── id: String
  ├── name: String
  ├── hand: List<Tile>       // 伺服器持有，不對外廣播
  ├── connected: boolean
  └── ai: boolean            // true = AI 玩家

Tile（record，不可變）
  ├── id: int
  ├── left: int
  ├── right: int
  └── isDouble(): left == right
```

### 6.3 AI 玩家策略

`AiPlayerServiceImpl` 採用 Greedy + Defensive 策略：
1. 空桌時出 [9|9]
2. 優先出對子（double）
3. 其次選點數最高的牌
4. 出牌端優先選 RIGHT
5. 無合法牌則過牌

### 6.4 WebSocket 廣播時機

後端在以下情況廣播 `GameRoomDTO` 至 `/topic/room/{roomId}`：
- 人類玩家出牌（`/app/game.play`）
- 人類玩家過牌（`/app/game.pass`）
- AI 連鎖行動的每一步（`triggerAiChain` 內部，每步間隔 600ms）

**進入遊戲頁面時不會收到廣播**，前端必須主動呼叫 REST 取得初始狀態（`GameComponent.ngOnInit`）。

---

## 7. 開發環境快速參考

```bash
# 啟動後端
cd backend && mvn spring-boot:run
# → http://localhost:8080

# 啟動前端
cd frontend && npm start
# → http://localhost:4200（透過 proxy.conf.json 代理到後端）

# 後端測試
cd backend && mvn test

# 前端建置（驗證）
cd frontend && npx ng build

# 前端 E2E（需後端運行）
cd frontend && npm run e2e
```
