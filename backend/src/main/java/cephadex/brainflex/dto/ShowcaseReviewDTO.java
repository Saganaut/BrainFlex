/**
 * Full post-showcase review payload. Each `rounds[]` entry includes the
 * un-redacted element + every player's submission for that element, so the
 * frontend can render whichever per-kind visualization fits (bar chart for
 * MCQ option counts, histogram for NUMBER, frequency list for TEXT, etc.).
 *
 * Aggregation is intentionally minimal here — the frontend has the raw
 * submissions and the canonical element, so it can shape any view we add
 * later without a backend redeploy.
 */
package cephadex.brainflex.dto;

import java.time.LocalDateTime;
import java.util.List;

import cephadex.brainflex.model.PlayerPlacement;
import cephadex.brainflex.model.answer.AnswerPayload;
import cephadex.brainflex.model.element.DeckElement;

public record ShowcaseReviewDTO(
        String showcaseId,
        String roomCode,
        LocalDateTime endedAt,
        boolean scoringEnabled,
        List<PlayerPlacement> placements,
        List<RoundReview> rounds) {

    public record RoundReview(
            int round,
            DeckElement element,
            int timedOutCount,
            List<PlayerRoundDetail> playerAnswers) {
    }

    public record PlayerRoundDetail(
            String userId,
            String userName,
            AnswerPayload payload,
            boolean wasCorrect,
            int pointsAwarded) {
    }
}
