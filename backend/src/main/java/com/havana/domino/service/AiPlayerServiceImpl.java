package com.havana.domino.service;

import com.havana.domino.model.Board;
import com.havana.domino.model.BoardEnd;
import com.havana.domino.model.GameRoom;
import com.havana.domino.model.Player;
import com.havana.domino.model.Tile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * AI 玩家決策服務實作。
 *
 * <h3>策略說明（Greedy + Defensive）</h3>
 * <ol>
 *   <li><strong>空桌</strong>：第一張必須是 [9|9]，直接出。</li>
 *   <li><strong>非空桌</strong>：從所有合法出牌中依序套用以下優先規則：
 *     <ol type="a">
 *       <li>優先出「對子」（double），避免卡在手上無法利用。</li>
 *       <li>其次選「點數最高」的牌（Greedy：儘快減少手牌總點數，有利死局計分）。</li>
 *       <li>出牌端優先選「RIGHT」（保持桌面延伸方向一致，減少對手可用端）。</li>
 *     </ol>
 *   </li>
 *   <li><strong>無合法出牌</strong>：過牌（Pass）。</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiPlayerServiceImpl implements AiPlayerService {

    private final GameEngineService gameEngineService;

    // ── selectMove() ─────────────────────────────────────────────────────────

    @Override
    public Optional<AiMove> selectMove(GameRoom room, Player player) {
        Board board = room.getBoard();
        List<Tile> hand = player.getHand();

        // 空桌：必須出 [9|9]
        if (board.isEmpty()) {
            return hand.stream()
                    .filter(t -> t.left() == 9 && t.right() == 9)
                    .findFirst()
                    .map(t -> new AiMove(t.id(), BoardEnd.LEFT));
        }

        int leftEnd  = board.getLeftEnd();
        int rightEnd = board.getRightEnd();

        // 收集所有合法出牌候選（牌 + 目標端）
        record Candidate(Tile tile, BoardEnd end) {}

        List<Candidate> candidates = hand.stream()
                .flatMap(tile -> {
                    java.util.stream.Stream.Builder<Candidate> sb = java.util.stream.Stream.builder();
                    if (tile.canMatch(leftEnd))  sb.accept(new Candidate(tile, BoardEnd.LEFT));
                    if (tile.canMatch(rightEnd)) sb.accept(new Candidate(tile, BoardEnd.RIGHT));
                    return sb.build();
                })
                // 去重：同一張牌對子（leftEnd == rightEnd）只保留 RIGHT
                .filter(c -> !(c.tile().isDouble()
                              && leftEnd == rightEnd
                              && c.end() == BoardEnd.LEFT))
                .toList();

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        // 策略排序：
        //   1. 對子優先（isDouble() desc）
        //   2. 點數高的牌優先（totalPips() desc）
        //   3. RIGHT 端優先
        Candidate best = candidates.stream()
                .max(Comparator
                        .comparingInt((Candidate c) -> c.tile().isDouble() ? 1 : 0)
                        .thenComparingInt(c -> c.tile().totalPips())
                        .thenComparingInt(c -> c.end() == BoardEnd.RIGHT ? 1 : 0))
                .orElseThrow();

        log.debug("AI 選擇出牌：tile={}, end={}", best.tile(), best.end());
        return Optional.of(new AiMove(best.tile().id(), best.end()));
    }

    // ── executeAction() ──────────────────────────────────────────────────────

    @Override
    public GameRoom executeAction(GameRoom room, String playerId) {
        Player aiPlayer = room.findPlayer(playerId);
        Optional<AiMove> move = selectMove(room, aiPlayer);

        if (move.isPresent()) {
            AiMove m = move.get();
            log.info("AI [{}] 出牌：tileId={}, end={}", aiPlayer.getName(), m.tileId(), m.targetEnd());
            return gameEngineService.playTile(room, playerId, m.tileId(), m.targetEnd());
        } else {
            log.info("AI [{}] 過牌", aiPlayer.getName());
            return gameEngineService.passTurn(room, playerId);
        }
    }
}
