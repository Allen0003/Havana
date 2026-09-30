package com.havana.domino.service;

import com.havana.domino.model.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * {@link GameEngineService} 核心實作。
 *
 * <ul>
 *   <li>M1-3：{@link #generateTileSet()}</li>
 *   <li>M1-4：{@link #initGame(GameRoom)}</li>
 *   <li>M1-5：{@link #playTile(GameRoom, String, int, BoardEnd)}</li>
 *   <li>M1-6：{@link #passTurn(GameRoom, String)}</li>
 *   <li>M1-7：{@link #checkWinner(GameRoom)}</li>
 *   <li>M1-8：{@link #isTrancado(GameRoom)}, {@link #calculateTrancadoScores(GameRoom)}</li>
 * </ul>
 */
@Service
public class GameEngineServiceImpl implements GameEngineService {

    private static final int MAX_PIP          = 9;
    private static final int PLAYERS          = 4;
    private static final int TILES_PER_PLAYER = 10;

    // ── M1-3：generateTileSet() ──────────────────────────────────────────────

    @Override
    public List<Tile> generateTileSet() {
        List<Tile> tiles = new ArrayList<>(55);
        int id = 0;
        for (int left = 0; left <= MAX_PIP; left++) {
            for (int right = left; right <= MAX_PIP; right++) {
                tiles.add(new Tile(id++, left, right));
            }
        }
        return Collections.unmodifiableList(tiles);
    }

    // ── M1-4：initGame() ─────────────────────────────────────────────────────

    @Override
    public GameRoom initGame(GameRoom room) {
        // 先取出 [9|9]，保證首出玩家一定持有；其餘 54 張隨機洗牌
        List<Tile> allTiles = new ArrayList<>(generateTileSet());
        Tile doubleNine = allTiles.stream()
                .filter(t -> t.left() == 9 && t.right() == 9)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("[9|9] not found in tile set"));
        allTiles.remove(doubleNine);
        Collections.shuffle(allTiles);

        List<Player> players     = room.getPlayers();
        int firstPlayerIndex     = new Random().nextInt(players.size());
        int deckIdx = 0;

        for (int i = 0; i < players.size(); i++) {
            int count = (i == firstPlayerIndex) ? TILES_PER_PLAYER - 1 : TILES_PER_PLAYER;
            List<Tile> hand = new ArrayList<>(allTiles.subList(deckIdx, deckIdx + count));
            if (i == firstPlayerIndex) {
                hand.add(doubleNine);
            }
            players.get(i).setHand(hand);
            deckIdx += count;
        }

        room.setBoneyard(new ArrayList<>(allTiles.subList(deckIdx, allTiles.size())));
        room.setCurrentTurnIndex(firstPlayerIndex);
        room.setStatus(GameStatus.IN_PROGRESS);
        return room;
    }

    // ── M1-5：playTile() ─────────────────────────────────────────────────────

    /**
     * 驗證並執行出牌。
     *
     * <p>驗證規則：
     * <ol>
     *   <li>playerId 必須是當前輪次玩家</li>
     *   <li>tileId 必須在該玩家手牌中</li>
     *   <li>空桌時：第一張牌必須是 [9|9]</li>
     *   <li>非空桌：牌的 targetEnd 側點數必須匹配桌面對應端點數</li>
     * </ol>
     *
     * <p>出牌成功後：
     * <ul>
     *   <li>從玩家手牌移除該牌</li>
     *   <li>更新桌面雙端佇列與 leftEnd/rightEnd</li>
     *   <li>推進至下一玩家</li>
     * </ul>
     *
     * @throws IllegalArgumentException 驗證失敗（非輪到此玩家、牌不在手牌、點數不匹配）
     */
    @Override
    public GameRoom playTile(GameRoom room, String playerId, int tileId, BoardEnd targetEnd) {
        // 1. 驗證輪次
        Player current = room.currentPlayer();
        if (!current.getId().equals(playerId)) {
            throw new IllegalArgumentException(
                "NOT_YOUR_TURN: Expected %s but got %s".formatted(current.getId(), playerId));
        }

        // 2. 確認手牌中有此 tileId
        Tile tile = current.getHand().stream()
                .filter(t -> t.id() == tileId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                    "TILE_NOT_IN_HAND: tileId=%d not found in player %s's hand"
                        .formatted(tileId, playerId)));

        Board board = room.getBoard();

        if (board.isEmpty()) {
            // 3a. 空桌：必須是 [9|9]
            if (tile.left() != 9 || tile.right() != 9) {
                throw new IllegalArgumentException(
                    "INVALID_FIRST_MOVE: First tile must be [9|9], got " + tile);
            }
            board.getTiles().addLast(tile);
            board.setLeftEnd(9);
            board.setRightEnd(9);
        } else {
            // 3b. 非空桌：驗證點數匹配並決定如何放置
            if (targetEnd == BoardEnd.LEFT) {
                int requiredPip = board.getLeftEnd();
                if (!tile.canMatch(requiredPip)) {
                    throw new IllegalArgumentException(
                        "INVALID_MOVE: Tile %s cannot match left end [%d]"
                            .formatted(tile, requiredPip));
                }
                // 若牌的 right 端匹配桌面左端，需翻轉擺放（left 端朝外）
                // 不論如何，新的 leftEnd 是牌的「另一端」
                board.getTiles().addFirst(tile);
                board.setLeftEnd(tile.matchingEnd(requiredPip));
            } else {
                int requiredPip = board.getRightEnd();
                if (!tile.canMatch(requiredPip)) {
                    throw new IllegalArgumentException(
                        "INVALID_MOVE: Tile %s cannot match right end [%d]"
                            .formatted(tile, requiredPip));
                }
                board.getTiles().addLast(tile);
                board.setRightEnd(tile.matchingEnd(requiredPip));
            }
        }

        // 4. 從手牌移除
        current.getHand().removeIf(t -> t.id() == tileId);

        // 5. 推進回合
        room.advanceTurn();
        return room;
    }

    // ── M1-6：passTurn() ─────────────────────────────────────────────────────

    /**
     * 驗證並執行過牌（Pass / Knock）。
     *
     * <p>只有當玩家手牌中沒有任何可匹配桌面兩端的骨牌時，才允許過牌。
     * 若玩家還有合法出牌，拋出 {@link IllegalStateException}。
     *
     * @throws IllegalArgumentException 非該玩家輪次
     * @throws IllegalStateException    玩家有牌可出，不能過牌
     */
    @Override
    public GameRoom passTurn(GameRoom room, String playerId) {
        // 1. 驗證輪次
        Player current = room.currentPlayer();
        if (!current.getId().equals(playerId)) {
            throw new IllegalArgumentException(
                "NOT_YOUR_TURN: Expected %s but got %s".formatted(current.getId(), playerId));
        }

        // 2. 確認確實無牌可出
        if (canPlay(current, room.getBoard())) {
            throw new IllegalStateException(
                "CAN_PLAY: Player %s still has playable tiles".formatted(playerId));
        }

        // 3. 推進回合
        room.advanceTurn();
        return room;
    }

    // ── M1-7：checkWinner() ──────────────────────────────────────────────────

    /**
     * 檢查是否有玩家手牌清空（獲勝條件）。
     * 回傳第一位手牌為空的玩家；若無人獲勝回傳 {@link Optional#empty()}。
     */
    @Override
    public Optional<Player> checkWinner(GameRoom room) {
        return room.getPlayers().stream()
                .filter(p -> p.getHand().isEmpty())
                .findFirst();
    }

    // ── M1-8：isTrancado() ───────────────────────────────────────────────────

    /**
     * 判斷是否發生死局（Trancado）。
     *
     * <p>死局條件：所有玩家手牌非空，且無任何玩家能出牌。
     * 即：每位玩家的手牌中，沒有任何一張可匹配桌面左端或右端。
     */
    @Override
    public boolean isTrancado(GameRoom room) {
        // 桌面為空時不可能是死局（還沒開始）
        if (room.getBoard().isEmpty()) {
            return false;
        }
        // 若有任何玩家手牌已空，表示有人贏了，不是死局
        boolean anyWinner = room.getPlayers().stream()
                .anyMatch(p -> p.getHand().isEmpty());
        if (anyWinner) {
            return false;
        }
        // 所有玩家都無法出牌 → 死局
        return room.getPlayers().stream()
                .noneMatch(p -> canPlay(p, room.getBoard()));
    }

    // ── M1-8：calculateTrancadoScores() ──────────────────────────────────────

    /**
     * 計算死局各玩家剩餘手牌點數。
     *
     * @return Map&lt;playerId, 剩餘點數總和&gt;，按點數由低到高排序
     */
    @Override
    public Map<String, Integer> calculateTrancadoScores(GameRoom room) {
        return room.getPlayers().stream()
                .collect(Collectors.toMap(
                        Player::getId,
                        Player::handPipTotal,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    // ── 私有輔助方法 ──────────────────────────────────────────────────────────

    /**
     * 判斷某玩家是否在現有桌面狀態下有合法出牌。
     * 空桌時：只有 [9|9] 是合法首張（通常是 initGame 後的第一手）。
     * 非空桌：任一手牌可匹配桌面左端或右端即為可出。
     */
    private boolean canPlay(Player player, Board board) {
        if (board.isEmpty()) {
            return player.getHand().stream()
                    .anyMatch(t -> t.left() == 9 && t.right() == 9);
        }
        int left  = board.getLeftEnd();
        int right = board.getRightEnd();
        return player.getHand().stream()
                .anyMatch(t -> t.canMatch(left) || t.canMatch(right));
    }
}
