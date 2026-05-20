/**
 * Per-element rollup embedded inside {@link DeckAnalytics}.
 *
 * Maintained incrementally by {@code DeckAnalyticsService.recordGame} on every
 * finished session — never recomputed at read time. One row per
 * {@code DeckElement.id} that has ever been presented in a finished game.
 *
 * {@code distribution} is a discriminated map whose key shape depends on the
 * element kind it belongs to (see {@code DeckAnalyticsService} for the per-kind
 * key conventions). For averages-style distributions (Allocation, Scales,
 * Ranking) the value is the average×100 to keep the {@code Map<String,Integer>}
 * shape — divide by 100.0 on read.
 *
 * {@code correctCount} is always zero for survey-only kinds
 * (DRAWING, WORD_CLOUD, ALLOCATION, Q_AND_A); accuracy calculations must skip
 * those kinds when rolling up deck-level accuracy.
 */
package cephadex.brainflex.model;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class ElementStats {

    private int presentedCount;
    private int answeredCount;
    private int correctCount;
    private long totalTimeMs;
    private double averageTimeMs;
    private Map<String, Integer> distribution = new HashMap<>();
    private int reactionsReceived;
    private int chatMessagesDuringRound;
}
