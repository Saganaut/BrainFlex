/**
 * Broadcast on /topic/interactive-session/{roomCode}/answered after each player submits an answer.
 * Lets every client show a live "✓ / waiting" indicator next to player names without
 * waiting for the round to end. Sent BEFORE round completion, so the answer details
 * stay hidden — only the userIds of people who've finished are revealed.
 */
package cephadex.brainflex.dto;

import java.util.List;

public record AnswerProgressMessage(
        int round,
        List<String> answeredUserIds,
        int totalPlayers) {
}
