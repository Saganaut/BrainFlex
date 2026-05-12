/**
 * Broadcast on /topic/showcase/{roomCode}/voted whenever a player casts a vote
 * during VOTE phase. Mirrors `AnswerProgressMessage` for SUBMIT phase — the
 * client uses it to render a "3 of 5 voted" indicator.
 */
package cephadex.brainflex.dto;

import java.util.List;

public record VoteProgressMessage(
        int round,
        List<String> votedUserIds,
        int totalPlayers) {
}
