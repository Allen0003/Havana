import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';

import { BoardComponent } from './board/board';
import { HandComponent } from './hand/hand';
import { ScoreComponent } from './score/score';

import { WebSocketService, ConnectionStatus } from '../../core/services/web-socket.service';
import {
  gameRoomSignal,
  localPlayerId,
  myHandSignal,
  connectionStatus,
  errorMessage,
  isGameFinished,
  winnerId,
  isTrancado,
  trancadoScores,
  players,
} from '../../core/store/game.store';
import { GameStatus } from '../../core/models/game-room.model';

/**
 * Game Page Component
 *
 * 主要遊戲畫面，負責：
 * 1. 從路由參數取得 roomId / playerId
 * 2. 建立 WebSocket 連線並訂閱房間更新
 * 3. 將廣播寫入全域 Signal store
 * 4. 組合 board/hand/score 子元件
 * 5. 偵測遊戲結束，導向 result 頁面
 */
@Component({
  selector: 'app-game',
  standalone: true,
  imports: [CommonModule, BoardComponent, HandComponent, ScoreComponent],
  template: `
    <div class="game-page">
      <!-- Header -->
      <header class="game-header">
        <div class="flex items-center gap-3">
          <span class="text-2xl">🎲</span>
          <h1 class="text-white font-bold text-xl">Havana Dominoes</h1>
        </div>
        <div class="flex items-center gap-3">
          <span class="text-slate-400 text-sm font-mono">
            Room: {{ currentRoomId }}
          </span>
          <button class="btn-leave" (click)="onLeave()">Leave</button>
        </div>
      </header>

      <!-- Error banner -->
      @if (errorMessage()) {
        <div class="error-banner" (click)="clearError()">
          ⚠️ {{ errorMessage() }}
          <span class="text-xs ml-2 opacity-75">(click to dismiss)</span>
        </div>
      }

      <!-- Game Finished overlay -->
      @if (isGameFinished()) {
        <div class="finished-overlay">
          <div class="finished-card">
            @if (isTrancado()) {
              <div class="text-4xl mb-2">🔒</div>
              <h2 class="text-2xl font-bold text-white mb-1">Trancado!</h2>
              <p class="text-slate-300 mb-4">Game locked — no moves possible</p>
              @if (trancadoScores()) {
                <div class="scores-list">
                  @for (player of players(); track player.id) {
                    <div class="score-row" [class.winner-row]="player.id === winnerId()">
                      <span class="text-white">{{ player.name }}</span>
                      <span class="text-slate-300 font-bold">
                        {{ trancadoScores()![player.id] }} pts
                      </span>
                      @if (player.id === winnerId()) {
                        <span class="text-yellow-400 text-sm">🏆 Winner</span>
                      }
                    </div>
                  }
                </div>
              }
            } @else {
              <div class="text-4xl mb-2">🏆</div>
              <h2 class="text-2xl font-bold text-white mb-1">Game Over!</h2>
              @if (winnerName()) {
                <p class="text-yellow-400 text-lg font-semibold">
                  {{ winnerName() }} wins!
                </p>
              }
            }
            <button class="btn-new-game mt-6" (click)="onLeave()">
              Back to Lobby
            </button>
          </div>
        </div>
      }

      <!-- Main layout: score left | board + hand right -->
      <main class="game-main">
        <aside class="game-sidebar">
          <app-score />
        </aside>

        <section class="game-content">
          <app-board />
          <app-hand />
        </section>
      </main>
    </div>
  `,
  styles: [`
    .game-page {
      @apply min-h-screen bg-slate-900 flex flex-col;
    }

    .game-header {
      @apply flex items-center justify-between px-6 py-3
             bg-slate-800 border-b border-slate-700;
    }

    .btn-leave {
      @apply bg-slate-700 hover:bg-slate-600 text-white text-sm
             px-3 py-1.5 rounded-lg transition-colors;
    }

    .error-banner {
      @apply bg-red-800 text-white text-sm px-4 py-2 cursor-pointer
             hover:bg-red-700 transition-colors;
    }

    .finished-overlay {
      @apply fixed inset-0 bg-black/70 flex items-center justify-center z-50;
    }

    .finished-card {
      @apply bg-slate-800 rounded-2xl p-8 flex flex-col items-center
             shadow-2xl min-w-[320px];
    }

    .scores-list {
      @apply w-full flex flex-col gap-2;
    }

    .score-row {
      @apply flex items-center justify-between bg-slate-700 rounded-lg px-3 py-2 gap-3;
    }

    .winner-row {
      @apply bg-yellow-900 ring-1 ring-yellow-400;
    }

    .btn-new-game {
      @apply bg-blue-600 hover:bg-blue-500 text-white font-semibold
             px-6 py-2 rounded-xl transition-colors;
    }

    .game-main {
      @apply flex flex-col md:flex-row gap-4 p-4 flex-1;
    }

    .game-sidebar {
      @apply md:w-64 flex-shrink-0;
    }

    .game-content {
      @apply flex-1 flex flex-col gap-4;
    }
  `],
})
export class GameComponent implements OnInit, OnDestroy {
  private ws = inject(WebSocketService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  readonly errorMessage = errorMessage;
  readonly isGameFinished = isGameFinished;
  readonly isTrancado = isTrancado;
  readonly trancadoScores = trancadoScores;
  readonly winnerId = winnerId;
  readonly players = players;

  currentRoomId = '';
  private subs = new Subscription();

  ngOnInit(): void {
    // 從 query params 取得 roomId 和 playerId
    const params = this.route.snapshot.queryParamMap;
    const roomId  = params.get('roomId')  ?? '';
    const playerId = params.get('playerId') ?? '';

    if (!roomId || !playerId) {
      this.router.navigate(['/']);
      return;
    }

    this.currentRoomId = roomId;

    // 寫入本機玩家 ID
    localPlayerId.set(playerId);

    // 訂閱房間廣播，寫入 Signal store
    this.subs.add(
      this.ws.getGameRoomUpdates().subscribe(room => {
        gameRoomSignal.set(room);
      })
    );

    // 訂閱連線狀態
    this.subs.add(
      this.ws.getConnectionStatus().subscribe(status => {
        connectionStatus.set(status as any);
      })
    );

    // 訂閱錯誤訊息
    this.subs.add(
      this.ws.getErrors().subscribe(err => {
        errorMessage.set(err.message);
        // 3 秒後自動清除
        setTimeout(() => errorMessage.set(null), 3000);
      })
    );

    // 建立 WebSocket 連線
    this.ws.connect(roomId);
  }

  ngOnDestroy(): void {
    this.subs.unsubscribe();
    this.ws.disconnect();
    // 清除 store
    gameRoomSignal.set(null);
    myHandSignal.set([]);
    localPlayerId.set(null);
    errorMessage.set(null);
  }

  clearError(): void {
    errorMessage.set(null);
  }

  onLeave(): void {
    this.ws.disconnect();
    this.router.navigate(['/']);
  }

  /** 找出勝者的顯示名稱 */
  winnerName(): string | null {
    const id = winnerId();
    if (!id) return null;
    return players().find(p => p.id === id)?.name ?? null;
  }
}
