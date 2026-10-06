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
  players: { id: string; name: string; handCount: number; connected: boolean; ai: boolean }[];
}

/**
 * Lobby Component
 *
 * 大廳頁面，讓玩家可以：
 * - 一鍵開啟單人對戰（VS AI，立即開始）
 * - 輸入名稱並建立多人房間（取得 roomId 後可分享）
 * - 輸入 roomId 並以玩家名稱加入現有房間
 * 成功後導向 /game?roomId=...&playerId=...
 */
@Component({
  selector: 'app-lobby',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './lobby.html',
  styleUrls: ['./lobby.css']
})
export class LobbyComponent {
  private http = inject(HttpClient);
  private router = inject(Router);

  soloName = '';
  createName = '';
  joinName = '';
  joinRoomId = '';

  loading = signal(false);
  activeAction = signal<'solo' | 'create' | 'join' | null>(null);
  errorMsg = signal<string | null>(null);
  createdRoomId = signal<string | null>(null);
  copied = signal(false);

  /** 單人 VS AI 對戰 */
  onSolo(): void {
    const name = this.soloName.trim();
    if (!name || this.loading()) return;

    this.loading.set(true);
    this.activeAction.set('solo');
    this.errorMsg.set(null);

    this.http.post<CreateRoomResponse>(
      `${environment.apiBaseUrl}/rooms/solo`, { hostName: name }
    ).subscribe({
      next: res => {
        this.loading.set(false);
        this.router.navigate(['/game'], {
          queryParams: { roomId: res.roomId, playerId: res.hostPlayerId },
        });
      },
      error: err => {
        this.loading.set(false);
        this.errorMsg.set(err.error?.message ?? 'Failed to start solo game. Please try again.');
      },
    });
  }

  /** 建立多人房間 */
  onCreate(): void {
    const name = this.createName.trim();
    if (!name || this.loading()) return;

    this.loading.set(true);
    this.activeAction.set('create');
    this.errorMsg.set(null);

    this.http.post<CreateRoomResponse>(
      `${environment.apiBaseUrl}/rooms`, { hostName: name }
    ).subscribe({
      next: res => {
        this.loading.set(false);
        this.createdRoomId.set(res.roomId);
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

  /** 加入現有房間 */
  onJoin(): void {
    const name = this.joinName.trim();
    const rid  = this.joinRoomId.trim();
    if (!name || !rid || this.loading()) return;

    this.loading.set(true);
    this.activeAction.set('join');
    this.errorMsg.set(null);

    this.http.post<JoinRoomResponse>(
      `${environment.apiBaseUrl}/rooms/${rid}/join`, { playerName: name }
    ).subscribe({
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
