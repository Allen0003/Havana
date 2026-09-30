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
  template: `
    <div class="hand-container">
      <h2 class="text-lg font-bold text-white mb-3 flex items-center gap-2">
        🖐 Your Hand
        <span class="text-sm font-normal text-slate-300">
          ({{ myHandSignal().length }} tiles)
        </span>
        @if (isMyTurn()) {
          <span class="turn-badge">Your Turn!</span>
        }
      </h2>

      <!-- 手牌列表 -->
      <div class="tiles-row">
        @for (tile of myHandSignal(); track tile.id) {
          <div
            class="hand-tile"
            [class.tile-active]="isMyTurn() && canPlay(tile)"
            [class.tile-disabled]="!isMyTurn() || isFinished()"
            [class.tile-selected]="selectedTile()?.id === tile.id"
            (click)="onTileClick(tile)"
          >
            <span class="pip">{{ tile.left }}</span>
            <span class="divider" [class.double]="tile.isDouble">
              {{ tile.isDouble ? '—' : '|' }}
            </span>
            <span class="pip">{{ tile.right }}</span>

            <!-- 可出牌的高亮指示 -->
            @if (isMyTurn() && canPlay(tile)) {
              <span class="playable-dot"></span>
            }
          </div>
        }

        @if (myHandSignal().length === 0) {
          <span class="text-slate-500 text-sm italic">No tiles in hand</span>
        }
      </div>

      <!-- 選擇出牌方向（出現在選中一張牌之後） -->
      @if (selectedTile() !== null && needsEndSelection()) {
        <div class="end-selection">
          <span class="text-white text-sm font-medium">Play to which end?</span>
          <div class="flex gap-2">
            <button class="btn-end" (click)="playSelected(BoardEnd.LEFT)">
              ← Left ({{ leftEnd() }})
            </button>
            <button class="btn-end" (click)="playSelected(BoardEnd.RIGHT)">
              Right ({{ rightEnd() }}) →
            </button>
            <button class="btn-cancel" (click)="cancelSelection()">Cancel</button>
          </div>
        </div>
      }

      <!-- 過牌按鈕 -->
      @if (isMyTurn() && !isFinished()) {
        <div class="mt-3">
          <button class="btn-pass" (click)="onPass()">
            Pass / Knock
          </button>
        </div>
      }
    </div>
  `,
  styles: [`
    .hand-container {
      @apply bg-slate-800 rounded-xl p-4;
    }

    .turn-badge {
      @apply bg-green-500 text-white text-xs font-bold px-2 py-0.5 rounded-full animate-pulse;
    }

    .tiles-row {
      @apply flex flex-wrap gap-2;
    }

    .hand-tile {
      @apply relative flex flex-col items-center bg-slate-100 text-slate-900
             rounded-lg px-2 py-1.5 min-w-[44px] cursor-not-allowed
             select-none transition-all duration-150 shadow;
    }

    .tile-active {
      @apply cursor-pointer hover:bg-yellow-100 hover:shadow-md hover:-translate-y-0.5;
    }

    .tile-disabled {
      @apply opacity-50;
    }

    .tile-selected {
      @apply ring-2 ring-green-400 bg-green-50;
    }

    .pip {
      @apply text-sm font-bold leading-none;
    }

    .divider {
      @apply text-slate-400 text-xs my-0.5 leading-none;
    }

    .playable-dot {
      @apply absolute -top-1 -right-1 w-2 h-2 bg-green-400 rounded-full;
    }

    .end-selection {
      @apply mt-3 bg-slate-700 rounded-lg p-3 flex flex-col gap-2;
    }

    .btn-end {
      @apply bg-blue-600 hover:bg-blue-500 text-white text-sm font-medium
             px-3 py-1.5 rounded-lg transition-colors;
    }

    .btn-cancel {
      @apply bg-slate-600 hover:bg-slate-500 text-white text-sm font-medium
             px-3 py-1.5 rounded-lg transition-colors;
    }

    .btn-pass {
      @apply bg-orange-600 hover:bg-orange-500 text-white text-sm font-semibold
             px-4 py-2 rounded-lg transition-colors;
    }
  `],
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
