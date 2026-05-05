/**
 * Broadcast to /topic/game/{roomCode}/round when a new round begins.
 * Clients use this to display the question, start the countdown timer,
 * and enable the answer buttons.
 */
package cephadex.brainflex.dto;

import java.time.LocalDateTime;

public record RoundStartMessage(
        int round,          // 0-based index
        int totalRounds,
        QuestionDTO question,
        LocalDateTime startedAt) {
}
