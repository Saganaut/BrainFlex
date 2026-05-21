/**
 * Per-element rollup embedded inside {@link DeckAnalytics}.
 *
 * Maintained incrementally by {@code DeckAnalyticsService.recordSessionFinish}
 * on every finished session — never recomputed at read time. One row per
 * {@code DeckElement.id} that has ever been presented in a finished session
 * (GAME or PRESENTATION; the per-element rollup is intentionally merged
 * across formats — see {@link DeckAnalytics} for the per-format split).
 *
 * {@code distribution} is a discriminated map whose key shape depends on the
 * element kind it belongs to (see {@code DeckAnalyticsService} for the per-kind
 * key conventions). For averages-style distributions (Allocation, Scales,
 * Ranking) the value is the average×100 to keep the {@code Map<String,Integer>}
 * shape — divide by 100.0 on read.
 *
 * {@code correctCount} is always zero for survey-only kinds
 * (DRAWING, WORD_CLOUD, ALLOCATION, Q_AND_A); accuracy calculations must skip
 * those kinds when rolling up deck-level accuracy. The frontend uses
 * {@code answeredCount + correctCount} to decide whether the Presentations
 * segment should show an accuracy column for this element.
 */
package cephadex.brainflex.model;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class ElementStats {
    // How many was this shown too
    private int presentedCount;
    // how many answered
    private int answeredCount;
    // how many got the answer correct
    private int correctCount;
    private long totalTimeMs;
    private double averageTimeMs;
    private Map<String, Integer> distribution = new HashMap<>();

    // these two fields are probably not necessary?
    private int reactionsReceived;
    private int chatMessagesDuringRound;
}
