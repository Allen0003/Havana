import { Injectable } from '@angular/core';
import { Client, IMessage, StompConfig } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { environment } from '../../../environments/environment';
import { GameRoom } from '../models/game-room.model';
import {
  PlayMoveRequest,
  PassMoveRequest,
  WebSocketError,
} from '../models/websocket-messages.model';

/**
 * 連線狀態枚舉
 */
export enum ConnectionStatus {
  DISCONNECTED = 'DISCONNECTED',
  CONNECTING = 'CONNECTING',
  CONNECTED = 'CONNECTED',
}

/**
 * WebSocket 服務
 *
 * 提供 STOMP over SockJS 連線管理、訂閱房間更新、發送遊戲操作。
 * 包含自動重連機制（指數退避，最多 5 次）。
 */
@Injectable({
  providedIn: 'root',
})
export class WebSocketService {
  private stompClient: Client | null = null;
  private reconnectAttempts = 0;
  private readonly maxReconnectAttempts = 5;
  private reconnectTimeoutId: any = null;

  // Observable 狀態
  private connectionStatus$ = new BehaviorSubject<ConnectionStatus>(
    ConnectionStatus.DISCONNECTED
  );
  private gameRoomUpdate$ = new Subject<GameRoom>();
  private error$ = new Subject<WebSocketError>();

  constructor() {}

  /**
   * 取得連線狀態 Observable
   */
  getConnectionStatus(): Observable<ConnectionStatus> {
    return this.connectionStatus$.asObservable();
  }

  /**
   * 取得房間更新 Observable
   */
  getGameRoomUpdates(): Observable<GameRoom> {
    return this.gameRoomUpdate$.asObservable();
  }

  /**
   * 取得錯誤訊息 Observable
   */
  getErrors(): Observable<WebSocketError> {
    return this.error$.asObservable();
  }

  /**
   * 建立 WebSocket 連線並訂閱指定房間
   */
  connect(roomId: string): void {
    if (this.stompClient?.connected) {
      console.warn('WebSocket 已連線，先斷線再重新連線');
      this.disconnect();
    }

    this.connectionStatus$.next(ConnectionStatus.CONNECTING);

    const stompConfig: StompConfig = {
      // 使用 SockJS 作為 WebSocket fallback
      webSocketFactory: () => new SockJS(environment.wsEndpoint) as any,

      // 連線成功回調
      onConnect: () => {
        console.log('WebSocket 連線成功');
        this.connectionStatus$.next(ConnectionStatus.CONNECTED);
        this.reconnectAttempts = 0;

        // 訂閱房間更新
        this.subscribeToRoom(roomId);
      },

      // 連線錯誤回調
      onStompError: (frame) => {
        console.error('STOMP 錯誤:', frame);
        this.handleConnectionError();
      },

      // 連線關閉回調
      onWebSocketClose: () => {
        console.warn('WebSocket 連線關閉');
        this.connectionStatus$.next(ConnectionStatus.DISCONNECTED);
        this.attemptReconnect(roomId);
      },

      // 除錯模式（生產環境應關閉）
      debug: (msg: string) => {
        if (!environment.production) {
          console.log('STOMP Debug:', msg);
        }
      },

      // 心跳設定（毫秒）
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,

      // 重連延遲（由我們自行處理）
      reconnectDelay: 0,
    };

    this.stompClient = new Client(stompConfig);
    this.stompClient.activate();
  }

  /**
   * 訂閱指定房間的更新
   */
  private subscribeToRoom(roomId: string): void {
    if (!this.stompClient?.connected) {
      console.error('無法訂閱：WebSocket 未連線');
      return;
    }

    const topic = `/topic/room/${roomId}`;
    this.stompClient.subscribe(topic, (message: IMessage) => {
      try {
        const payload = JSON.parse(message.body);

        // 判斷是錯誤訊息還是房間更新
        if (payload.error) {
          this.error$.next(payload as WebSocketError);
        } else {
          this.gameRoomUpdate$.next(payload as GameRoom);
        }
      } catch (error) {
        console.error('解析 WebSocket 訊息失敗:', error);
      }
    });

    console.log(`已訂閱房間: ${topic}`);
  }

  /**
   * 斷線重連機制（指數退避）
   */
  private attemptReconnect(roomId: string): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('達到最大重連次數，停止重連');
      this.error$.next({
        error: 'CONNECTION_FAILED',
        message: '無法連線至伺服器，請稍後再試',
      });
      return;
    }

    // 指數退避：2^n 秒（1, 2, 4, 8, 16）
    const delay = Math.pow(2, this.reconnectAttempts) * 1000;
    this.reconnectAttempts++;

    console.log(`${delay / 1000} 秒後嘗試重連（第 ${this.reconnectAttempts} 次）`);

    this.reconnectTimeoutId = setTimeout(() => {
      console.log('嘗試重新連線...');
      this.connect(roomId);
    }, delay);
  }

  /**
   * 處理連線錯誤
   */
  private handleConnectionError(): void {
    this.connectionStatus$.next(ConnectionStatus.DISCONNECTED);
    this.error$.next({
      error: 'CONNECTION_ERROR',
      message: 'WebSocket 連線發生錯誤',
    });
  }

  /**
   * 斷開 WebSocket 連線
   */
  disconnect(): void {
    if (this.reconnectTimeoutId) {
      clearTimeout(this.reconnectTimeoutId);
      this.reconnectTimeoutId = null;
    }

    if (this.stompClient) {
      this.stompClient.deactivate();
      this.stompClient = null;
    }

    this.connectionStatus$.next(ConnectionStatus.DISCONNECTED);
    this.reconnectAttempts = 0;
    console.log('WebSocket 已斷線');
  }

  /**
   * 發送出牌請求
   */
  playTile(request: PlayMoveRequest): void {
    if (!this.stompClient?.connected) {
      console.error('無法出牌：WebSocket 未連線');
      this.error$.next({
        error: 'NOT_CONNECTED',
        message: '未連線至伺服器',
      });
      return;
    }

    this.stompClient.publish({
      destination: '/app/game.play',
      body: JSON.stringify(request),
    });

    console.log('已發送出牌請求:', request);
  }

  /**
   * 發送過牌請求
   */
  passTurn(request: PassMoveRequest): void {
    if (!this.stompClient?.connected) {
      console.error('無法過牌：WebSocket 未連線');
      this.error$.next({
        error: 'NOT_CONNECTED',
        message: '未連線至伺服器',
      });
      return;
    }

    this.stompClient.publish({
      destination: '/app/game.pass',
      body: JSON.stringify(request),
    });

    console.log('已發送過牌請求:', request);
  }

  /**
   * 檢查是否已連線
   */
  isConnected(): boolean {
    return this.stompClient?.connected ?? false;
  }
}
