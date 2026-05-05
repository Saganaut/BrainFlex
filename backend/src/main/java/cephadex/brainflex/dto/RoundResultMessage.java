/**
 * Broadcast to /topic/game/{roomCode}/roundResult when a round ends.
 * Reveals the correct answer and each player's points so the UI can
 * show the result screen before advancing to the next round.
 */
package cephadex.brainflex.dto;

import java.util.List;

public record RoundResultMessage(
        int round,
        int correctAnswer,          // index into the question's options list
        String correctAnswerText,   // human-readable for convenience
        List<PlayerRoundResult> playerResults) {

    /** Per-player outcome for a single round. */
    public record PlayerRoundResult(
            String userId,
            String userName,
            int selectedOption,     // -1 = timed out
            boolean wasCorrect,
            int pointsAwarded,
            int totalScore) {
    }
}
