/**
 * Wire-format wrapper around {@link cephadex.brainflex.model.GameHistoryEntry}.
 *
 * Field-for-field projection of the document; the frontend chooses whether to
 * surface team / host / streak fields based on the entry. The wrapping page
 * envelope ({@link GameHistoryPage}) carries pagination metadata.
 */
package cephadex.brainflex.dto;

import java.time.LocalDateTime;

import cephadex.brainflex.model.GameHistoryEntry;

public record GameHistoryDTO(
        String id,
        String interactiveSessionId,
        String deckId,
        String deckName,
        String hostUserId,
        String hostName,
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
        LocalDateTime playedAt) {

    public static GameHistoryDTO from(GameHistoryEntry e) {
        return new GameHistoryDTO(
                e.getId(),
                e.getInteractiveSessionId(),
                e.getDeckId(),
                e.getDeckName(),
                e.getHostUserId(),
                e.getHostName(),
                e.getFinalScore(),
                e.getPlacement(),
                e.getTotalQuestions(),
                e.getCorrectAnswers(),
                e.getLongestStreak(),
                e.getCurrentStreakAtEnd(),
                e.getAccuracy(),
                e.getReactionsSent(),
                e.getDurationMs(),
                e.getTeamId(),
                e.getTeamName(),
                e.isWasHost(),
                e.isWasGuest(),
                e.getPlayedAt());
    }
}
