/**
 * Per-deck rolled-up statistics document, 1:1 with {@link Deck} via shared id.
 *
 * Read-amplified: writes happen on every finished session (incrementally —
 * running averages, never a full recompute), reads happen rarely on the
 * analytics dashboard. {@code DeckAnalyticsService.recordGame} is the sole
 * writer; the backfill migration replays existing finished sessions through
 * the same path.
 *
 * Deck-level fields ({@code totalPlays}, {@code averageScore},
 * {@code averageAccuracy}, {@code averageDurationMs}) aggregate across all
 * plays. {@code perElement} carries one {@link ElementStats} per element id
 * that has ever been presented; survey-only kinds report zero
 * {@code correctCount}.
 *
 * {@code totalPlayers} is a coarse "submissions accepted" tally — distinct
 * players across plays are not deduped (no HyperLogLog yet). If we ever need
 * exact distinct-player counts the value will be replaced; treat it as an
 * upper bound on "people who played" for now.
 */
package cephadex.brainflex.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "deck_analytics")
public class DeckAnalytics {

    @Id
    private String deckId;

    private int totalPlays;
    private int totalPlayers;

    private double averageScore;
    private double averageAccuracy;
    private long averageDurationMs;

    private Map<String, ElementStats> perElement = new HashMap<>();

    private LocalDateTime lastPlayedAt;
    private LocalDateTime updatedAt;
}
