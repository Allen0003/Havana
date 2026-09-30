import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { boardTiles, leftEnd, rightEnd } from '../../../core/store/game.store';

/**
 * Board Component
 *
 * 渲染桌面骨牌序列，並顯示左右兩端的點數。
 * 使用 Angular Computed Signals，廣播到來時自動更新。
 */
@Component({
  selector: 'app-board',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="board-container">
      <h2 class="text-lg font-bold text-white mb-3 flex items-center gap-2">
        🎲 Board
        @if (boardTiles().length > 0) {
          <span class="text-sm font-normal text-slate-300">
            ({{ boardTiles().length }} tiles)
          </span>
        }
      </h2>

      <!-- 左右端點數 -->
      @if (boardTiles().length > 0) {
        <div class="flex items-center gap-4 mb-3">
          <div class="end-badge">
            <span class="text-xs text-slate-400">LEFT</span>
            <span class="text-xl font-bold text-yellow-400">{{ leftEnd() }}</span>
          </div>
          <div class="flex-1 border-t border-dashed border-slate-600"></div>
          <div class="end-badge">
            <span class="text-xs text-slate-400">RIGHT</span>
            <span class="text-xl font-bold text-yellow-400">{{ rightEnd() }}</span>
          </div>
        </div>
      }

      <!-- 骨牌序列 -->
      <div class="tiles-scroll">
        @if (boardTiles().length === 0) {
          <div class="empty-board">
            <span class="text-slate-500 text-sm">Waiting for the first tile...</span>
          </div>
        } @else {
          <div class="tiles-row">
            @for (tile of boardTiles(); track tile.id) {
              <div class="board-tile" [class.double-tile]="tile.isDouble">
                <span class="pip">{{ tile.left }}</span>
                <span class="divider" [class.divider-h]="tile.isDouble">
                  {{ tile.isDouble ? '—' : '|' }}
                </span>
                <span class="pip">{{ tile.right }}</span>
              </div>
            }
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .board-container {
      @apply bg-slate-800 rounded-xl p-4 min-h-[120px];
    }

    .end-badge {
      @apply flex flex-col items-center bg-slate-700 rounded-lg px-3 py-1;
    }

    .tiles-scroll {
      @apply overflow-x-auto pb-2;
    }

    .tiles-row {
      @apply flex flex-row gap-1 flex-nowrap;
    }

    .empty-board {
      @apply flex items-center justify-center py-4;
    }

    .board-tile {
      @apply flex flex-row items-center bg-slate-100 text-slate-900
             rounded px-1.5 py-1 text-xs font-bold flex-shrink-0
             shadow min-w-[36px] justify-center gap-0.5;
    }

    .double-tile {
      @apply flex-col bg-yellow-100;
    }

    .pip {
      @apply text-sm font-bold leading-none;
    }

    .divider {
      @apply text-slate-400 text-xs leading-none;
    }

    .divider-h {
      @apply rotate-0;
    }
  `],
})
export class BoardComponent {
  readonly boardTiles = boardTiles;
  readonly leftEnd = leftEnd;
  readonly rightEnd = rightEnd;
}
