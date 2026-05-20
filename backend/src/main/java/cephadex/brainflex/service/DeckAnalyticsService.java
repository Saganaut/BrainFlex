/**
 * Maintains the per-deck rolled-up analytics document.
 *
 * Sole writer of the {@code deck_analytics} collection. Called once per
 * finished session from {@link InteractiveSessionService} (after
 * {@code GameHistoryService.recordFinish}); the backfill migration replays
 * historical sessions through the same path.
 *
 * Updates are incremental: running averages use Welford's online formula
 * {@code new_avg = old_avg + (x - old_avg) / n} so the rollup is a true
 * source of truth — no per-read aggregation. Survey-only kinds skip
 * correct-count bookkeeping but still bucket the response distribution.
 *
 * <h3>Distribution semantics per element kind</h3>
 *
 * The {@code Map<String,Integer>} on {@link ElementStats} is a discriminated
 * union — the key/value meaning depends on the element kind:
 *
 * <ul>
 *   <li>MCQ, GRID, MATCHING, WORD_CLOUD, NUMBER, TEXT, PLACE_ON_IMAGE — <b>count
 *       style</b>: value is the hit count for that bucket.</li>
 *   <li>ALLOCATION, SCALES, RANKING — <b>sum style</b>: value is the running sum
 *       of the integer measurement across answers. The dashboard divides by
 *       {@code answeredCount} (or per-kind denominator) at read time to
 *       produce the average.</li>
 *   <li>DRAWING, Q_AND_A — empty distribution; only counts are tracked.</li>
 *   <li>SLIDE — skipped entirely; slides don't accept answers.</li>
 * </ul>
 *
 * <h3>Failure isolation</h3>
 *
 * {@link #recordGame} swallows all exceptions and logs them — a broken
 * analytics rollup must never break game-end. Game history's call already
 * runs synchronously and unguarded; this one is wrapped because the per-kind
 * bucketing surface area is larger and any new element kind we forget to
 * branch on would otherwise blow up the entire finish path.
 *
 * <h3>Backfill</h3>
 *
 * {@code DeckAnalyticsBackfillMigration} (gated on
 * {@code --migrate.deck-analytics=true}) wipes {@code deck_analytics} and
 * replays every finished session in chronological order so the rollup is
 * deterministic post-migration.
 */
package cephadex.brainflex.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import cephadex.brainflex.model.DeckAnalytics;
import cephadex.brainflex.model.ElementStats;
import cephadex.brainflex.model.InteractiveSession;
import cephadex.brainflex.model.InteractiveSessionPlayer;
import cephadex.brainflex.model.PlayerAnswer;
import cephadex.brainflex.model.answer.AllocationAnswer;
import cephadex.brainflex.model.answer.AnswerPayload;
import cephadex.brainflex.model.answer.DrawingAnswer;
import cephadex.brainflex.model.answer.GridAnswer;
import cephadex.brainflex.model.answer.MatchingAnswer;
import cephadex.brainflex.model.answer.McqAnswer;
import cephadex.brainflex.model.answer.NumberAnswer;
import cephadex.brainflex.model.answer.PlaceOnImageAnswer;
import cephadex.brainflex.model.answer.RankingAnswer;
import cephadex.brainflex.model.answer.ScalesAnswer;
import cephadex.brainflex.model.answer.TextAnswer;
import cephadex.brainflex.model.answer.TimeoutAnswer;
import cephadex.brainflex.model.answer.WordCloudAnswer;
import cephadex.brainflex.model.element.DeckElement;
import cephadex.brainflex.model.enums.ElementKind;
import cephadex.brainflex.repository.DeckAnalyticsRepository;
import cephadex.brainflex.repository.InteractiveSessionChatMessageRepository;
import cephadex.brainflex.repository.ReactionRepository;

