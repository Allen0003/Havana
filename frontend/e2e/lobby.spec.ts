import { test, expect } from '@playwright/test';
import { LobbyPage } from './pages/lobby.page';

/**
 * Lobby 頁面 E2E 測試（M4-2）
 *
 * 測試大廳頁面的基本互動：
 * - 頁面正確渲染
 * - Create Room 表單驗證
 * - Join Room 表單驗證
 * - 錯誤狀態顯示
 *
 * 注意：需要後端 Spring Boot 伺服器在 localhost:8080 執行。
 * 純 UI 測試（不依賴後端）會使用 API mock。
 */
test.describe('Lobby 頁面', () => {

  test('應該正確顯示大廳頁面標題與兩個區塊', async ({ page }) => {
    const lobby = new LobbyPage(page);
    await lobby.goto();

    // 標題
    await expect(page.getByRole('heading', { name: 'Havana' })).toBeVisible();
    await expect(page.getByText('Cuban Dominoes — 4 Players')).toBeVisible();

    // 兩個 card
    await expect(page.getByRole('heading', { name: /Create Room/ })).toBeVisible();
    await expect(page.getByRole('heading', { name: /Join Room/ })).toBeVisible();
  });

  test('Create Room 按鈕在名稱為空時應該被禁用', async ({ page }) => {
    const lobby = new LobbyPage(page);
    await lobby.goto();

    // 初始狀態：名稱為空，按鈕禁用
    await expect(lobby.createButton).toBeDisabled();

    // 輸入名稱後啟用
    await lobby.createNameInput.fill('Alice');
    await expect(lobby.createButton).toBeEnabled();

    // 清空後再次禁用
    await lobby.createNameInput.clear();
    await expect(lobby.createButton).toBeDisabled();
  });

  test('Join Room 按鈕在名稱或 Room ID 為空時應該被禁用', async ({ page }) => {
    const lobby = new LobbyPage(page);
    await lobby.goto();

    // 初始狀態：禁用
    await expect(lobby.joinButton).toBeDisabled();

    // 只填名稱：仍然禁用
    await lobby.joinNameInput.fill('Bob');
    await expect(lobby.joinButton).toBeDisabled();

    // 再填 Room ID：啟用
    await lobby.joinRoomIdInput.fill('some-room-id');
    await expect(lobby.joinButton).toBeEnabled();
  });

  test('名稱欄位應限制最多 20 個字元', async ({ page }) => {
    const lobby = new LobbyPage(page);
    await lobby.goto();

    const longName = 'A'.repeat(25);
    await lobby.createNameInput.fill(longName);

    // HTML maxlength=20 會限制輸入
    const value = await lobby.createNameInput.inputValue();
    expect(value.length).toBeLessThanOrEqual(20);
  });

  test('加入不存在的房間應顯示錯誤訊息', async ({ page }) => {
    const lobby = new LobbyPage(page);
    await lobby.goto();

    await lobby.joinNameInput.fill('Bob');
    await lobby.joinRoomIdInput.fill('non-existent-room-id');
    await lobby.joinButton.click();

    // 等待錯誤訊息出現（需要後端回傳 404）
    await expect(lobby.errorBox).toBeVisible({ timeout: 10_000 });
    const errorText = await lobby.errorBox.textContent();
    expect(errorText).toContain('Room not found');
  });

  test('How to Play 規則說明應該顯示', async ({ page }) => {
    const lobby = new LobbyPage(page);
    await lobby.goto();

    await expect(page.getByText('How to Play')).toBeVisible();
    await expect(page.getByText('double-nine set')).toBeVisible();
    await expect(page.getByText('Trancado')).toBeVisible();
  });
});
