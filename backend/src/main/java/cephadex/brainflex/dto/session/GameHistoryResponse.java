/**
 * Wire-format wrapper around {@link cephadex.brainflex.model.session.GameHistoryEntry}.
 *
 * Field-for-field projection of the document; the frontend chooses whether to
 * surface team / host / streak fields based on the entry. The wrapping page
 * envelope ({@link Page}) carries pagination metadata.
 */
package cephadex.brainflex.dto.session;

import cephadex.brainflex.dto.shared.Page;

import java.time.Instant;

import cephadex.brainflex.model.session.GameHistoryEntry;
import cephadex.brainflex.model.shared.UserSnapshot;

public record GameHistoryResponse(
        String id,
        String interactiveSessionId,
        String deckId,
        String deckName,
        UserSnapshot host,
        int finalScore,
        int placement,
        int totalQuestions,
        int correctAnswers,
        int longestStreak,
        int currentStreakAtEnd,
        double accuracy,
        int reactionsSent,
        long durationMs,
        String teamId,
        String teamName,
        boolean wasHost,
        boolean wasGuest,
        Instant playedAt) {

    public static GameHistoryResponse from(GameHistoryEntry e) {
        return new GameHistoryResponse(
                e.getId(),
                e.getInteractiveSessionId(),
                e.getDeckId(),
                e.getDeckName(),
                e.getHost(),
                e.getFinalScore(),
                e.getPlacement(),
                e.getTotalQuestions(),
                e.getCorrectAnswers(),
                e.getEndStats().longestStreak(),
                e.getCurrentStreakAtEnd(),
                e.getEndStats().accuracy(),
                e.getEndStats().reactionsSent(),
                e.getDurationMs(),
                e.getTeamId(),
                e.getTeamName(),
                e.isWasHost(),
                e.isWasGuest(),
                e.getPlayedAt());
    }
}
