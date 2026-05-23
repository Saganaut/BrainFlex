/**
 * Wire shape for {@link cephadex.brainflex.model.session.PlayerPlacement}. Projects the
 * stored {@code UserSnapshot} through {@link cephadex.brainflex.model.shared.PublicUserSnapshot}
 * so the broadcast leaderboard never carries the real userId; clients identify
 * placements by their session-scoped {@code playerId}.
 */
package cephadex.brainflex.dto.session;

import cephadex.brainflex.model.session.PlayerEndStats;
import cephadex.brainflex.model.session.PlayerPlacement;
import cephadex.brainflex.model.shared.PublicUserSnapshot;

public record PlayerPlacementResponse(
        String playerId,
        PublicUserSnapshot user,
        int finalScore,
        int placement,
        int correctAnswers,
        int totalQuestions,
        String teamId,
        PlayerEndStats endStats,
        int speedBonusTotal) {

    public PlayerPlacementResponse(PlayerPlacement placement) {
        this(
                placement.getPlayerId(),
                PublicUserSnapshot.from(placement.getUser()),
                placement.getFinalScore(),
                placement.getPlacement(),
                placement.getCorrectAnswers(),
                placement.getTotalQuestions(),
                placement.getTeamId(),
                placement.getEndStats(),
                placement.getSpeedBonusTotal());
    }
}
