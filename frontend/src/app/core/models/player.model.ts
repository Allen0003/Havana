/**
 * 玩家模型（廣播用，不含手牌牌面）
 */
export interface Player {
  id: string;
  name: string;
  handCount: number;
  connected: boolean;
}
