/**
 * Business logic for the per-user game-history index.
 *
 * Writes one {@link GameHistoryEntry} per (user, session) at game end:
 *   - each finishing player gets a row (including guests);
 *   - if the host was also one of those players, that row carries
 *     {@code wasHost=true};
 *   - if the host did not play, a standalone host row is written so the
 *     "games I ran" view still surfaces it.
 *
 * The {@code (userId, interactiveSessionId)} unique index makes
 * {@link #recordFinish(InteractiveSession, java.util.List)} idempotent —
 * replays from the backfill or a retried finish path are swallowed.
 *
 * Also bumps {@code User.membership.monthlyInteractiveSessionCount} for the
 * host so the Membership quota dashboard reflects the new session without a
 * cross-collection aggregation. The counter auto-rolls when the period start
 * lands in a previous calendar month.
 */
package cephadex.brainflex.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import cephadex.brainflex.model.Deck;
import cephadex.brainflex.model.GameHistoryEntry;
import cephadex.brainflex.model.InteractiveSession;
import cephadex.brainflex.model.InteractiveSessionPlayer;
import cephadex.brainflex.model.Membership;
import cephadex.brainflex.model.PlayerPlacement;
import cephadex.brainflex.model.Team;
import cephadex.brainflex.model.User;
import cephadex.brainflex.model.UserSnapshot;
import cephadex.brainflex.model.enums.AchievementTrigger;
import cephadex.brainflex.repository.DeckRepository;
import cephadex.brainflex.repository.GameHistoryRepository;
import cephadex.brainflex.repository.UserRepository;

@Service
public class GameHistoryService {

    private final GameHistoryRepository historyRepository;
    private final DeckRepository deckRepository;
    private final UserRepository userRepository;
    private final AchievementService achievementService;

    public GameHistoryService(
            GameHistoryRepository historyRepository,
            DeckRepository deckRepository,
            UserRepository userRepository,
            AchievementService achievementService) {
        this.historyRepository = historyRepository;
        this.deckRepository = deckRepository;
        this.userRepository = userRepository;
        this.achievementService = achievementService;
    }

    /**
     * Writes the per-player + (optional) host-only rows for a finished session
     * and bumps the host's monthly Membership counter. Safe to call more than
     * once — the unique index swallows duplicates.
     */
    public void recordFinish(InteractiveSession session, List<PlayerPlacement> placements) {
        if (session == null) return;

        String deckName = deckRepository.findById(session.getDeckId())
                .map(Deck::getName)
                .orElse(null);
        long durationMs = computeDurationMs(session);
        LocalDateTime playedAt = session.getEndedAt() != null
                ? session.getEndedAt()
                : LocalDateTime.now();
        boolean hostPlayed = false;
        boolean hostRowInserted = false;

        for (PlayerPlacement p : placements) {
            boolean isHost = session.getHostUserId() != null
                    && session.getHostUserId().equals(p.getUserId());
            if (isHost) hostPlayed = true;
            boolean inserted = writeEntry(buildPlayerEntry(session, p, deckName, durationMs, playedAt, isHost));
            if (isHost) hostRowInserted = inserted;
            // Chunk 17 — fire-and-forget achievement evaluation. Only runs on
            // a true insert so a replayed finish doesn't double-evaluate.
            if (inserted && !p.isGuest()) {
                evaluatePlayerAchievements(session, p, isHost);
            }
        }

        if (!hostPlayed && session.getHostUserId() != null) {
            hostRowInserted = writeEntry(buildHostOnlyEntry(session, deckName, durationMs, playedAt));
            // Standalone host row — only the host-side triggers apply.
            if (hostRowInserted) {
                evaluateHostOnlyAchievements(session);
            }
        }

        // Bump the Membership counter only when the host's row was actually
        // newly inserted. A retried finish (duplicate-key) leaves the counter
        // alone, and a backfill of a session the host has already been
        // credited for is a no-op.
        if (hostRowInserted) {
            bumpHostMonthlyCounter(session.getHostUserId(), playedAt);
        }
    }

