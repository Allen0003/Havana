import { test, expect } from '@playwright/test';
import { LobbyPage } from './pages/lobby.page';
import { GamePage } from './pages/game.page';

/**
 * Game 頁面 E2E 測試（M4-2）
 *
 * 測試遊戲頁面的核心 UI 行為。
 * 完整對局測試（4人連線出牌）需要真實後端，
 * 此骨架提供 UI 元素驗證與連線狀態檢查。
 *
 * 要執行完整 E2E：
 *   1. cd backend && mvn spring-boot:run
 *   2. cd frontend && npx playwright test
 */
test.describe('Game 頁面 UI', () => {

  test.beforeEach(async ({ page }) => {
    // 直接導向 /game 並帶假參數來測試頁面元素
    // 真實測試中要先建立房間並取得 roomId / playerId
    await page.goto('/game?roomId=test-room&playerId=test-player');
  });

  test('應該顯示所有主要 UI 元件', async ({ page }) => {
    const gamePage = new GamePage(page);

    // 等待頁面載入
    await gamePage.waitForLoad();

    // Header
    await expect(page.getByText('Havana Dominoes')).toBeVisible();
    await expect(gamePage.leaveButton).toBeVisible();

    // 核心元件存在
    await expect(gamePage.boardContainer).toBeVisible();
    await expect(gamePage.scoreContainer).toBeVisible();
  });

  test('Header 應顯示 Room ID', async ({ page }) => {
    const gamePage = new GamePage(page);
    await gamePage.waitForLoad();
    await expect(gamePage.roomIdLabel).toContainText('test-room');
  });

  test('點擊 Leave 按鈕應導回大廳', async ({ page }) => {
    const gamePage = new GamePage(page);
    await gamePage.waitForLoad();
    await gamePage.leaveButton.click();
    await expect(page).toHaveURL('/');
  });

  test('連線狀態指示器應可見', async ({ page }) => {
    const gamePage = new GamePage(page);
    await gamePage.waitForLoad();

    // 連線狀態元件存在（可能是 Connecting... 或 Connected 或 Disconnected）
    await expect(gamePage.connectionStatus).toBeVisible();
    const statusText = await gamePage.connectionStatus.textContent();
    expect(statusText).toMatch(/Connected|Connecting|Disconnected/);
  });

  test('Board 元件初始應顯示等待提示', async ({ page }) => {
    const gamePage = new GamePage(page);
    await gamePage.waitForLoad();

    // 桌面尚無骨牌時顯示等待提示
    await expect(gamePage.boardContainer).toBeVisible();
    const boardText = await gamePage.boardContainer.textContent();
    expect(boardText).toContain('Board');
  });

  test('Score 元件應顯示計分看板標題', async ({ page }) => {
    const gamePage = new GamePage(page);
    await gamePage.waitForLoad();
    await expect(gamePage.scoreContainer).toContainText('Scoreboard');
  });
});

/**
 * 完整遊戲流程 E2E（需要後端運行）
 *
 * 這組測試測試真實的 Lobby → Create Room → Game 流程。
 * 只測試單一玩家視角（建立房間後進入遊戲等待其他人加入）。
 */
test.describe('Lobby → Game 完整流程', () => {

  test('建立房間後應自動進入遊戲頁面', async ({ page }) => {
    const lobby = new LobbyPage(page);
    await lobby.goto();

    // 點擊建立房間（需要後端）
    await lobby.createNameInput.fill('Alice');
    await lobby.createButton.click();

    // 應跳轉到遊戲頁面，URL 含有 roomId 和 playerId
    await expect(page).toHaveURL(/\/game\?roomId=.+&playerId=.+/, { timeout: 10_000 });

    const gamePage = new GamePage(page);
    await gamePage.waitForLoad();

    // 房間 ID 應顯示在 header
    const url = new URL(page.url());
    const roomId = url.searchParams.get('roomId')!;
    await expect(gamePage.roomIdLabel).toContainText(roomId);
  });

  test('遊戲頁面應顯示連線嘗試', async ({ page }) => {
    const lobby = new LobbyPage(page);
    await lobby.goto();

    await lobby.createNameInput.fill('Alice');
    await lobby.createButton.click();

    await expect(page).toHaveURL(/\/game/, { timeout: 10_000 });

    const gamePage = new GamePage(page);
    await gamePage.waitForLoad();

    // 進入頁面後應嘗試 WebSocket 連線
    // 狀態應從 Connecting 變成 Connected（或 Disconnected 如果後端未開）
    await expect(gamePage.connectionStatus).toBeVisible();
    const status = await gamePage.connectionStatus.textContent();
    expect(status).toMatch(/Connecting|Connected|Disconnected/);
  });
});

/**
 * 錯誤處理 E2E
 */
test.describe('遊戲錯誤處理', () => {

  test('直接訪問 /game 沒有參數應重導向至大廳', async ({ page }) => {
    // 沒有 roomId 和 playerId 的情況，GameComponent 應重導向
    await page.goto('/game');
    await expect(page).toHaveURL('/', { timeout: 5_000 });
  });

  test('訪問不存在的路徑應重導向至大廳', async ({ page }) => {
    await page.goto('/non-existent-page');
    await expect(page).toHaveURL('/', { timeout: 5_000 });
  });
});
