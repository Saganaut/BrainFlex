/**
 * Per-game terminal stats snapshot — the three counters that survive the
 * LOBBY → IN_PROGRESS → FINISHED transition and are still meaningful after
 * the live {@link InteractiveSessionPlayer} record has been pruned.
 *
 * Embedded in {@link InteractiveSessionPlayer} (where it tracks running
 * values during play), copied verbatim to {@link PlayerPlacement} at game
 * end, and again to {@link GameHistoryEntry} when the per-user history
 * row is written. Keeping the shape identical across all three avoids the
 * field-by-field copy that used to live in {@code endGame} /
 * {@code recordFinish} and makes it obvious that these three values are
 * a single conceptual bundle.
 *
 * Currentstreak / speedBonusTotal / currentStreakAtEnd live on the parent
 * classes alongside this record — they are intentionally not part of the
 * bundle (currentStreak is a live-only counter, speedBonusTotal is a
 * scoring-derived field, and currentStreakAtEnd is history-only).
 */
package cephadex.brainflex.model.session;

public record PlayerEndStats(int longestStreak, double accuracy, int reactionsSent) {

    public static PlayerEndStats empty() {
        return new PlayerEndStats(0, 0.0, 0);
    }

    public PlayerEndStats withLongestStreak(int v) {
        return new PlayerEndStats(v, accuracy, reactionsSent);
    }

    public PlayerEndStats withAccuracy(double v) {
        return new PlayerEndStats(longestStreak, v, reactionsSent);
    }

    public PlayerEndStats withReactionsSent(int v) {
        return new PlayerEndStats(longestStreak, accuracy, v);
    }
}