    /**
     * Evaluates every gameplay-driven trigger for a single player who just
     * finished a game. The achievement service is fire-and-forget — any
     * failure here is logged and swallowed so a broken trigger can't break
     * the {@code endGame} write path.
     *
     * {@code TOTAL_POINTS} is intentionally NOT evaluated here — it lives in
     * {@code InteractiveSessionService.updateStatsAfterGame} where the User
     * doc is already loaded for the points roll-up. Doing it twice would
     * waste a Mongo round-trip; doing it here would also miss the
     * just-bumped value because updateStatsAfterGame runs after recordFinish.
     */
    private void evaluatePlayerAchievements(
            InteractiveSession session, PlayerPlacement p, boolean isHost) {
        String userId = p.getUserId();
        if (userId == null || userId.isBlank()) return;
        String sessionId = session.getId();

        long gamesPlayed = historyRepository.countByUserId(userId);
        achievementService.evaluate(userId, AchievementTrigger.FIRST_GAME, 1, sessionId, null);
        achievementService.evaluate(userId, AchievementTrigger.GAMES_PLAYED,
                (int) Math.min(gamesPlayed, Integer.MAX_VALUE), sessionId, null);
        achievementService.evaluate(userId, AchievementTrigger.HIGH_SCORE,
                p.getFinalScore(), sessionId, null);
        achievementService.evaluate(userId, AchievementTrigger.STREAK,
                p.getLongestStreak(), sessionId, null);

        // PERFECT_GAME only applies when the player got every question right;
        // threshold gates on the minimum number of questions so a one-question
        // deck doesn't trivially unlock the badge.
        if (p.getAccuracy() >= 1.0d && p.getTotalQuestions() > 0) {
            achievementService.evaluate(userId, AchievementTrigger.PERFECT_GAME,
                    p.getTotalQuestions(), sessionId, null);
        }

        if (isHost) {
            long hostGames = historyRepository.countByUserIdAndWasHostTrue(userId);
            achievementService.evaluate(userId, AchievementTrigger.HOST_GAMES,
                    (int) Math.min(hostGames, Integer.MAX_VALUE), sessionId, null);
        }
    }

    /**
     * Triggers that apply when the host did not play. The host's player-side
     * counters (HIGH_SCORE / STREAK / PERFECT_GAME) don't apply, but the
     * GAMES_PLAYED counter still bumps and HOST_GAMES is the whole point.
     */
    private void evaluateHostOnlyAchievements(InteractiveSession session) {
        String hostId = session.getHostUserId();
        if (hostId == null || hostId.isBlank()) return;
        String sessionId = session.getId();

        long gamesPlayed = historyRepository.countByUserId(hostId);
        long hostGames = historyRepository.countByUserIdAndWasHostTrue(hostId);
        achievementService.evaluate(hostId, AchievementTrigger.FIRST_GAME, 1, sessionId, null);
        achievementService.evaluate(hostId, AchievementTrigger.GAMES_PLAYED,
                (int) Math.min(gamesPlayed, Integer.MAX_VALUE), sessionId, null);
        achievementService.evaluate(hostId, AchievementTrigger.HOST_GAMES,
                (int) Math.min(hostGames, Integer.MAX_VALUE), sessionId, null);
    }

    /** Paginated history for the given user, newest-first by {@code playedAt}. */
    public Page<GameHistoryEntry> listForUser(String userId, Pageable pageable) {
        return historyRepository.findAllByUserId(userId, pageable);
    }

    /** Paginated history filtered to a single deck — drives "your best on this deck". */
    public Page<GameHistoryEntry> listForUserAndDeck(String userId, String deckId, Pageable pageable) {
        return historyRepository.findAllByUserIdAndDeckId(userId, deckId, pageable);
    }

