/**
 * Broadcast to /topic/interactive-session/{roomCode}/ended when the final round ends.
 * Contains the full ranked leaderboard so the results screen can be
 * rendered immediately without an additional REST call.
 *
 * Each entry travels as a {@link PlayerPlacementResponse}, which is keyed by
 * session-scoped {@code playerId} and projects display fields through
 * {@link cephadex.brainflex.model.shared.PublicUserSnapshot} — the underlying
 * account userId never crosses the wire.
 */
package cephadex.brainflex.dto;

import java.util.List;

public record InteractiveSessionEndedMessage(List<PlayerPlacementResponse> placements) {
}
