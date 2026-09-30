# Havana — 系統設計文件 (Design)

> 版本：1.0 | 基準：spec.md、requirements.md

---

## 1. 整體架構概觀

```
┌─────────────────────────────────────────────────────────────┐
│                        Browser (Angular)                    │
│  ┌──────────┐  ┌──────────────┐  ┌───────────────────────┐ │
│  │ board/   │  │    hand/     │  │       score/          │ │
│  │component │  │  component   │  │      component        │ │
│  └────┬─────┘  └──────┬───────┘  └──────────┬────────────┘ │
│       └────────────────┴──────────────────────┘             │
│                        Signal Store                         │
│              (gameRoom, myHand, boardTiles)                 │
│       ┌──────────────────┬──────────────────┐              │
│       │  WebSocketService│   GameApiService  │              │
│       │  (STOMP/SockJS)  │   (HTTP REST)     │              │
└───────┼──────────────────┼───────────────────┼─────────────┘
        │ WS /ws-havana     │ HTTP /api/**       │
        ▼                   ▼                   │
┌────────────────────────────────────────────────────────────┐
│                  Spring Boot 3 Server                      │
│  ┌─────────────────┐   ┌──────────────────────────────┐   │
│  │  WebSocket       │   │      REST Controllers        │   │
│  │  Controller      │   │  RoomController              │   │
│  │  (GameWsCtrl)    │   └──────────────┬───────────────┘   │
│  └──────┬───────────┘                  │                   │
│         └──────────────────┬───────────┘                   │
│                    ┌───────▼────────┐                      │
│                    │ GameEngineService                      │
│                    │ MatchmakingService                     │
│                    └───────┬────────┘                      │
│                    ┌───────▼────────┐                      │
│                    │  In-Memory     │                      │
│                    │  Repository    │                      │
│                    │  (ConcHashMap) │                      │
│                    └────────────────┘                      │
└────────────────────────────────────────────────────────────┘
```

---

## 2. 後端設計 (Spring Boot 3)

### 2.1 套件結構

```
com.havana.domino
├── config/
│   └── WebSocketConfig.java          # STOMP endpoint 與 message broker 設定
├── controller/
│   ├── RoomController.java           # REST: /api/rooms
│   └── GameWebSocketController.java  # STOMP: /app/game.*
├── service/
│   ├── GameEngineService.java        # 核心規則引擎
│   └── MatchmakingService.java       # 房間管理與配對
├── model/
│   ├── Tile.java                     # 骨牌 entity
│   ├── Player.java                   # 玩家 entity
│   ├── GameRoom.java                 # 房間聚合根
│   └── Board.java                    # 桌面狀態
├── dto/
│   ├── GameRoomDTO.java              # 廣播用 DTO（不含他人手牌）
│   ├── PlayMoveRequest.java          # 出牌請求
│   ├── PassMoveRequest.java          # 過牌請求
│   ├── CreateRoomRequest.java
│   └── JoinRoomRequest.java
├── repository/
│   └── GameRoomRepository.java       # ConcurrentHashMap 記憶體儲存
└── HavanaDominoApplication.java
```

### 2.2 核心領域模型

#### Tile（骨牌）
```java
public record Tile(int id, int left, int right) {
    public boolean isDouble() { return left == right; }
    public boolean canMatch(int value) { return left == value || right == value; }
    public int matchingEnd(int value) { return left == value ? right : left; }
    public int totalPips() { return left + right; }
}
```

#### Player
```java
public class Player {
    String id;          // UUID
    String name;
    List<Tile> hand;    // 手牌（僅伺服器端完整持有）
    boolean connected;
}
```

#### Board（桌面）
```java
public class Board {
    Deque<Tile> tiles;  // 雙端佇列，代表桌面骨牌排列
    Integer leftEnd;    // 桌面左端點數（null 表示空桌）
    Integer rightEnd;   // 桌面右端點數
}
```

#### GameRoom（聚合根）
```java
public class GameRoom {
    String roomId;
    List<Player> players;      // 有序（回合順序）
    Board board;
    List<Tile> boneyard;       // 備用庫
    GameStatus status;         // WAITING / IN_PROGRESS / FINISHED
    int currentTurnIndex;      // 當前輪次玩家 index
    String winnerId;           // 勝者 playerId（可 null）
    boolean trancado;          // 是否死局
}
```

### 2.3 GameEngineService 方法設計

```java
public interface GameEngineService {
    /** 洗牌並發牌，回傳初始化後的 GameRoom */
    GameRoom initGame(GameRoom room);

    /** 驗證並執行出牌；回傳更新後的 GameRoom */
    GameRoom playTile(GameRoom room, String playerId, int tileId, BoardEnd targetEnd);

    /** 驗證並執行過牌 */
    GameRoom passTurn(GameRoom room, String playerId);

    /** 檢查是否有玩家獲勝（手牌清空） */
    Optional<Player> checkWinner(GameRoom room);

    /** 檢查是否死局（所有玩家皆無合法出牌） */
    boolean isTrancado(GameRoom room);

    /** 計算死局分數，回傳各玩家剩餘點數 Map */
    Map<String, Integer> calculateTrancadoScores(GameRoom room);

    /** 產生 55 張雙九牌組 */
    List<Tile> generateTileSet();
}
```

### 2.4 WebSocket 設定 (WebSocketConfig)