@Service
public class DeckAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(DeckAnalyticsService.class);

    private static final int TEXT_DISTRIBUTION_CAP = 200;
    private static final int WORD_CLOUD_DISTRIBUTION_CAP = 500;
    private static final int PLACE_ON_IMAGE_GRID = 10;

    private final DeckAnalyticsRepository analyticsRepository;
    private final ReactionRepository reactionRepository;
    private final InteractiveSessionChatMessageRepository chatRepository;

    public DeckAnalyticsService(
            DeckAnalyticsRepository analyticsRepository,
            ReactionRepository reactionRepository,
            InteractiveSessionChatMessageRepository chatRepository) {
        this.analyticsRepository = analyticsRepository;
        this.reactionRepository = reactionRepository;
        this.chatRepository = chatRepository;
    }

    /**
     * Incorporates one finished session into the per-deck rollup. Idempotency
     * is not guaranteed — running this twice for the same session double-counts.
     * The caller (InteractiveSessionService.endGame) calls it exactly once;
     * the backfill resets the collection before replaying.
     */
    public void recordGame(InteractiveSession session) {
        if (session == null || session.getDeckId() == null) return;
        try {
            DeckAnalytics analytics = analyticsRepository.findById(session.getDeckId())
                    .orElseGet(() -> {
                        DeckAnalytics fresh = new DeckAnalytics();
                        fresh.setDeckId(session.getDeckId());
                        return fresh;
                    });

            applyGameLevelRollup(analytics, session);
            applyPerElementRollup(analytics, session);

            analytics.setUpdatedAt(LocalDateTime.now());
            analyticsRepository.save(analytics);
        } catch (Exception e) {
            // Swallow + log: a broken analytics call must not break game-end.
            log.warn("DeckAnalytics.recordGame failed for deck {} (session {}): {}",
                    session.getDeckId(), session.getId(), e.toString());
        }
    }

    /** Read-side accessor used by the controller; null when the deck has never been played. */
    public DeckAnalytics findByDeckId(String deckId) {
        return analyticsRepository.findById(deckId).orElse(null);
    }

    /** Wipes the collection — only invoked by the backfill migration. */
    public void deleteAll() {
        analyticsRepository.deleteAll();
    }

    private void applyGameLevelRollup(DeckAnalytics analytics, InteractiveSession session) {
        long durationMs = computeDurationMs(session);
        int newTotalPlays = analytics.getTotalPlays() + 1;
        analytics.setAverageDurationMs(incrementalAvgLong(
                analytics.getAverageDurationMs(), durationMs, newTotalPlays));
        analytics.setTotalPlays(newTotalPlays);

        List<InteractiveSessionPlayer> players = session.getPlayers() == null
                ? List.of() : session.getPlayers();
        for (InteractiveSessionPlayer p : players) {
            int newTotalPlayers = analytics.getTotalPlayers() + 1;
            analytics.setAverageScore(incrementalAvgDouble(
                    analytics.getAverageScore(), p.getScore(), newTotalPlayers));
            analytics.setAverageAccuracy(incrementalAvgDouble(
                    analytics.getAverageAccuracy(), p.getAccuracy(), newTotalPlayers));
            analytics.setTotalPlayers(newTotalPlayers);
        }

        LocalDateTime endedAt = session.getEndedAt() != null ? session.getEndedAt() : LocalDateTime.now();
        if (analytics.getLastPlayedAt() == null || endedAt.isAfter(analytics.getLastPlayedAt())) {
            analytics.setLastPlayedAt(endedAt);
        }
    }

    private void applyPerElementRollup(DeckAnalytics analytics, InteractiveSession session) {
        if (session.getDeckSnapshot() == null) return;
        Map<String, ElementStats> perElement = analytics.getPerElement();
        if (perElement == null) {
            perElement = new HashMap<>();
            analytics.setPerElement(perElement);
        }

        for (DeckElement element : session.getDeckSnapshot()) {
            if (element == null || element.kind() == ElementKind.SLIDE) continue;
            String elementId = element.id();
            if (elementId == null) continue;

            ElementStats stats = perElement.computeIfAbsent(elementId, k -> new ElementStats());
            stats.setPresentedCount(stats.getPresentedCount() + 1);

            for (InteractiveSessionPlayer player : session.getPlayers()) {
                if (player.getAnswers() == null) continue;
                for (PlayerAnswer answer : player.getAnswers()) {
                    if (!elementId.equals(answer.getElementId())) continue;
                    applyAnswer(element, stats, answer);
                }
            }

            long reactions = reactionRepository
                    .countByInteractiveSessionIdAndElementId(session.getId(), elementId);
            stats.setReactionsReceived(stats.getReactionsReceived() + (int) reactions);
            // chatMessagesDuringRound is deferred — chunk-11 ChatMessage has no
            // elementId and we don't retain per-round timestamps after a session
            // ends, so per-round attribution isn't reconstructible. We surface
            // total session chat at the deck level instead (see below).
        }

        // Add total session chat once to the first non-slide element as a
        // best-effort signal until chat gets per-round attribution. This keeps
        // the field non-zero in a defensible way without lying about which
        // round the chat happened in.
        long totalChat = chatRepository.countByInteractiveSessionId(session.getId());
        if (totalChat > 0) {
            for (DeckElement element : session.getDeckSnapshot()) {
                if (element == null || element.kind() == ElementKind.SLIDE) continue;
                ElementStats first = perElement.get(element.id());
                if (first != null) {
                    first.setChatMessagesDuringRound(
                            first.getChatMessagesDuringRound() + (int) totalChat);
                }
                break;
            }
        }
    }

    private void applyAnswer(DeckElement element, ElementStats stats, PlayerAnswer answer) {
        AnswerPayload payload = answer.getPayload();
        if (payload instanceof TimeoutAnswer || payload == null) {
            // Player didn't submit before the round ended — count nothing.
            return;
        }
        stats.setAnsweredCount(stats.getAnsweredCount() + 1);
        if (answer.isCorrect()) {
            stats.setCorrectCount(stats.getCorrectCount() + 1);
        }
        long timeMs = Math.max(0L, answer.getTimeTakenMs());
        stats.setTotalTimeMs(stats.getTotalTimeMs() + timeMs);
        if (stats.getAnsweredCount() > 0) {
            stats.setAverageTimeMs((double) stats.getTotalTimeMs() / stats.getAnsweredCount());
        }

        Map<String, Integer> dist = stats.getDistribution();
        switch (payload) {
            case McqAnswer mcq -> bucketMcq(dist, mcq);
            case NumberAnswer num -> bucketNumber(dist, num);
            case TextAnswer text -> bucketText(dist, text);
            case RankingAnswer rank -> bucketRanking(dist, rank);
            case ScalesAnswer scales -> bucketScales(dist, scales);
            case GridAnswer grid -> bucketGrid(dist, grid);
            case PlaceOnImageAnswer place -> bucketPlaceOnImage(dist, place);
            case WordCloudAnswer wc -> bucketWordCloud(dist, wc);
            case AllocationAnswer alloc -> bucketAllocation(dist, alloc);
            case MatchingAnswer match -> bucketMatching(dist, match);
            case DrawingAnswer drawing -> { /* survey only: no per-stroke bucketing */ }
            case TimeoutAnswer ignored -> { /* unreachable: handled above */ }
        }
    }

    // ── Per-kind distribution buckets ────────────────────────────────────

    private void bucketMcq(Map<String, Integer> dist, McqAnswer answer) {
        if (answer.optionIds() == null) return;
        for (String optionId : answer.optionIds()) {
            if (optionId == null) continue;
            dist.merge(optionId, 1, Integer::sum);
        }
    }

    /** Integer floor of the submitted value used as the bucket key. */
    private void bucketNumber(Map<String, Integer> dist, NumberAnswer answer) {
        long bucket = (long) Math.floor(answer.value());
        dist.merge(Long.toString(bucket), 1, Integer::sum);
    }

    private void bucketText(Map<String, Integer> dist, TextAnswer answer) {
        if (answer.text() == null) return;
        String normalized = answer.text().trim().toLowerCase();
        if (normalized.isEmpty()) return;
        if (!dist.containsKey(normalized) && dist.size() >= TEXT_DISTRIBUTION_CAP) {
            return;  // cap distribution growth; existing keys still increment
        }
        dist.merge(normalized, 1, Integer::sum);
    }

    /** Sum-style: distribution[itemId] += zero-based placement. */
    private void bucketRanking(Map<String, Integer> dist, RankingAnswer answer) {
        if (answer.orderedItemIds() == null) return;
        for (int i = 0; i < answer.orderedItemIds().size(); i++) {
            String itemId = answer.orderedItemIds().get(i);
            if (itemId == null) continue;
            dist.merge(itemId, i, Integer::sum);
        }
    }

    /** Sum-style: distribution[statementId] += chosen rating. */
    private void bucketScales(Map<String, Integer> dist, ScalesAnswer answer) {
        if (answer.ratings() == null) return;
        for (Map.Entry<String, Integer> e : answer.ratings().entrySet()) {
            if (e.getKey() == null || e.getValue() == null) continue;
            dist.merge(e.getKey(), e.getValue(), Integer::sum);
        }
    }

    private void bucketGrid(Map<String, Integer> dist, GridAnswer answer) {
        if (answer.selectedCellIndexes() == null) return;
        for (Integer cell : answer.selectedCellIndexes()) {
            if (cell == null) continue;
            dist.merge(Integer.toString(cell), 1, Integer::sum);
        }
    }

    /** Bucket by 10x10 grid cell of the normalized (x, y) coordinate. */
    private void bucketPlaceOnImage(Map<String, Integer> dist, PlaceOnImageAnswer answer) {
        int col = clamp((int) Math.floor(answer.x() * PLACE_ON_IMAGE_GRID), 0, PLACE_ON_IMAGE_GRID - 1);
        int row = clamp((int) Math.floor(answer.y() * PLACE_ON_IMAGE_GRID), 0, PLACE_ON_IMAGE_GRID - 1);
        String key = row + "," + col;
        dist.merge(key, 1, Integer::sum);
    }

    private void bucketWordCloud(Map<String, Integer> dist, WordCloudAnswer answer) {
        if (answer.words() == null) return;
        for (String word : answer.words()) {
            if (word == null) continue;
            String normalized = word.trim().toLowerCase();
            if (normalized.isEmpty()) continue;
            if (!dist.containsKey(normalized) && dist.size() >= WORD_CLOUD_DISTRIBUTION_CAP) continue;
            dist.merge(normalized, 1, Integer::sum);
        }
    }

    /** Sum-style: distribution[optionId] += points allocated. */
    private void bucketAllocation(Map<String, Integer> dist, AllocationAnswer answer) {
        if (answer.optionIdToPoints() == null) return;
        for (Map.Entry<String, Integer> e : answer.optionIdToPoints().entrySet()) {
            if (e.getKey() == null || e.getValue() == null) continue;
            dist.merge(e.getKey(), e.getValue(), Integer::sum);
        }
    }

    /** Bucket by "leftId>rightId" pair string so the dashboard can see actual pair frequency. */
    private void bucketMatching(Map<String, Integer> dist, MatchingAnswer answer) {
        if (answer.leftIdToRightId() == null) return;
        for (Map.Entry<String, String> e : answer.leftIdToRightId().entrySet()) {
            if (e.getKey() == null || e.getValue() == null) continue;
            String key = e.getKey() + ">" + e.getValue();
            dist.merge(key, 1, Integer::sum);
        }
    }

    // ── Math helpers ────────────────────────────────────────────────────

    private static double incrementalAvgDouble(double oldAvg, double x, int newCount) {
        if (newCount <= 0) return oldAvg;
        return oldAvg + (x - oldAvg) / newCount;
    }

    private static long incrementalAvgLong(long oldAvg, long x, int newCount) {
        if (newCount <= 0) return oldAvg;
        return oldAvg + (x - oldAvg) / newCount;
    }

    private static long computeDurationMs(InteractiveSession session) {
        if (session.getStartedAt() == null || session.getEndedAt() == null) return 0L;
        return Duration.between(session.getStartedAt(), session.getEndedAt()).toMillis();
    }

    private static int clamp(int value, int lo, int hi) {
        return Math.max(lo, Math.min(hi, value));
    }
}
