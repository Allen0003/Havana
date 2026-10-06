import { Component, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { boardTiles, leftEnd, rightEnd } from '../../../core/store/game.store';
import { Tile } from '../../../core/models/tile.model';

// ─────────────────────────────────────────────────────────────
// 型別定義
// ─────────────────────────────────────────────────────────────

/**
 * 主幹行進方向（蛇形路徑的當前段方向）
 *   E = 向右、W = 向左、S = 向下、N = 向上
 */
type Dir = 'E' | 'W' | 'S' | 'N';

/** 牌的擺放方向 */
type Orient = 'H' | 'V'; // Horizontal | Vertical

/** 座標增量表 */
const DELTA: Record<Dir, { dc: number; dr: number }> = {
  E: {  dc: 1,  dr: 0 },
  W: {  dc: -1, dr: 0 },
  S: {  dc: 0,  dr: 1 },
  N: {  dc: 0,  dr: -1 },
};

/** 蛇形轉彎順序：E→S→W→N→E… */
const SNAKE: Dir[] = ['E', 'S', 'W', 'N'];

/** 每段直線最多走幾格後強制轉彎（仿真實桌面寬度） */
const SEG_LEN = 7;

/** 單格像素尺寸 */
const CW = 48; // cell width
const CH = 48; // cell height

// ─────────────────────────────────────────────────────────────
// 佈局資料結構
// ─────────────────────────────────────────────────────────────

export interface PlacedTile {
  tile:    Tile;
  col:     number;   // 邏輯格 X
  row:     number;   // 邏輯格 Y
  orient:  Orient;   // H = 水平，V = 垂直
  /**
   * 顯示翻轉旗標：
   * - false → 上/左端顯示 tile.left，下/右端顯示 tile.right
   * - true  → 上/左端顯示 tile.right，下/右端顯示 tile.left
   */
  flipped: boolean;
  isFirst: boolean;
  isLeft:  boolean;  // 是否是桌面左端最後一張
  isRight: boolean;  // 是否是桌面右端最後一張
}

// ─────────────────────────────────────────────────────────────
// 核心佈局演算法
// ─────────────────────────────────────────────────────────────
//
// 真實 Cuban Domino 排版規則：
//   1. [9|9] 橫放在中央，為「起點 spinner」。
//   2. 後續每張牌依出牌端（LEFT / RIGHT）往主幹方向延伸。
//      後端的 boardTiles 陣列依「從左端到右端」順序排列。
//   3. 雙牌（double / spinner）必須 **垂直於** 行進方向擺放，
//      且不佔用方向格——後一張牌繼續沿同方向延伸。
//   4. 到達 SEG_LEN 格後強制轉彎，形成蛇形路徑。
//
// 此函式將 boardTiles（後端提供，從左端到右端的線性陣列）
// 分成「左半段」（index 0 往左延伸）和「右半段」（最後一張往右延伸），
// 然後分別用蛇形算法計算座標，最後合併。
// ─────────────────────────────────────────────────────────────

function buildLayout(tiles: Tile[]): PlacedTile[] {
  if (tiles.length === 0) return [];

  // ── 特殊情況：只有一張牌 ──────────────────────────────────
  if (tiles.length === 1) {
    return [{
      tile:    tiles[0],
      col:     0,
      row:     0,
      orient:  tiles[0].isDouble ? 'V' : 'H',
      flipped: false,
      isFirst: true,
      isLeft:  true,
      isRight: true,
    }];
  }

  // ── 找出中心點：[9|9] 是第一張牌，索引 0 ───────────────────
  // boardTiles 陣列排列方式：
  //   [0] = 最左端的牌  ...  [n-1] = 最右端的牌
  // 中心牌（[9|9]）固定是 tiles[0]。
  // 左半段：只有 tiles[0]（[9|9] 本身）
  // 右半段：tiles[1..n-1]，從 [9|9] 的右側往右延伸

  // 為了讓 [9|9] 在畫面中央，我們先把右半段放好，
  // 再把左半段（理論上除了 [9|9] 沒有更多）放回左側。
  // 雙向延伸：tiles[0] 放在 origin，右側往 E 走，左側往 W 走。

  const result: PlacedTile[] = [];

  // 中心牌固定放 (0, 0)，水平（若是對子則垂直）
  const center = tiles[0];
  result.push({
    tile:    center,
    col:     0,
    row:     0,
    orient:  center.isDouble ? 'V' : 'H',
    flipped: false,
    isFirst: true,
    isLeft:  tiles.length === 1,
    isRight: false,
  });

  // ── 右半段：從 (1,0) 往 E 開始蛇行 ─────────────────────────
  placeSegment(
    tiles.slice(1),   // tiles[1..n-1]
    /* startCol */ 1,
    /* startRow */ 0,
    /* startDirIdx */ 0,   // 第一個方向 = E
    result,
    /* markLeft */ false,
    /* markRight */ true,
    /* prevExitPip: [9|9] 的右端 */ center.right,
  );

  return result;
}

/**
 * 把一段 tiles 依蛇形算法放到 result 裡。
 *
 * @param tiles      要排列的骨牌（有序）
 * @param startCol   起始格 col
 * @param startRow   起始格 row
 * @param startDirIdx 起始方向索引（對應 SNAKE 陣列）
 * @param result     輸出陣列（in-place append）
 * @param markLeft   最後一張標記為 isLeft
 * @param markRight  最後一張標記為 isRight
 */
function placeSegment(
  tiles: Tile[],
  startCol: number,
  startRow: number,
  startDirIdx: number,
  result: PlacedTile[],
  markLeft: boolean,
  markRight: boolean,
  /** 前一張牌「離開端」的點數，用於計算翻轉 */
  prevExitPip: number,
): void {
  let col = startCol;
  let row = startRow;
  let dirIdx = startDirIdx;
  let steps = 0;
  let entryPip = prevExitPip; // 本張牌「接合端」的點數

  for (let i = 0; i < tiles.length; i++) {
    const tile = tiles[i];
    const isLast = i === tiles.length - 1;
    const dir: Dir = SNAKE[dirIdx % SNAKE.length];

    const movingH = (dir === 'E' || dir === 'W');
    let orient: Orient;
    if (tile.isDouble) {
      orient = movingH ? 'V' : 'H';
    } else {
      orient = movingH ? 'H' : 'V';
    }

    // ── 翻轉邏輯 ───────────────────────────────────────────
    // 規則：接合端（entryPip 對應的那一面）必須朝向「進入方向」
    //   E 方向：進入側 = 左側  → 接合端應是 tile.left → 若接合端是 tile.right 則 flipped=true
    //   W 方向：進入側 = 右側  → 接合端應是 tile.right
    //   S 方向：進入側 = 上側  → 接合端應是 tile.left（orient=V 時 top=left）
    //   N 方向：進入側 = 下側  → 接合端應是 tile.right
    //
    // 對子兩端相同，不需翻轉。
    let flipped = false;
    if (!tile.isDouble) {
      const entryIsLeft = (tile.left === entryPip);
      // 「進入側」在畫面上應顯示 left（flipped=false）
      // 若實際上接合端是 tile.right，需要翻轉
      if (dir === 'E' || dir === 'S') {
        // 進入側 = 顯示 top/left → 應顯示接合端 → 若接合端是 tile.right → flipped=true
        flipped = !entryIsLeft;
      } else {
        // W / N：進入側 = 顯示 bottom/right → 應顯示接合端 → 若接合端是 tile.left → flipped=true
        flipped = entryIsLeft;
      }
    }

    result.push({
      tile,
      col,
      row,
      orient,
      flipped,
      isFirst: false,
      isLeft:  isLast && markLeft,
      isRight: isLast && markRight,
    });

    // 計算本張牌的「離開端」，作為下一張的 entryPip
    if (!tile.isDouble) {
      // 接合端（entryPip）是「進入那面」，離開端就是另一面
      const joinPip = entryPip;
      entryPip = (tile.left === joinPip) ? tile.right : tile.left;
    }
    // 對子兩端相同，entryPip 維持不變

    // 移動游標
    const { dc, dr } = DELTA[dir];
    col += dc;
    row += dr;
    steps++;
    if (steps >= SEG_LEN) {
      dirIdx++;
      steps = 0;
    }
  }
}

// ─────────────────────────────────────────────────────────────
// 標準化座標（轉為全正數，並計算 canvas 尺寸）
// ─────────────────────────────────────────────────────────────

interface Layout {
  placed: PlacedTile[];
  cols: number;
  rows: number;
}

function normalize(raw: PlacedTile[]): Layout {
  if (raw.length === 0) return { placed: [], cols: 1, rows: 1 };

  const minC = Math.min(...raw.map(p => p.col));
  const minR = Math.min(...raw.map(p => p.row));
  const maxC = Math.max(...raw.map(p => p.col));
  const maxR = Math.max(...raw.map(p => p.row));

  return {
    placed: raw.map(p => ({ ...p, col: p.col - minC, row: p.row - minR })),
    cols: maxC - minC + 1,
    rows: maxR - minR + 1,
  };
}

// ─────────────────────────────────────────────────────────────
// Angular Component
// ─────────────────────────────────────────────────────────────

@Component({
  selector: 'app-board',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './board.html',
  styleUrls: ['./board.css']
})
export class BoardComponent {
  readonly boardTiles = boardTiles;
  readonly leftEnd    = leftEnd;
  readonly rightEnd   = rightEnd;

  /** 暴露常數給 template 使用 */
  readonly CW = CW;
  readonly CH = CH;

  /** 完整佈局（Computed Signal：boardTiles 變動時自動重算） */
  readonly lout = computed((): Layout => {
    const raw = buildLayout(this.boardTiles());
    return normalize(raw);
  });
}