```
- SockJS endpoint:  /ws-havana
- Application destination prefix: /app
- Simple message broker prefix:   /topic
```

### 2.5 錯誤處理策略

| 情境 | HTTP / WS 回應 |
| :--- | :--- |
| 非法出牌（牌不匹配） | WS error frame，`{ "error": "INVALID_MOVE", "message": "..." }` |
| 非輪到該玩家 | WS error frame，`{ "error": "NOT_YOUR_TURN" }` |
| 房間已滿 | HTTP 409 Conflict |
| 房間不存在 | HTTP 404 Not Found |
| 玩家嘗試過牌但有牌可出 | WS error frame，`{ "error": "CAN_PLAY" }` |

---

## 3. 前端設計 (Angular 17+)

### 3.1 目錄結構

```
src/
├── app/
│   ├── core/
│   │   ├── models/
│   │   │   ├── tile.model.ts
│   │   │   ├── player.model.ts
│   │   │   ├── game-room.model.ts
│   │   │   └── websocket-messages.model.ts
│   │   ├── services/
│   │   │   ├── web-socket.service.ts   # STOMP 連線管理
│   │   │   └── game-api.service.ts     # REST HTTP Client
│   │   └── store/
│   │       └── game.store.ts           # Angular Signals 狀態
│   ├── features/
│   │   ├── lobby/                      # 建立/加入房間頁面
│   │   ├── game/
│   │   │   ├── board/                  # 桌面骨牌渲染
│   │   │   ├── hand/                   # 玩家手牌
│   │   │   └── score/                  # 計分看板
│   │   └── result/                     # 遊戲結果頁面
│   └── app.routes.ts
├── environments/
│   ├── environment.ts
│   └── environment.prod.ts
└── styles.css                          # Tailwind / global styles
```

### 3.2 Angular Signals 狀態設計

```typescript
// game.store.ts
export const gameRoomSignal   = signal<GameRoomDTO | null>(null);
export const myHandSignal     = signal<Tile[]>([]);
export const connectionStatus = signal<'DISCONNECTED' | 'CONNECTING' | 'CONNECTED'>('DISCONNECTED');
export const errorMessage     = signal<string | null>(null);

// derived signals
export const boardTiles      = computed(() => gameRoomSignal()?.boardTiles ?? []);
export const currentPlayerId = computed(() => gameRoomSignal()?.currentTurnPlayerId ?? null);
export const isMyTurn        = computed(() => currentPlayerId() === localPlayerId());
```

### 3.3 WebSocketService 職責

- 建立 / 關閉 STOMP Client（over SockJS）
- 訂閱 `/topic/room/{roomId}` 並將 payload 寫入 `gameRoomSignal`
- 提供 `playTile(req: PlayMoveRequest)` 與 `passTurn(req: PassMoveRequest)` 方法
- 處理連線失敗自動重試（指數退避，最多 5 次）

---

## 4. API Contract（完整）

### 4.1 REST Endpoints

#### POST /api/rooms
- **Request**: `{ "hostName": "string" }`
- **Response 201**: `{ "roomId": "uuid", "status": "WAITING" }`

#### POST /api/rooms/{roomId}/join
- **Request**: `{ "playerName": "string" }`
- **Response 200**: `{ "success": true, "playerId": "uuid", "players": [PlayerDTO] }`
- **Response 409**: `{ "error": "ROOM_FULL" }`
- **Response 404**: `{ "error": "ROOM_NOT_FOUND" }`

#### GET /api/rooms/{roomId}
- **Response 200**: `GameRoomDTO`

### 4.2 WebSocket Messages

#### Client → Server

| Destination | Payload |
| :--- | :--- |
| `/app/game.play` | `{ roomId, playerId, tileId: number, targetEnd: "LEFT"\|"RIGHT" }` |
| `/app/game.pass` | `{ roomId, playerId }` |

#### Server → Client (Broadcast)

**Topic**: `/topic/room/{roomId}`

```json
{
  "roomId": "string",
  "status": "WAITING | IN_PROGRESS | FINISHED",
  "players": [
    {
      "id": "string",
      "name": "string",
      "handCount": 10,
      "connected": true
    }
  ],
  "boardTiles": [
    { "id": 1, "left": 9, "right": 9, "isDouble": true }
  ],
  "leftEnd": 9,
  "rightEnd": 9,
  "boneyardCount": 15,
  "currentTurnPlayerId": "string",
  "winnerId": null,
  "trancado": false,
  "trancadoScores": null
}
```

---

## 5. 序列圖：完整出牌流程

```
Player A          Angular              Spring Boot          Other Players
   │                │                      │                     │
   │ click tile     │                      │                     │
   │───────────────►│                      │                     │
   │                │ STOMP /app/game.play │                     │
   │                │─────────────────────►│                     │
   │                │                      │ validate move       │
   │                │                      │ update GameRoom     │
   │                │                      │ check winner/trancado
   │                │    broadcast         │                     │
   │                │◄─────────────────────│────────────────────►│
   │                │ update signals       │                     │
   │◄───────────────│                      │                     │
```

---

## 6. 狀態機

```
WAITING ──(4 players joined)──► IN_PROGRESS ──(winner/trancado)──► FINISHED
```

房間在 `FINISHED` 狀態後不可再接受出牌，可選擇重新建立房間。
