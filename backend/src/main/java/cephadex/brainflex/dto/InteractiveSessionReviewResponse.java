/**
 * Full post-interactiveSession review payload. Each `rounds[]` entry includes the
 * un-redacted element + every player's submission for that element, so the
 * frontend can render whichever per-kind visualization fits (bar chart for
 * MCQ option counts, histogram for NUMBER, frequency list for TEXT, etc.).
 *
 * Aggregation is intentionally minimal here — the frontend has the raw
 * submissions and the canonical element, so it can shape any view we add
 * later without a backend redeploy.
 */
package cephadex.brainflex.dto;

import java.time.Instant;
import java.util.List;

import cephadex.brainflex.model.element.DeckElement;
import cephadex.brainflex.model.session.PlayerPlacement;

public record InteractiveSessionReviewResponse(
                String interactiveSessionId,
                String roomCode,
                Instant endedAt,
                boolean scoringEnabled,
                List<PlayerPlacement> placements,
                List<RoundReview> rounds) {

        public record RoundReview(
                        int round,
                        DeckElement element,
                        int timedOutCount,
                        // Per-player rows for this round. Same shape as the live
                        // RoundResultMessage broadcast — totalScore here is the running
                        // cumulative total after this round, computed in snapshot order
                        // by InteractiveSessionService.buildReview.
                        List<PlayerRoundResponse> playerAnswers) {
        }
}
