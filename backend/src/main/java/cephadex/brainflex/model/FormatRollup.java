/**
 * Per-{@link cephadex.brainflex.model.enums.SessionFormat} slice of a
 * {@link DeckAnalytics} rollup. Lives twice on the parent — once for GAME, once
 * for PRESENTATION — so the dashboard can show format-aware KPIs without
 * re-walking the sessions collection.
 *
 * The deck-wide totals on {@code DeckAnalytics} (averages across all formats)
 * stay populated for back-compat; this record is a strict slice on top of them.
 *
 * <h3>Score in PRESENTATION mode</h3>
 *
 * Presentations typically run with {@code scoringEnabled=false}, so player
 * scores are zero. The recorder leaves {@code averageScore} at its default for
 * PRESENTATION finishes rather than averaging zeros — the dashboard hides this
 * field in the Presentations segment, but a non-zero value here would be
 * misleading if a presentation ever does enable scoring.
 *
 * <h3>Legacy documents</h3>
 *
 * {@code DeckAnalytics} rows persisted before this record existed deserialize
 * with {@code gameRollup}/{@code presentationRollup} null; the service lazily
 * creates them on the next finish. Operators can re-run the
 * {@code --migrate.deck-analytics=true} backfill to repopulate them
 * deterministically from history.
 */
package cephadex.brainflex.model;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class FormatRollup {

    private int sessionCount;
    private int participantCount;

    private double averageScore;
    private double averageAccuracy;
    private long averageDurationMs;

    private LocalDateTime lastRunAt;
}
