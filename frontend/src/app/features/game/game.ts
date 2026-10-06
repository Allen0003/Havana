import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { Subscription, switchMap } from 'rxjs';

import { BoardComponent } from './board/board';
import { HandComponent } from './hand/hand';
import { ScoreComponent } from './score/score';

import { WebSocketService } from '../../core/services/web-socket.service';
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
import { GameRoom, GameStatus } from '../../core/models/game-room.model';
import { Tile } from '../../core/models/tile.model';
import { environment } from '../../../environments/environment';

/**
 * Game Page Component
 *
 * 主要遊戲畫面，負責：
 * 1. 從路由參數取得 roomId / playerId
 * 2. 進入頁面後立即 REST fetch 初始房間狀態 + 個人手牌
 * 3. 建立 WebSocket 連線並訂閱房間更新
 * 4. 每次收到廣播後 re-fetch 手牌（出牌後手牌會減少）
 * 5. 組合 board/hand/score 子元件
 */
@Component({
  selector: 'app-game',
  standalone: true,
  imports: [CommonModule, BoardComponent, HandComponent, ScoreComponent],
  templateUrl: './game.html',
  styleUrls: ['./game.css']
})
export class GameComponent implements OnInit, OnDestroy {
  private ws   = inject(WebSocketService);
  private http = inject(HttpClient);
  private route  = inject(ActivatedRoute);
  private router = inject(Router);

  readonly errorMessage  = errorMessage;
  readonly isGameFinished = isGameFinished;
  readonly isTrancado    = isTrancado;
  readonly trancadoScores = trancadoScores;
  readonly winnerId = winnerId;
  readonly players  = players;

  currentRoomId = '';
  private playerId  = '';
  private subs = new Subscription();

  ngOnInit(): void {
    const params   = this.route.snapshot.queryParamMap;
    const roomId   = params.get('roomId')   ?? '';
    const playerId = params.get('playerId') ?? '';

    if (!roomId || !playerId) {
      this.router.navigate(['/']);
      return;
    }

    this.currentRoomId = roomId;
    this.playerId      = playerId;
    localPlayerId.set(playerId);

    // ── 1. 立即 fetch 初始房間狀態（Board 初始化） ─────────────────────────
    this.fetchRoomState(roomId);

    // ── 2. 立即 fetch 個人手牌（Your Hand 初始化） ─────────────────────────
    this.fetchHand(roomId, playerId);

    // ── 3. 訂閱 WebSocket 廣播：更新房間狀態 + re-fetch 手牌 ───────────────
    this.subs.add(
      this.ws.getGameRoomUpdates().subscribe(room => {
        gameRoomSignal.set(room);
        // 每次盤面更新後，同步取得最新手牌
        // （廣播 DTO 不含手牌牌面，需透過 REST 取得）
        if (room.status !== GameStatus.FINISHED) {
          this.fetchHand(room.roomId, playerId);
        }
      })
    );

    // ── 4. 訂閱連線狀態 ────────────────────────────────────────────────────
    this.subs.add(
      this.ws.getConnectionStatus().subscribe(status => {
        connectionStatus.set(status as any);
      })
    );

    // ── 5. 訂閱 WebSocket 錯誤 ─────────────────────────────────────────────
    this.subs.add(
      this.ws.getErrors().subscribe(err => {
        errorMessage.set(err.message);
        setTimeout(() => errorMessage.set(null), 3000);
      })
    );

    // ── 6. 建立 WebSocket 連線 ─────────────────────────────────────────────
    this.ws.connect(roomId);
  }

  ngOnDestroy(): void {
    this.subs.unsubscribe();
    this.ws.disconnect();
    gameRoomSignal.set(null);
    myHandSignal.set([]);
    localPlayerId.set(null);
    errorMessage.set(null);
  }

  // ── 私有輔助方法 ───────────────────────────────────────────────────────────

  /** 呼叫 GET /api/rooms/{roomId} 取得房間快照，寫入 gameRoomSignal */
  private fetchRoomState(roomId: string): void {
    this.http.get<GameRoom>(`${environment.apiBaseUrl}/rooms/${roomId}`)
      .subscribe({
        next: room => gameRoomSignal.set(room),
        error: err => console.error('Failed to fetch room state:', err),
      });
  }

  /** 呼叫 GET /api/rooms/{roomId}/hand?playerId=... 取得手牌，寫入 myHandSignal */
  private fetchHand(roomId: string, playerId: string): void {
    this.http
      .get<Tile[]>(`${environment.apiBaseUrl}/rooms/${roomId}/hand`, {
        params: { playerId },
      })
      .subscribe({
        next: hand => myHandSignal.set(hand),
        error: err => console.error('Failed to fetch hand:', err),
      });
  }

  // ── 公開方法 ──────────────────────────────────────────────────────────────

  clearError(): void {
    errorMessage.set(null);
  }

  onLeave(): void {
    this.ws.disconnect();
    this.router.navigate(['/']);
  }

  winnerName(): string | null {
    const id = winnerId();
    if (!id) return null;
    return players().find(p => p.id === id)?.name ?? null;
  }
}
