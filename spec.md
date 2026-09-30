
# Havana - 古巴骨牌遊戲系統規格書 (Spec & API Contract)

## 1. 專案基本資訊
- **專案名稱**：Havana (古巴骨牌 / Domino Cubano 線上對戰平台)
- **架構風格**：Spec-Driven Development (規格驅動開發)
- **前端技術**：Angular (v17+), Angular Signals, Tailwind CSS / Angular Material
- **後端技術**：Java 17, Spring Boot 3.x, Spring WebSocket (STOMP), Maven
- **核心特色**：採用古巴骨牌規則（55張雙九牌 Double-Nine，4人對戰或2v2組隊，支援即時WebSocket同步與死局自動計分）。

---

## 2. 系統架構與模組設計

### 2.1 後端架構 (Java / Spring Boot)
- **根套件路徑**：`com.havana.domino`
- **核心套件結構**：
  - `controller/`：REST API 與 WebSocket Controller (STOMP endpoints)
  - `service/`：遊戲核心邏輯 (`GameEngineService`, `MatchmakingService`)
  - `model/`：網域模型 (`GameRoom`, `Player`, `Tile`, `Board`)
  - `repository/`：記憶體或 Redis 狀態暫存
- **核心遊戲規則引擎 (`GameEngineService`)**：
  - **牌組**：共 55 張骨牌（雙九制，點數從 0 到 9）。
  - **發牌**：4 名玩家，每人發 10 張牌，剩餘 15 張為備用庫（Boneyard）。
  - **出牌與驗證**：首局由擁有雙九 (`9-9`) 的玩家先出，後續依序匹配桌面兩端點數。
  - **死局判斷 (Trancado)**：當所有玩家手中都有牌，但無人能出牌時，系統自動判定死局，計算各玩家剩餘手牌點數總和，點數最小者獲勝。

### 2.2 前端架構 (Angular)
- **核心模組與狀態**：
  - `signals/`：使用 Angular Signals 管理遊戲房間狀態、玩家手牌與桌面陣列。
  - `services/`：`WebSocketService`（處理 STOMP 連線與訊息廣播）、`GameApiService`。
  - `components/`：
    - `board/`：呈現蛇形延伸的骨牌桌面。
    - `hand/`：玩家手牌區（支援點擊出牌）。
    - `score/`：即時積分與戰況看板。

---

## 3. API Contract 與 WebSocket 協議規格

### 3.1 REST API (HTTP)
| 方式 | 路徑 | 描述 | 請求 Body / 參數 | 回應格式 |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/rooms` | 建立新遊戲房間 | `{ "hostName": "string" }` | `{ "roomId": "string", "status": "WAITING" }` |
| `POST` | `/api/rooms/{roomId}/join` | 加入房間 | `{ "playerName": "string" }` | `{ "success": boolean, "players": [...] }` |
| `GET` | `/api/rooms/{roomId}` | 取得房間當前狀態 | - | `GameRoomDTO` |

### 3.2 WebSocket 協議 (STOMP Over SockJS)
- **連線端點**：`/ws-havana`
- **客戶端發送訊息 (Destination)**：
  - `/app/game.play`：玩家出牌
    - **Payload**: `{ "roomId": "string", "playerId": "string", "tileId": number, "targetEnd": "LEFT"|"RIGHT" }`
  - `/app/game.pass`：玩家過牌 (Pass/Knock)
    - **Payload**: `{ "roomId": "string", "playerId": "string" }`
- **伺服器廣播頻道 (Topic)**：
  - `/topic/room/{roomId}`：廣播房間內所有玩家的最新狀態、桌面兩端點數與當前輪到的玩家。

---

## 4. 資料模型定義 (JSON Schema / Java Records)

### Tile (骨牌)
```json
{
  "id": 1,
  "left": 5,
  "right": 9,
  "isDouble": false
}