    private GameHistoryEntry buildPlayerEntry(InteractiveSession session, PlayerPlacement p,
            String deckName, long durationMs, LocalDateTime playedAt, boolean isHost) {
        GameHistoryEntry e = baseEntry(session, deckName, durationMs, playedAt);
        e.setUserId(p.getUserId());
        e.setFinalScore(p.getFinalScore());
        e.setPlacement(p.getPlacement());
        e.setTotalQuestions(p.getTotalQuestions());
        e.setCorrectAnswers(p.getCorrectAnswers());
        e.setLongestStreak(p.getLongestStreak());
        e.setAccuracy(p.getAccuracy());
        e.setReactionsSent(p.getReactionsSent());
        e.setTeamId(p.getTeamId());
        e.setTeamName(resolveTeamName(session, p.getTeamId()));
        e.setWasHost(isHost);
        e.setWasGuest(p.isGuest());

        // currentStreakAtEnd isn't on PlayerPlacement — recover it from the
        // live InteractiveSessionPlayer if still attached. Defaults to 0 when
        // the player record has already been pruned.
        for (InteractiveSessionPlayer sp : session.getPlayers()) {
            if (sp.getUserId() != null && sp.getUserId().equals(p.getUserId())) {
                e.setCurrentStreakAtEnd(sp.getCurrentStreak());
                break;
            }
        }
        return e;
    }

    private GameHistoryEntry buildHostOnlyEntry(InteractiveSession session, String deckName,
            long durationMs, LocalDateTime playedAt) {
        GameHistoryEntry e = baseEntry(session, deckName, durationMs, playedAt);
        e.setUserId(session.getHostUserId());
        e.setTotalQuestions(session.getDeckSnapshot() == null ? 0 : session.getDeckSnapshot().size());
        e.setWasHost(true);
        e.setWasGuest(isUserGuest(session.getHostUserId()));
        return e;
    }

    private GameHistoryEntry baseEntry(InteractiveSession session, String deckName,
            long durationMs, LocalDateTime playedAt) {
        GameHistoryEntry e = new GameHistoryEntry();
        e.setId(UUID.randomUUID().toString());
        e.setInteractiveSessionId(session.getId());
        e.setDeckId(session.getDeckId());
        e.setDeckName(deckName);
        e.setHost(UserSnapshot.of(session.getHostUserId(), session.getHostName()));
        e.setDurationMs(durationMs);
        e.setPlayedAt(playedAt);
        return e;
    }

    private boolean writeEntry(GameHistoryEntry entry) {
        try {
            historyRepository.insert(entry);
            return true;
        } catch (DuplicateKeyException ignored) {
            // Replay / retry — the (userId, sessionId) row already exists.
            return false;
        }
    }

    private boolean isUserGuest(String userId) {
        if (userId == null) return false;
        return userRepository.findById(userId)
                .map(u -> Boolean.TRUE.equals(u.getIsGuest()))
                .orElse(false);
    }

    private String resolveTeamName(InteractiveSession session, String teamId) {
        if (teamId == null || session.getTeams() == null) return null;
        for (Team t : session.getTeams()) {
            if (teamId.equals(t.getId())) return t.getName();
        }
        return null;
    }

    private long computeDurationMs(InteractiveSession session) {
        if (session.getStartedAt() == null || session.getEndedAt() == null) return 0L;
        return Duration.between(session.getStartedAt(), session.getEndedAt()).toMillis();
    }

    /**
     * Read-modify-write on the host's Membership counter. We resolve the User
     * doc anyway to read {@code isGuest}; guests never accrue Membership
     * usage, and missing users (e.g. a deleted host) are silently skipped.
     * Counter rolls over when the previous period start falls in an earlier
     * calendar month.
     */
    private void bumpHostMonthlyCounter(String hostUserId, LocalDateTime playedAt) {
        if (hostUserId == null) return;
        userRepository.findById(hostUserId).ifPresent(user -> {
            if (Boolean.TRUE.equals(user.getIsGuest())) return;
            Membership membership = user.getMembership();
            if (membership == null) {
                membership = new Membership();
                user.setMembership(membership);
            }
            LocalDateTime periodStart = membership.getMonthlyCountPeriodStart();
            boolean newMonth = periodStart == null
                    || periodStart.getYear() != playedAt.getYear()
                    || periodStart.getMonthValue() != playedAt.getMonthValue();
            if (newMonth) {
                membership.setMonthlyInteractiveSessionCount(1);
                membership.setMonthlyCountPeriodStart(playedAt.withDayOfMonth(1).toLocalDate().atStartOfDay());
            } else {
                membership.setMonthlyInteractiveSessionCount(
                        membership.getMonthlyInteractiveSessionCount() + 1);
            }
            userRepository.save(user);
        });
    }
}
