/**
 * Wire shape for the post-game results endpoint. Wraps the stored
 * {@link cephadex.brainflex.model.session.InteractiveSessionResult} but projects
 * every placement through {@link PlayerPlacementResponse} so the broadcast
 * leaderboard never carries the underlying account userId — every player is
 * identified by their session-scoped {@code playerId} and the display fields
 * travel via {@link cephadex.brainflex.model.shared.PublicUserSnapshot}.
 */
package cephadex.brainflex.dto;

import java.time.Instant;
import java.util.List;

import cephadex.brainflex.model.session.InteractiveSessionResult;

public record InteractiveSessionResultResponse(
        String id,
        String interactiveSessionId,
        List<PlayerPlacementResponse> placements,
        Instant endedAt) {

    public static InteractiveSessionResultResponse of(InteractiveSessionResult result) {
        return new InteractiveSessionResultResponse(
                result.getId(),
                result.getInteractiveSessionId(),
                result.getPlacements() == null
                        ? List.of()
                        : result.getPlacements().stream()
                                .map(PlayerPlacementResponse::new)
                                .toList(),
                result.getEndedAt());
    }
}
