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
package cephadex.brainflex.model;

import java.time.LocalDateTime;
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
    private int currentStreak = 0;

    /** Best per-game streak the user has ever hit — distinct from {@link #currentStreak},
     *  which chunk 15 reinterpreted as a lifetime "games played" tally. */
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
    private LocalDateTime weeklyPointsResetAt;
    private LocalDateTime monthlyPointsResetAt;

    /** Last time the user finished an interactive session. Drives "back from a break"
     *  copy in the digest email and the "last active" column in admin tooling. */
    private LocalDateTime lastPlayedAt;
}
