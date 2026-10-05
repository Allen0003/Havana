import { type Page, type Locator, expect } from '@playwright/test';

/**
 * Game Page Object Model
 *
 * 封裝遊戲頁面的所有互動。
 */
export class GamePage {
  readonly page: Page;

  // Header
  readonly roomIdLabel: Locator;
  readonly leaveButton: Locator;

  // Board
  readonly boardContainer: Locator;
  readonly boardTiles: Locator;
  readonly leftEndBadge: Locator;
  readonly rightEndBadge: Locator;

  // Hand
  readonly handTiles: Locator;
  readonly passButton: Locator;
  readonly yourTurnBadge: Locator;

  // Score
  readonly scoreContainer: Locator;
  readonly connectionStatus: Locator;

  // Toast / Error
  readonly errorBanner: Locator;

  // Finished overlay
  readonly finishedOverlay: Locator;
  readonly winnerText: Locator;
  readonly trancadoText: Locator;
  readonly backToLobbyButton: Locator;

  constructor(page: Page) {
    this.page = page;

    this.roomIdLabel       = page.locator('.game-header .font-mono');
    this.leaveButton       = page.getByRole('button', { name: 'Leave' });

    this.boardContainer    = page.locator('app-board');
    this.boardTiles        = page.locator('app-board .board-tile');
    this.leftEndBadge      = page.locator('app-board .end-badge').first();
    this.rightEndBadge     = page.locator('app-board .end-badge').last();

    this.handTiles         = page.locator('app-hand .hand-tile');
    this.passButton        = page.getByRole('button', { name: 'Pass / Knock' });
    this.yourTurnBadge     = page.locator('.turn-badge');

    this.scoreContainer    = page.locator('app-score');
    this.connectionStatus  = page.locator('.connection-status');

    this.errorBanner       = page.locator('.error-banner');

    this.finishedOverlay   = page.locator('.finished-overlay');
    this.winnerText        = page.locator('.finished-card h2');
    this.trancadoText      = page.locator('.finished-card p').first();
    this.backToLobbyButton = page.getByRole('button', { name: 'Back to Lobby' });
  }

  /** 等待遊戲頁面載入完成 */
  async waitForLoad(): Promise<void> {
    await expect(this.page).toHaveURL(/\/game/);
    await expect(this.boardContainer).toBeVisible();
    await expect(this.scoreContainer).toBeVisible();
  }

  /** 等待 WebSocket 連線成功 */
  async waitForConnection(): Promise<void> {
    await expect(this.connectionStatus).toContainText('Connected', { timeout: 10_000 });
  }

  /** 點擊第 n 張手牌（0-indexed） */
  async clickTile(index: number): Promise<void> {
    const tile = this.handTiles.nth(index);
    await expect(tile).toBeVisible();
    await tile.click();
  }

  /** 選擇出牌方向（出現在 end-selection 之後） */
  async selectEnd(end: 'Left' | 'Right'): Promise<void> {
    const btn = this.page.getByRole('button', { name: new RegExp(end, 'i') }).first();
    await expect(btn).toBeVisible({ timeout: 3_000 });
    await btn.click();
  }

  /** 取得手牌數量 */
  async getHandCount(): Promise<number> {
    return await this.handTiles.count();
  }

  /** 取得桌面骨牌數量 */
  async getBoardTileCount(): Promise<number> {
    return await this.boardTiles.count();
  }

  /** 等待遊戲結束 overlay 出現 */
  async waitForFinished(): Promise<void> {
    await expect(this.finishedOverlay).toBeVisible({ timeout: 15_000 });
  }

  /** 是否顯示「Your Turn」badge */
  async isMyTurn(): Promise<boolean> {
    return await this.yourTurnBadge.isVisible();
  }

  /** 取得 error banner 文字 */
  async getErrorBannerText(): Promise<string> {
    await expect(this.errorBanner).toBeVisible({ timeout: 5_000 });
    return (await this.errorBanner.textContent()) ?? '';
  }
}
