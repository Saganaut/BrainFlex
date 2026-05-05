/**
 * Broadcast to /topic/game/{roomCode}/gameOver when the final round ends.
 * Contains the full ranked leaderboard so the results screen can be
 * rendered immediately without an additional REST call.
 */
package cephadex.brainflex.dto;

import java.util.List;

import cephadex.brainflex.model.PlayerPlacement;

public record GameOverMessage(List<PlayerPlacement> placements) {
}
