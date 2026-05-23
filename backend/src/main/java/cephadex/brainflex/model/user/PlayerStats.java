/**
 * Embedded gameplay tallies for a {@link User}. All counters are monotonic
 * (and never reset) except {@link #weeklyPoints} and {@link #monthlyPoints},
 * which a scheduled job rewinds to 0 at the start of each ISO week / calendar
 * month — chunk 20 promises a cron for this; until that lands the timestamps
 * stay null and the totals just accumulate.
 *
 * The per-{@code ElementKind} maps {@link #presentedByKind} and {@link #correctByKind}
 * are keyed by the enum's {@code name()} (e.g. {@code "MCQ"}, {@code "WORD_CLOUD"})
 * so we don't have to touch this file when a new kind is added — callers stash
 * tallies under the string name on the way out of {@code GameHistoryService}.
 */
package cephadex.brainflex.model.user;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerStats {
    private int gamesPlayed = 0;
    private int highScore = 0;
    private int totalPoints = 0;

    /** Days-in-a-row the user has finished at least one interactive session.
     *  Distinct from
     *  {@link cephadex.brainflex.model.session.InteractiveSessionPlayer#getCurrentStreak()},
     *  which is the per-game consecutive-correct-answers counter. Maintained
     *  by {@code InteractiveSessionService#updateStatsAfterGame} via
     *  {@link #nextDailyLoginStreak(int, Instant, Instant, ZoneId)}; the day
     *  boundary is computed in the user's {@code timezone} (UTC fallback). */
    private int dailyLoginStreak = 0;

    /** Best per-game consecutive-correct streak the user has ever hit. Tracks the
     *  high-water mark of {@code InteractiveSessionPlayer.currentStreak} across
     *  finished games. */
    private int longestStreak = 0;

    /** Number of games where the user answered every scored element correctly. */
    private int perfectGames = 0;

    /** Lifetime emoji reactions the user has sent in interactive sessions. Drives the
     *  {@code REACTIONS_SENT} achievement trigger (still deferred per chunk 17). */
    private int totalReactionsSent = 0;

    /** Per-ElementKind tallies, keyed by {@code ElementKind.name()}. Maps stay sparse —
     *  a kind only appears once it has been presented or correctly answered at least
     *  once. */
    private Map<String, Integer> presentedByKind = new LinkedHashMap<>();
    private Map<String, Integer> correctByKind = new LinkedHashMap<>();

    /** Rolling-window point totals reset by the weekly / monthly point-reset cron.
     *  Until the cron lands these accumulate freely. */
    private int weeklyPoints = 0;
    private int monthlyPoints = 0;

    /** Timestamp of the most recent reset; null until the cron has fired at least once. */
    private Instant weeklyPointsResetAt;
    private Instant monthlyPointsResetAt;

    /** Last time the user finished an interactive session. Drives "back from a break"
     *  copy in the digest email and the "last active" column in admin tooling. */
    private Instant lastPlayedAt;

    /**
     * Pure-function helper for advancing {@link #dailyLoginStreak} when a game
     * finishes. Returns:
     * <ul>
     *   <li>{@code 1} if {@code previousPlay} is {@code null} (first ever
     *       finished game),</li>
     *   <li>{@code max(currentStreak, 1)} if {@code now} falls on the same
     *       local day as {@code previousPlay} — the user has already played
     *       today, the streak doesn't move,</li>
     *   <li>{@code currentStreak + 1} if {@code now}'s local day is exactly
     *       one day after {@code previousPlay}'s,</li>
     *   <li>{@code 1} on any larger gap (missed a day, streak broken).</li>
     * </ul>
     * Day boundaries are computed in {@code zone}; callers should resolve
     * {@link User#getTimezone()} to a {@link ZoneId} and fall back to UTC
     * when null or unparseable.
     */
    public static int nextDailyLoginStreak(
            int currentStreak, Instant previousPlay, Instant now, ZoneId zone) {
        if (previousPlay == null) return 1;
        LocalDate previousDay = previousPlay.atZone(zone).toLocalDate();
        LocalDate today = now.atZone(zone).toLocalDate();
        long gap = ChronoUnit.DAYS.between(previousDay, today);
        if (gap <= 0) return Math.max(currentStreak, 1);
        if (gap == 1) return currentStreak + 1;
        return 1;
    }
}
