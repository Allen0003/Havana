/**
 * 桌面延伸方向
 */
export enum BoardEnd {
  LEFT = 'LEFT',
  RIGHT = 'RIGHT',
}

/**
 * 出牌請求
 */
export interface PlayMoveRequest {
  roomId: string;
  playerId: string;
  tileId: number;
  targetEnd: BoardEnd;
}

/**
 * 過牌請求
 */
export interface PassMoveRequest {
  roomId: string;
  playerId: string;
}

/**
 * WebSocket 錯誤訊息
 */
export interface WebSocketError {
  error: string;
  message: string;
}
