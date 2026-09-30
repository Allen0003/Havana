import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  players,
  boneyardCount,
  currentTurnPlayerId,
  localPlayerId,
  connectionStatus,
} from '../../../core/store/game.store';

/**
 * Score Component
 *
 * 即時顯示所有玩家的手牌數量、備用庫數量，以及目前輪到誰出牌。
 * 數值隨 WebSocket 廣播自動更新（Computed Signals）。
 */
@Component({
  selector: 'app-score',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="score-container">
      <h2 class="text-lg font-bold text-white mb-3">📊 Scoreboard</h2>

      <!-- 玩家狀態列表 -->
      <div class="players-list">
        @for (player of players(); track player.id) {
          <div
            class="player-row"
            [class.current-turn]="player.id === currentTurnPlayerId()"
            [class.is-me]="player.id === localPlayerId()"
          >
            <!-- 玩家名稱 -->
            <div class="flex items-center gap-2 flex-1">
              @if (player.id === currentTurnPlayerId()) {
                <span class="text-yellow-400 text-sm">▶</span>
              } @else {
                <span class="text-slate-600 text-sm">○</span>
              }
              <span class="player-name">
                {{ player.name }}
                @if (player.id === localPlayerId()) {
                  <span class="me-badge">You</span>
                }
              </span>
              @if (!player.connected) {
                <span class="text-red-400 text-xs">(disconnected)</span>
              }
            </div>

            <!-- 手牌數 -->
            <div class="tile-count">
              <span class="text-slate-400 text-xs">tiles</span>
              <span
                class="count-value"
                [class.low-count]="player.handCount <= 2"
              >
                {{ player.handCount }}
              </span>
            </div>
          </div>
        }
      </div>

      <!-- 備用庫 -->
      <div class="boneyard-row">
        <span class="text-slate-400 text-sm">🦴 Boneyard</span>
        <span class="count-value text-slate-300">{{ boneyardCount() }}</span>
      </div>

      <!-- 連線狀態 -->
      <div class="connection-status" [class]="connectionClass()">
        <span class="status-dot"></span>
        <span class="text-xs">{{ connectionLabel() }}</span>
      </div>
    </div>
  `,
  styles: [`
    .score-container {
      @apply bg-slate-800 rounded-xl p-4 flex flex-col gap-2;
    }

    .players-list {
      @apply flex flex-col gap-1;
    }

    .player-row {
      @apply flex items-center justify-between bg-slate-700 rounded-lg px-3 py-2
             transition-colors duration-300;
    }

    .current-turn {
      @apply bg-slate-600 ring-1 ring-yellow-400;
    }

    .is-me {
      @apply ring-1 ring-blue-400;
    }

    .player-name {
      @apply text-white text-sm font-medium;
    }

    .me-badge {
      @apply bg-blue-600 text-white text-xs px-1.5 py-0.5 rounded ml-1;
    }

    .tile-count {
      @apply flex flex-col items-center;
    }

    .count-value {
      @apply text-white font-bold text-lg leading-none;
    }

    .low-count {
      @apply text-orange-400 animate-pulse;
    }

    .boneyard-row {
      @apply flex items-center justify-between bg-slate-700 rounded-lg px-3 py-2;
    }

    .connection-status {
      @apply flex items-center gap-1.5 px-2 py-1 rounded-lg mt-1;
    }

    .status-dot {
      @apply w-2 h-2 rounded-full;
    }
  `],
})
export class ScoreComponent {
  readonly players = players;
  readonly boneyardCount = boneyardCount;
  readonly currentTurnPlayerId = currentTurnPlayerId;
  readonly localPlayerId = localPlayerId;
  readonly connectionStatus = connectionStatus;

  connectionClass(): string {
    const status = this.connectionStatus();
    switch (status) {
      case 'CONNECTED':    return 'bg-green-900 text-green-300';
      case 'CONNECTING':   return 'bg-yellow-900 text-yellow-300';
      case 'DISCONNECTED': return 'bg-red-900 text-red-300';
    }
  }

  connectionLabel(): string {
    const status = this.connectionStatus();
    switch (status) {
      case 'CONNECTED':    return '● Connected';
      case 'CONNECTING':   return '◌ Connecting...';
      case 'DISCONNECTED': return '○ Disconnected';
    }
  }
}
