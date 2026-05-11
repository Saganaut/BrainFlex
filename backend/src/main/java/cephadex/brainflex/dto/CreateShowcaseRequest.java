/**
 * Request body for POST /api/showcases. The host picks a deck; settings fall
 * back to that deck's `defaultSettings` (which in turn fall back to platform
 * defaults). `timePerQuestion = 0` disables the round timer for questions
 * (slides always have their own display timer).
 */
package cephadex.brainflex.dto;

import cephadex.brainflex.model.enums.GameMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateShowcaseRequest(
        @NotBlank String deckId,
        GameMode gameMode,                              // null → deck default
        @Min(1) @Max(60) Integer totalRounds,           // null → deck default
        @Min(0) @Max(120) Integer timePerQuestion,      // null → deck default; 0 = unlimited
        Boolean speedBonus,
        Boolean allowGuests,
        @Min(2) @Max(20) Integer maxPlayers,
        Boolean allowLateJoin,
        Boolean showScoresImmediately,
        Boolean scoringEnabled) {
}
