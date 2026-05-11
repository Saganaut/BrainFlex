/**
 * Request body for POST /api/showcases.
 * Registered users supply the content pack and optional overrides for game
 * settings; any omitted fields fall back to the defaults in ShowcaseSettings.
 */
package cephadex.brainflex.dto;

import cephadex.brainflex.model.enums.GameMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateShowcaseRequest(
        @NotBlank String deckId,
        GameMode gameMode,       // null → SIMULTANEOUS
        @Min(1) @Max(30) Integer totalRounds,        // null → 10
        @Min(5) @Max(60) Integer timePerQuestion,    // null → 15 seconds
        Boolean speedBonus,      // null → true
        Boolean allowGuests,     // null → true
        @Min(2) @Max(20) Integer maxPlayers,         // null → 8
        Boolean noTimer,                // null → false
        Boolean allowLateJoin,          // null → false
        Boolean showScoresImmediately,  // null → true
        Boolean scoringEnabled          // null → true (Game preset); false for Pulse
) {
}
