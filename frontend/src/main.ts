// Polyfill: sockjs-client 和 stompjs 依賴 Node.js 的 `global`，
// 瀏覽器沒有這個變數，需要在所有模組載入前先補上。
(window as any)['global'] = window;

import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';

bootstrapApplication(App, appConfig)
  .catch((err) => console.error(err));
