import { type Page, type Locator, expect } from '@playwright/test';

/**
 * Lobby Page Object Model
 *
 * 封裝大廳頁面的所有互動，測試只需呼叫這些方法，
 * 不需要知道 selector 細節。
 */
export class LobbyPage {
  readonly page: Page;

  // Create Room 區塊
  readonly createNameInput: Locator;
  readonly createButton: Locator;
  readonly roomIdDisplay: Locator;
  readonly copyButton: Locator;

  // Join Room 區塊
  readonly joinNameInput: Locator;
  readonly joinRoomIdInput: Locator;
  readonly joinButton: Locator;

  // 錯誤訊息
  readonly errorBox: Locator;

  constructor(page: Page) {
    this.page = page;
    this.createNameInput  = page.getByPlaceholder('e.g. Alice');
    this.createButton     = page.getByRole('button', { name: 'Create Room' });
    this.roomIdDisplay    = page.locator('.room-id-box .font-mono');
    this.copyButton       = page.getByRole('button', { name: /Copy/ });
    this.joinNameInput    = page.getByPlaceholder('e.g. Bob');
    this.joinRoomIdInput  = page.getByPlaceholder('Paste Room ID here');
    this.joinButton       = page.getByRole('button', { name: 'Join Room' });
    this.errorBox         = page.locator('.error-box');
  }

  /** 開啟大廳頁面 */
  async goto(): Promise<void> {
    await this.page.goto('/');
    await expect(this.page).toHaveTitle(/Havana|Angular/);
  }

  /** 建立房間，回傳 roomId */
  async createRoom(hostName: string): Promise<string> {
    await this.createNameInput.fill(hostName);
    await this.createButton.click();
    // 等待導向 /game 頁面
    await this.page.waitForURL(/\/game/);
    return new URL(this.page.url()).searchParams.get('roomId') ?? '';
  }

  /** 加入房間 */
  async joinRoom(playerName: string, roomId: string): Promise<void> {
    await this.joinNameInput.fill(playerName);
    await this.joinRoomIdInput.fill(roomId);
    await this.joinButton.click();
  }

  /** 取得錯誤訊息文字 */
  async getError(): Promise<string> {
    await expect(this.errorBox).toBeVisible();
    return (await this.errorBox.textContent()) ?? '';
  }
}
