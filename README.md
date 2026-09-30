# Havana 🎲

> Online Real-Time Domino Cubano Platform

4-player double-nine dominoes game with real-time WebSocket synchronization, automatic deadlock detection (Trancado), and scoring calculation.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.2.5, Spring WebSocket (STOMP), Maven |
| Frontend | Angular 20, Angular Signals, Tailwind CSS 3 |
| Communication | SockJS + STOMP over WebSocket |
| Testing | JUnit 5, AssertJ, Mockito, Spring MockMvc |

---

## Project Structure

```
Havana/
├── backend/          # Spring Boot 3 REST + WebSocket backend
├── frontend/         # Angular 20 Standalone frontend
├── docs/
│   ├── requirements.md   # Functional and non-functional requirements
│   ├── design.md         # System design, API contracts, sequence diagrams
│   └── tasks.md          # Development task breakdown (Milestone 0–4)
├── spec.md           # Project specification
└── .github/
    └── workflows/
        └── ci.yml    # GitHub Actions CI
```

---

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.6+
- Node.js 22+
- npm 10+

### Backend

```bash
cd backend
mvn spring-boot:run
```

Server starts at `http://localhost:8080`

### Frontend

```bash
cd frontend
npm install
npm start
```

Development server starts at `http://localhost:4200`

> In development mode, API requests point to `http://localhost:8080` (see `src/environments/environment.ts`)

---

## REST API

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/rooms` | Create new room |
| `POST` | `/api/rooms/{roomId}/join` | Join room |
| `GET`  | `/api/rooms/{roomId}` | Query room status |

### Create Room

```bash
curl -X POST http://localhost:8080/api/rooms \
  -H "Content-Type: application/json" \
  -d '{"hostName": "Alice"}'
```

```json
{
  "roomId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "WAITING",
  "hostPlayerId": "a1b2c3d4-..."
}
```

### Join Room

```bash
curl -X POST http://localhost:8080/api/rooms/{roomId}/join \
  -H "Content-Type: application/json" \
  -d '{"playerName": "Bob"}'
```

When the 4th player joins, room status automatically changes to `IN_PROGRESS` and the game begins.

---

## WebSocket Protocol (STOMP)

**Connection Endpoint**: `/ws-havana` (SockJS)

### Client → Server

| Destination | Description | Payload |
|---|---|---|
| `/app/game.play` | Play tile | `{ roomId, playerId, tileId, targetEnd: "LEFT"\|"RIGHT" }` |
| `/app/game.pass` | Pass turn | `{ roomId, playerId }` |

### Server → Client (Broadcast)

Subscribe to `/topic/room/{roomId}` to receive board updates:

```json
{
  "roomId": "string",
  "status": "IN_PROGRESS",
  "players": [
    { "id": "string", "name": "Alice", "handCount": 9, "connected": true }
  ],
  "boardTiles": [{ "id": 54, "left": 9, "right": 9, "isDouble": true }],
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

## Game Rules

| Rule | Description |
|---|---|
| Tile Set | Double-nine, 55 tiles ([0\|0] to [9\|9]) |
| Deal | 4 players get 10 tiles each, remaining 15 go to Boneyard |
| First Move | Player holding [9\|9] goes first, must play [9\|9] |
| Playing | Match the left or right end value on the board |
| Passing | Pass/knock when no tiles can be played |
| Victory | First player to empty their hand wins |
| Deadlock | All players unable to play → Trancado, player with lowest remaining points wins |

---

## Running Tests

### Backend

```bash
cd backend
mvn test
```

Currently **102 tests, 0 failures**:

| Test Class | Count |
|---|---|
| Model (Tile / Player / Board / GameRoom) | 24 |
| GameEngineService (deck, deal, play, pass, victory, trancado) | 39 |
| GameRoomRepository (CRUD + concurrency) | 14 |
| MatchmakingService (create/join room) | 14 |
| RoomController (MockMvc integration) | 10 |
| Spring Context Load | 1 |

### Frontend

```bash
cd frontend
npm test -- --watch=false --browsers=ChromeHeadless
```

---

## Development Progress

| Milestone | Description | Status |
|---|---|---|
| M0 | Project skeleton, WebSocket setup, Angular initialization, CI | ✅ Complete |
| M1 | Core game engine (GameEngineService + full unit tests) | ✅ Complete |
| M2 | REST API, room management (MatchmakingService + RoomController) | ✅ Complete |
| M3 | WebSocket real-time gameplay (GameWebSocketController + Angular frontend) | 🔲 In Progress |
| M4 | Polish, E2E tests, performance validation | 🔲 Not Started |

See [docs/tasks.md](docs/tasks.md) for detailed task breakdown.

---

## Environment Variables

### Frontend (`frontend/src/environments/`)

| Environment | File | `apiBaseUrl` | `wsEndpoint` |
|---|---|---|---|
| Development | `environment.ts` | `http://localhost:8080/api` | `http://localhost:8080/ws-havana` |
| Production | `environment.prod.ts` | `/api` | `/ws-havana` |

---

## CI/CD

Automatically triggered on push to `main` or `develop` branches:

- **backend job**: `mvn -B verify` (Java 17 Temurin)
- **frontend job**: `npm ci` → `ng build --configuration=production`

See [.github/workflows/ci.yml](.github/workflows/ci.yml) for details.
