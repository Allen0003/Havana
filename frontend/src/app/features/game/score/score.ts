import { Component, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  players,
  boneyardCount,
  currentTurnPlayerId,
  localPlayerId,
  connectionStatus,
  gameRoomSignal,
} from '../../../core/store/game.store';

/**
 * Score Component
 *
 * 即時顯示所有玩家的手牌數量、備用庫數量，以及目前輪到誰出牌。
 * 數值隨 WebSocket 廣播自動更新（Computed Signals）。
 * AI 玩家以 🤖 badge 標示。
 */
@Component({
  selector: 'app-score',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="score-container">
      <!-- 標題：依模式顯示 -->
      <h2 class="text-lg font-bold text-white mb-3 flex items-center gap-2">
        📊 Scoreboard
        @if (isSoloMode()) {
          <span class="solo-badge">VS AI</span>
        }
      </h2>

      <!-- 玩家狀態列表 -->
      <div class="players-list">
        @for (player of players(); track player.id) {
          <div
            class="player-row"
            [class.current-turn]="player.id === currentTurnPlayerId()"
            [class.is-me]="player.id === localPlayerId()"
            [class.is-ai]="player.ai"
          >
            <!-- 輪次指示 -->
            <div class="flex items-center gap-2 flex-1 min-w-0">
              @if (player.id === currentTurnPlayerId()) {
                <span class="text-yellow-400 text-sm flex-shrink-0">▶</span>
              } @else {
                <span class="text-slate-600 text-sm flex-shrink-0">○</span>
              }

              <!-- 玩家名稱 + badges -->
              <span class="player-name truncate">{{ player.name }}</span>
              @if (player.id === localPlayerId()) {
                <span class="me-badge">You</span>
              }
              @if (player.ai) {
                <span class="ai-badge">🤖 AI</span>
              }
              @if (!player.connected && !player.ai) {
                <span class="text-red-400 text-xs flex-shrink-0">(offline)</span>
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

    .solo-badge {
      @apply bg-purple-600 text-white text-xs font-bold px-2 py-0.5 rounded-full;
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

    .is-ai {
      @apply opacity-90;
    }

    .player-name {
      @apply text-white text-sm font-medium;
    }

    .me-badge {
      @apply bg-blue-600 text-white text-xs px-1.5 py-0.5 rounded flex-shrink-0;
    }

    .ai-badge {
      @apply bg-purple-700 text-purple-200 text-xs px-1.5 py-0.5 rounded flex-shrink-0;
    }

    .tile-count {
      @apply flex flex-col items-center flex-shrink-0 ml-2;
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

  /** 是否為單人 VS AI 模式（房間內有任何 AI 玩家） */
  readonly isSoloMode = computed(() =>
    gameRoomSignal()?.players.some(p => p.ai) ?? false
  );

  connectionClass(): string {
    switch (this.connectionStatus()) {
      case 'CONNECTED':    return 'bg-green-900 text-green-300';
      case 'CONNECTING':   return 'bg-yellow-900 text-yellow-300';
      case 'DISCONNECTED': return 'bg-red-900 text-red-300';
    }
  }

  connectionLabel(): string {
    switch (this.connectionStatus()) {
      case 'CONNECTED':    return '● Connected';
      case 'CONNECTING':   return '◌ Connecting...';
      case 'DISCONNECTED': return '○ Disconnected';
    }
  }
}
