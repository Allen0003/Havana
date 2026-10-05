import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright E2E 測試設定
 *
 * 前端開發伺服器（ng serve）需在執行測試前啟動，
 * 或設定 webServer 讓 Playwright 自動啟動。
 */
export default defineConfig({
  testDir: './e2e',

  /* 每個測試的最大執行時間 */
  timeout: 30_000,

  /* 預期斷言的最長等待時間 */
  expect: {
    timeout: 5_000,
  },

  /* 全部測試失敗立即停止 */
  fullyParallel: false,

  /* CI 環境下不允許 test.only */
  forbidOnly: !!process.env['CI'],

  /* CI 不重試，本地重試 1 次 */
  retries: process.env['CI'] ? 0 : 1,

  /* worker 數量 */
  workers: 1,

  /* 測試報告輸出 */
  reporter: [['html', { outputFolder: 'playwright-report', open: 'never' }]],

  use: {
    /* 所有測試都打這個 base URL */
    baseURL: 'http://localhost:4200',

    /* 每次測試留下 trace（失敗時可回放） */
    trace: 'on-first-retry',

    /* 截圖：只在失敗時 */
    screenshot: 'only-on-failure',

    /* headless 模式 */
    headless: true,
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],

  /**
   * 自動啟動 Angular dev server（如果尚未在執行）。
   * webServer 設定：Playwright 會在執行測試前啟動 ng serve，
   * 測試結束後自動關閉。
   */
  webServer: {
    command: 'npx ng serve --configuration development',
    url: 'http://localhost:4200',
    reuseExistingServer: true,
    timeout: 120_000,
    stdout: 'pipe',
    stderr: 'pipe',
  },
});
