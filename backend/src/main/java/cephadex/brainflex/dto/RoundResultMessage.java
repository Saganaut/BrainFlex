/**
 * Broadcast to /topic/showcase/{roomCode}/roundResult when a round ends.
 * Reveals the correct answer and each player's points so the UI can
 * show the result screen before advancing to the next round.
 *
 * MCQ: correctAnswer = winning option index; correctAnswerText = its label.
 * TEXT_INPUT: correctAnswer = -1; correctAnswerText = the canonical correct text.
 * Per-player: textAnswer is populated only for TEXT_INPUT rounds; selectedOption only for MCQ.
 */
package cephadex.brainflex.dto;

import java.util.List;

public record RoundResultMessage(
        int round,
        int correctAnswer,
        String correctAnswerText,
        List<PlayerRoundResult> playerResults) {

    /** Per-player outcome for a single round. */
    public record PlayerRoundResult(
            String userId,
            String userName,
            int selectedOption,     // MCQ: chosen index; -1 = timed out or N/A for text-input
            String textAnswer,      // TEXT_INPUT: player's submitted text; null otherwise
            boolean wasCorrect,
            int pointsAwarded,
            int totalScore) {
    }
}
