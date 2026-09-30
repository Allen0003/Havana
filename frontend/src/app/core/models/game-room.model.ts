import { Player } from './player.model';
import { Tile } from './tile.model';

/**
 * 遊戲狀態枚舉
 */
export enum GameStatus {
  WAITING = 'WAITING',
  IN_PROGRESS = 'IN_PROGRESS',
  FINISHED = 'FINISHED',
}

/**
 * 遊戲房間 DTO（對應後端 GameRoomDTO）
 */
export interface GameRoom {
  roomId: string;
  status: GameStatus;
  players: Player[];
  boardTiles: Tile[];
  leftEnd: number | null;
  rightEnd: number | null;
  boneyardCount: number;
  currentTurnPlayerId: string | null;
  winnerId: string | null;
  trancado: boolean;
  trancadoScores: { [playerId: string]: number } | null;
}
