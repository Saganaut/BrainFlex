/**
 * Broadcast on /topic/interactive-session/{roomCode}/voted whenever a player casts a vote
 * during VOTE phase. Mirrors `AnswerProgressMessage` for SUBMIT phase — the
 * client uses it to render a "3 of 5 voted" indicator. Carries session-scoped
 * {@code playerId}s; the underlying userId never crosses the wire.
 */
package cephadex.brainflex.dto.session.message;

import java.util.List;

public record VoteProgressMessage(
        int round,
        List<String> votedPlayerIds,
        int totalPlayers) {
}
