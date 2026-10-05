import { Component, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { boardTiles, leftEnd, rightEnd } from '../../../core/store/game.store';
import { Tile } from '../../../core/models/tile.model';

// ─────────────────────────────────────────────────────────────
// 型別定義
// ─────────────────────────────────────────────────────────────

/** 行進方向（東西南北） */
type Dir = 'E' | 'W' | 'S' | 'N';

/** 排列方向 */
type Orientation = 'H' | 'V'; // Horizontal | Vertical

/** 單張牌的佈局資訊 */
interface PlacedTile {
  tile: Tile;
  col: number;   // 格子 X（可負數）
  row: number;   // 格子 Y（可負數）
  orientation: Orientation;
  /** 牌面顯示是否需要翻轉（left 端朝哪個方向） */
  flipped: boolean;
}

// ─────────────────────────────────────────────────────────────
// 佈局演算法常數
// ─────────────────────────────────────────────────────────────

/** 直線段走幾格後轉彎（模擬真實骨牌桌邊界） */
const STRAIGHT_RUN = 7;

/**
 * 轉彎規則：循環轉向序列
 * 起始方向 E（向右），達到 STRAIGHT_RUN 後轉 S（向下），
 * 再轉 W（向左），再轉 N（向上），再轉 E，依此循環。
 */
const TURN_SEQUENCE: Dir[] = ['E', 'S', 'W', 'N'];

/** 各方向的座標增量 */
const DELTA: Record<Dir, { dc: number; dr: number }> = {
  E: { dc:  1, dr:  0 },
  W: { dc: -1, dr:  0 },
  S: { dc:  0, dr:  1 },
  N: { dc:  0, dr: -1 },
};

// ─────────────────────────────────────────────────────────────
// 佈局計算函式
// ─────────────────────────────────────────────────────────────

/**
 * 計算所有骨牌的平面座標。
 *
 * 規則：
 * 1. 第一張牌（[9|9]）放在 (0, 0)，水平（H）
 * 2. 之後每張牌沿當前方向延伸：
 *    - 普通牌（H/V 由當前方向決定）佔 1 格
 *    - 對子（double）垂直於當前方向放置，佔 1 格，延伸繼續
 * 3. 每走完 STRAIGHT_RUN 格就轉彎一次
 */
function layoutTiles(tiles: Tile[]): PlacedTile[] {
  if (tiles.length === 0) return [];

  const placed: PlacedTile[] = [];

  // 當前游標位置與方向
  let col = 0;
  let row = 0;
  let dirIdx = 0;   // 索引到 TURN_SEQUENCE
  let stepInSeg = 0; // 目前方向已走幾步

  for (let i = 0; i < tiles.length; i++) {
    const tile = tiles[i];
    const dir: Dir = TURN_SEQUENCE[dirIdx % TURN_SEQUENCE.length];

    // 對子垂直於行進方向放置
    const isDouble = tile.isDouble;
    let orientation: Orientation;
    if (isDouble) {
      orientation = (dir === 'E' || dir === 'W') ? 'V' : 'H';
    } else {
      orientation = (dir === 'E' || dir === 'W') ? 'H' : 'V';
    }

    // 判斷是否需要翻轉（確保 left 端始終朝「進入」方向）
    const flipped = dir === 'W' || dir === 'N';

    placed.push({ tile, col, row, orientation, flipped });

    // 移動游標到下一個位置
    const { dc, dr } = DELTA[dir];
    col += dc;
    row += dr;
    stepInSeg++;

    // 達到直線段長度時轉彎
    if (stepInSeg >= STRAIGHT_RUN) {
      dirIdx++;
      stepInSeg = 0;
    }
  }

  return placed;
}

/**
 * 將座標標準化到從 (0,0) 開始（正數範圍），計算 canvas 尺寸。
 */
function normalizePlaced(placed: PlacedTile[]): {
  normalized: PlacedTile[];
  cols: number;
  rows: number;
} {
  if (placed.length === 0) return { normalized: [], cols: 1, rows: 1 };

  const minCol = Math.min(...placed.map(p => p.col));
  const minRow = Math.min(...placed.map(p => p.row));
  const maxCol = Math.max(...placed.map(p => p.col));
  const maxRow = Math.max(...placed.map(p => p.row));

  const normalized = placed.map(p => ({
    ...p,
    col: p.col - minCol,
    row: p.row - minRow,
  }));

  return {
    normalized,
    cols: maxCol - minCol + 1,
    rows: maxRow - minRow + 1,
  };
}

// ─────────────────────────────────────────────────────────────
// Board Component
// ─────────────────────────────────────────────────────────────

/** 單格尺寸（px） */
const CELL_W = 44;
const CELL_H = 44;

/**
 * Board Component（平面網格版）
 *
 * 以絕對定位把每張骨牌放在 2D canvas 上，
 * 模擬真實桌遊的平面分岔排法。
 */
@Component({
  selector: 'app-board',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="board-container">
      <!-- 標題列 -->
      <div class="board-header">
        <h2 class="text-lg font-bold text-white flex items-center gap-2">
          🎲 Board
          @if (boardTiles().length > 0) {
            <span class="text-sm font-normal text-slate-300">
              ({{ boardTiles().length }} tiles)
            </span>
          }
        </h2>

        <!-- 端點 badge -->
        @if (boardTiles().length > 0) {
          <div class="ends-row">
            <div class="end-badge end-left">
              <span class="end-label">LEFT</span>
              <span class="end-value">{{ leftEnd() }}</span>
            </div>
            <div class="end-badge end-right">
              <span class="end-label">RIGHT</span>
              <span class="end-value">{{ rightEnd() }}</span>
            </div>
          </div>
        }
      </div>

      <!-- Canvas 區域 -->
      <div class="canvas-scroll">
        @if (boardTiles().length === 0) {
          <div class="empty-board">
            <span class="text-slate-500 text-sm">Waiting for the first tile...</span>
          </div>
        } @else {
          <!-- 絕對定位容器，高度由骨牌數量決定 -->
          <div
            class="tile-canvas"
            [style.width.px]="canvasWidth()"
            [style.height.px]="canvasHeight()"
          >
            @for (p of layout(); track p.tile.id; let i = $index) {
              <div
                class="placed-tile"
                [class.tile-double]="p.tile.isDouble"
                [class.tile-h]="p.orientation === 'H'"
                [class.tile-v]="p.orientation === 'V'"
                [class.tile-first]="i === 0"
                [class.tile-last]="i === layout().length - 1"
                [style.left.px]="p.col * CELL_W"
                [style.top.px]="p.row * CELL_H"
                [attr.title]="p.tile.left + '|' + p.tile.right"
              >
                @if (p.orientation === 'H') {
                  <!-- 水平牌：左端 | 分隔 | 右端 -->
                  <span class="pip">{{ p.flipped ? p.tile.right : p.tile.left }}</span>
                  <span class="divider-v">|</span>
                  <span class="pip">{{ p.flipped ? p.tile.left : p.tile.right }}</span>
                } @else {
                  <!-- 垂直牌：上端 — 下端 -->
                  <span class="pip">{{ p.flipped ? p.tile.right : p.tile.left }}</span>
                  <span class="divider-h">—</span>
                  <span class="pip">{{ p.flipped ? p.tile.left : p.tile.right }}</span>
                }
              </div>
            }
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .board-container {
      @apply bg-slate-800 rounded-xl p-4 min-h-[160px] flex flex-col gap-3;
    }

    .board-header {
      @apply flex items-center justify-between flex-wrap gap-2;
    }

    .ends-row {
      @apply flex items-center gap-3;
    }

    .end-badge {
      @apply flex items-center gap-1.5 bg-slate-700 rounded-lg px-3 py-1;
    }

    .end-label {
      @apply text-xs text-slate-400 font-medium uppercase tracking-wide;
    }

    .end-value {
      @apply text-lg font-bold text-yellow-400 leading-none;
    }

    /* 捲動區域 */
    .canvas-scroll {
      @apply overflow-auto rounded-lg bg-slate-900/50 p-3 min-h-[120px];
    }

    .empty-board {
      @apply flex items-center justify-center py-8;
    }

    /* 絕對定位畫布 */
    .tile-canvas {
      @apply relative;
    }

    /* ── 單張骨牌基底 ──────────────────────────────────── */
    .placed-tile {
      @apply absolute flex items-center justify-center
             bg-slate-100 text-slate-900 rounded
             shadow-md select-none font-bold text-xs
             border border-slate-300;
      transition: box-shadow 0.15s;
    }

    /* 水平牌：寬 40px，高 22px */
    .tile-h {
      width: 40px;
      height: 22px;
      flex-direction: row;
      gap: 2px;
      /* 水平牌置中在格子內 */
      margin-top: 11px;
    }

    /* 垂直牌：寬 22px，高 40px */
    .tile-v {
      width: 22px;
      height: 40px;
      flex-direction: column;
      gap: 1px;
      margin-left: 11px;
    }

    /* 對子：米黃色底，加粗框 */
    .tile-double {
      @apply bg-yellow-100 border-yellow-400;
    }

    /* 第一張牌（[9|9]）：特別標示 */
    .tile-first {
      @apply bg-amber-200 border-amber-500 ring-2 ring-amber-400;
    }

    /* 最後一張牌：綠色外框（提示可接的端點） */
    .tile-last {
      @apply ring-2 ring-emerald-400;
    }

    .pip {
      @apply text-xs font-bold leading-none;
    }

    .divider-v {
      @apply text-slate-400 text-xs leading-none;
    }

    .divider-h {
      @apply text-slate-400 text-xs leading-none;
    }
  `],
})
export class BoardComponent {
  readonly boardTiles = boardTiles;
  readonly leftEnd    = leftEnd;
  readonly rightEnd   = rightEnd;

  /** 格子尺寸常數，暴露給 template 使用 */
  readonly CELL_W = CELL_W;
  readonly CELL_H = CELL_H;

  /** 計算所有骨牌的平面佈局（Computed Signal） */
  readonly layout = computed(() => {
    const placed = layoutTiles(this.boardTiles());
    const { normalized } = normalizePlaced(placed);
    return normalized;
  });

  /** Canvas 寬度（px） */
  readonly canvasWidth = computed(() => {
    const { cols } = normalizePlaced(layoutTiles(this.boardTiles()));
    return Math.max(cols * CELL_W + 16, 200);
  });

  /** Canvas 高度（px） */
  readonly canvasHeight = computed(() => {
    const { rows } = normalizePlaced(layoutTiles(this.boardTiles()));
    return Math.max(rows * CELL_H + 16, 60);
  });
}
