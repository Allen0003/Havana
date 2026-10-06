import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  isMyTurn,
  myHandSignal,
  gameRoomSignal,
  localPlayerId,
  leftEnd,
  rightEnd,
} from '../../../core/store/game.store';
import { WebSocketService } from '../../../core/services/web-socket.service';
import { BoardEnd } from '../../../core/models/websocket-messages.model';
import { Tile } from '../../../core/models/tile.model';
import { GameStatus } from '../../../core/models/game-room.model';

/**
 * Hand Component
 *
 * 渲染本機玩家的手牌。
 * - 非本人回合時，所有牌禁用點擊
 * - 點擊一張牌後，如桌面兩端點數都存在，彈出選擇 LEFT / RIGHT 的按鈕
 * - 如桌面為空（第一張牌），直接出牌（targetEnd 預設 LEFT）
 * - 出牌動作透過 WebSocketService.playTile() 送出
 */
@Component({
  selector: 'app-hand',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './hand.html',
  styleUrls: ['./hand.css']
})
export class HandComponent {
  private ws = inject(WebSocketService);

  readonly isMyTurn = isMyTurn;
  readonly myHandSignal = myHandSignal;
  readonly leftEnd = leftEnd;
  readonly rightEnd = rightEnd;
  readonly BoardEnd = BoardEnd;

  /** 目前選中要出的牌 */
  selectedTile = signal<Tile | null>(null);

  /** 是否遊戲已結束 */
  isFinished(): boolean {
    return gameRoomSignal()?.status === GameStatus.FINISHED;
  }

  /** 判斷這張牌是否能出（匹配左端或右端，或桌面為空） */
  canPlay(tile: Tile): boolean {
    const left = leftEnd();
    const right = rightEnd();
    if (left === null || right === null) {
      // 桌面為空，任何牌都可出
      return true;
    }
    return tile.left === left || tile.right === left ||
           tile.left === right || tile.right === right;
  }

  /** 是否需要選擇出牌端（左或右） */
  needsEndSelection(): boolean {
    const tile = this.selectedTile();
    if (!tile) return false;
    const left = leftEnd();
    const right = rightEnd();
    // 桌面為空時不需選端
    if (left === null || right === null) return false;
    // 左右端都能匹配時需要選端
    const matchLeft = tile.left === left || tile.right === left;
    const matchRight = tile.left === right || tile.right === right;
    return matchLeft && matchRight;
  }

  /** 點擊手牌 */
  onTileClick(tile: Tile): void {
    if (!this.isMyTurn() || !this.canPlay(tile) || this.isFinished()) return;

    const left = leftEnd();
    const right = rightEnd();

    // 桌面為空（第一張牌）：直接出牌
    if (left === null || right === null) {
      this.dispatchPlay(tile, BoardEnd.LEFT);
      return;
    }

    const matchLeft = tile.left === left || tile.right === left;
    const matchRight = tile.left === right || tile.right === right;

    // 只能匹配一端：直接出牌
    if (matchLeft && !matchRight) {
      this.dispatchPlay(tile, BoardEnd.LEFT);
      return;
    }
    if (!matchLeft && matchRight) {
      this.dispatchPlay(tile, BoardEnd.RIGHT);
      return;
    }

    // 兩端都能匹配：讓玩家選擇
    this.selectedTile.set(tile);
  }

  /** 選擇端點後出牌 */
  playSelected(end: BoardEnd): void {
    const tile = this.selectedTile();
    if (!tile) return;
    this.dispatchPlay(tile, end);
    this.selectedTile.set(null);
  }

  /** 取消選牌 */
  cancelSelection(): void {
    this.selectedTile.set(null);
  }

  /** 過牌 */
  onPass(): void {
    const rid = gameRoomSignal()?.roomId;
    const pid = localPlayerId();
    if (!rid || !pid) return;
    this.ws.passTurn({ roomId: rid, playerId: pid });
  }

  /** 送出出牌請求 */
  private dispatchPlay(tile: Tile, targetEnd: BoardEnd): void {
    const rid = gameRoomSignal()?.roomId;
    const pid = localPlayerId();
    if (!rid || !pid) return;
    this.ws.playTile({ roomId: rid, playerId: pid, tileId: tile.id, targetEnd });
  }
}
