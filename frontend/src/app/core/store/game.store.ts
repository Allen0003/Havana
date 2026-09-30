import { computed, signal } from '@angular/core';
import { GameRoom, GameStatus } from '../models/game-room.model';
import { Tile } from '../models/tile.model';
import { Player } from '../models/player.model';

// ─────────────────────────────────────────────
// 可寫 Signals（Writable Signals）
// ─────────────────────────────────────────────

/** 目前本機玩家的 ID（加入/建立房間後寫入） */
export const localPlayerId = signal<string | null>(null);

/** 目前本機玩家的手牌（只有自己能看到） */
export const myHandSignal = signal<Tile[]>([]);

/** 從 WebSocket 收到的最新房間狀態快照 */
export const gameRoomSignal = signal<GameRoom | null>(null);

/** WebSocket 連線狀態提示文字 */
export const connectionStatus = signal<'DISCONNECTED' | 'CONNECTING' | 'CONNECTED'>('DISCONNECTED');

/** 全域錯誤訊息 */
export const errorMessage = signal<string | null>(null);

// ─────────────────────────────────────────────
// 計算 Signals（Computed Signals）
// ─────────────────────────────────────────────

/** 桌面骨牌序列 */
export const boardTiles = computed((): Tile[] => gameRoomSignal()?.boardTiles ?? []);

/** 桌面左端點數 */
export const leftEnd = computed((): number | null => gameRoomSignal()?.leftEnd ?? null);

/** 桌面右端點數 */
export const rightEnd = computed((): number | null => gameRoomSignal()?.rightEnd ?? null);

/** 備用庫剩餘數量 */
export const boneyardCount = computed((): number => gameRoomSignal()?.boneyardCount ?? 0);

/** 目前輪到哪個玩家的 ID */
export const currentTurnPlayerId = computed((): string | null =>
  gameRoomSignal()?.currentTurnPlayerId ?? null
);

/** 是否輪到本機玩家出牌 */
export const isMyTurn = computed((): boolean =>
  localPlayerId() !== null && currentTurnPlayerId() === localPlayerId()
);

/** 所有玩家清單 */
export const players = computed((): Player[] => gameRoomSignal()?.players ?? []);

/** 遊戲是否已結束 */
export const isGameFinished = computed((): boolean =>
  gameRoomSignal()?.status === GameStatus.FINISHED
);

/** 勝者 ID */
export const winnerId = computed((): string | null => gameRoomSignal()?.winnerId ?? null);

/** 是否為死局 */
export const isTrancado = computed((): boolean => gameRoomSignal()?.trancado ?? false);

/** 死局分數 */
export const trancadoScores = computed(
  (): { [playerId: string]: number } | null => gameRoomSignal()?.trancadoScores ?? null
);

/** 本機玩家資訊 */
export const localPlayer = computed((): Player | null =>
  players().find(p => p.id === localPlayerId()) ?? null
);

/** 房間 ID */
export const roomId = computed((): string | null => gameRoomSignal()?.roomId ?? null);
