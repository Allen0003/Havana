import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

interface CreateRoomResponse {
  roomId: string;
  status: string;
  hostPlayerId: string;
}

interface JoinRoomResponse {
  success: boolean;
  playerId: string;
  players: { id: string; name: string; handCount: number; connected: boolean }[];
}

/**
 * Lobby Component
 *
 * 大廳頁面，讓玩家可以：
 * - 輸入名稱並建立新房間（取得 roomId 後可分享給其他玩家）
 * - 輸入 roomId 並以玩家名稱加入現有房間
 * 成功後導向 /game?roomId=...&playerId=...
 */
@Component({
  selector: 'app-lobby',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="lobby-page">
      <!-- Header -->
      <header class="lobby-header">
        <span class="text-4xl">🎲</span>
        <h1 class="text-3xl font-bold text-white">Havana</h1>
        <p class="text-slate-400 text-sm mt-1">Cuban Dominoes — 4 Players</p>
      </header>

      <div class="cards-grid">

        <!-- 建立房間 -->
        <div class="card">
          <h2 class="card-title">🏠 Create Room</h2>
          <p class="card-desc">Start a new game and share the Room ID with 3 friends.</p>

          <div class="form-field">
            <label class="label">Your Name</label>
            <input
              class="input"
              type="text"
              placeholder="e.g. Alice"
              maxlength="20"
              [(ngModel)]="createName"
              (keydown.enter)="onCreate()"
            />
          </div>

          @if (createdRoomId()) {
            <div class="room-id-box">
              <span class="text-slate-400 text-xs uppercase tracking-wider">Room ID</span>
              <span class="text-white font-mono text-lg font-bold">{{ createdRoomId() }}</span>
              <button class="btn-copy" (click)="copyRoomId()">
                {{ copied() ? '✓ Copied!' : 'Copy' }}
              </button>
            </div>
          }

          <button
            class="btn-primary"
            (click)="onCreate()"
            [disabled]="loading() || !createName.trim()"
          >
            @if (loading() && activeAction() === 'create') {
              <span class="spinner"></span> Creating...
            } @else {
              Create Room
            }
          </button>
        </div>

        <!-- 加入房間 -->
        <div class="card">
          <h2 class="card-title">🚪 Join Room</h2>
          <p class="card-desc">Enter the Room ID shared by the host and join the game.</p>

          <div class="form-field">
            <label class="label">Your Name</label>
            <input
              class="input"
              type="text"
              placeholder="e.g. Bob"
              maxlength="20"
              [(ngModel)]="joinName"
            />
          </div>

          <div class="form-field">
            <label class="label">Room ID</label>
            <input
              class="input font-mono"
              type="text"
              placeholder="Paste Room ID here"
              [(ngModel)]="joinRoomId"
              (keydown.enter)="onJoin()"
            />
          </div>

          <button
            class="btn-primary"
            (click)="onJoin()"
            [disabled]="loading() || !joinName.trim() || !joinRoomId.trim()"
          >
            @if (loading() && activeAction() === 'join') {
              <span class="spinner"></span> Joining...
            } @else {
              Join Room
            }
          </button>
        </div>

      </div>

      <!-- Error message -->
      @if (errorMsg()) {
        <div class="error-box">
          ⚠️ {{ errorMsg() }}
        </div>
      }

      <!-- How to play -->
      <div class="rules-box">
        <h3 class="text-white font-semibold mb-2">📖 How to Play</h3>
        <ul class="text-slate-400 text-sm space-y-1 list-disc list-inside">
          <li>4 players, double-nine set (55 tiles)</li>
          <li>Each player gets 10 tiles; 15 remain in the Boneyard</li>
          <li>The player holding [9|9] goes first with that tile</li>
          <li>Match the left or right end of the board</li>
          <li>Pass (Knock) when no tile can be played</li>
          <li>First to empty their hand wins; Trancado if nobody can play</li>
        </ul>
      </div>
    </div>
  `,
  styles: [`
    .lobby-page {
      @apply min-h-screen bg-slate-900 flex flex-col items-center
             px-4 py-10 gap-8;
    }

    .lobby-header {
      @apply flex flex-col items-center gap-1;
    }

    .cards-grid {
      @apply grid grid-cols-1 md:grid-cols-2 gap-6 w-full max-w-2xl;
    }

    .card {
      @apply bg-slate-800 rounded-2xl p-6 flex flex-col gap-4;
    }

    .card-title {
      @apply text-white text-xl font-bold;
    }

    .card-desc {
      @apply text-slate-400 text-sm;
    }

    .form-field {
      @apply flex flex-col gap-1;
    }

    .label {
      @apply text-slate-400 text-xs uppercase tracking-wider font-medium;
    }

    .input {
      @apply bg-slate-700 border border-slate-600 rounded-lg px-3 py-2
             text-white placeholder-slate-500 text-sm
             focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent
             transition-all;
    }

    .btn-primary {
      @apply bg-blue-600 hover:bg-blue-500 disabled:bg-slate-600 disabled:cursor-not-allowed
             text-white font-semibold px-4 py-2.5 rounded-xl
             transition-colors flex items-center justify-center gap-2;
    }

    .room-id-box {
      @apply bg-slate-700 rounded-lg px-4 py-3 flex items-center
             justify-between gap-3 border border-slate-600;
    }

    .btn-copy {
      @apply bg-slate-600 hover:bg-slate-500 text-white text-xs
             px-2 py-1 rounded transition-colors;
    }

    .error-box {
      @apply bg-red-900 border border-red-700 text-red-200
             px-4 py-3 rounded-xl text-sm max-w-2xl w-full;
    }

    .rules-box {
      @apply bg-slate-800 rounded-2xl p-6 w-full max-w-2xl;
    }

    .spinner {
      @apply inline-block w-4 h-4 border-2 border-white border-t-transparent
             rounded-full animate-spin;
    }
  `],
})
export class LobbyComponent {
  private http = inject(HttpClient);
  private router = inject(Router);

  createName = '';
  joinName = '';
  joinRoomId = '';

  loading = signal(false);
  activeAction = signal<'create' | 'join' | null>(null);
  errorMsg = signal<string | null>(null);
  createdRoomId = signal<string | null>(null);
  copied = signal(false);

  /** 建立房間 */
  onCreate(): void {
    const name = this.createName.trim();
    if (!name || this.loading()) return;

    this.loading.set(true);
    this.activeAction.set('create');
    this.errorMsg.set(null);

    this.http.post<CreateRoomResponse>(`${environment.apiBaseUrl}/rooms`, { hostName: name })
      .subscribe({
        next: res => {
          this.loading.set(false);
          this.createdRoomId.set(res.roomId);
          // 建立房間後直接進入遊戲
          this.router.navigate(['/game'], {
            queryParams: { roomId: res.roomId, playerId: res.hostPlayerId },
          });
        },
        error: err => {
          this.loading.set(false);
          this.errorMsg.set(err.error?.message ?? 'Failed to create room. Please try again.');
        },
      });
  }

  /** 加入房間 */
  onJoin(): void {
    const name = this.joinName.trim();
    const rid  = this.joinRoomId.trim();
    if (!name || !rid || this.loading()) return;

    this.loading.set(true);
    this.activeAction.set('join');
    this.errorMsg.set(null);

    this.http.post<JoinRoomResponse>(`${environment.apiBaseUrl}/rooms/${rid}/join`, { playerName: name })
      .subscribe({
        next: res => {
          this.loading.set(false);
          this.router.navigate(['/game'], {
            queryParams: { roomId: rid, playerId: res.playerId },
          });
        },
        error: err => {
          this.loading.set(false);
          const status = err.status;
          if (status === 404) {
            this.errorMsg.set('Room not found. Please check the Room ID.');
          } else if (status === 409) {
            this.errorMsg.set('Room is full (max 4 players).');
          } else {
            this.errorMsg.set(err.error?.message ?? 'Failed to join room. Please try again.');
          }
        },
      });
  }

  /** 複製 Room ID 到剪貼簿 */
  copyRoomId(): void {
    const id = this.createdRoomId();
    if (!id) return;
    navigator.clipboard.writeText(id).then(() => {
      this.copied.set(true);
      setTimeout(() => this.copied.set(false), 2000);
    });
  }
}
