/**
 * Request body for POST /api/decks.
 * Creates a new user-owned deck with no elements; elements are added separately
 * via the element-CRUD endpoints.
 */
package cephadex.brainflex.dto;

import java.util.List;

import cephadex.brainflex.model.enums.DeckPreset;
import cephadex.brainflex.model.enums.DeckVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDeckRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        List<@Size(max = 50) String> tags,
        DeckVisibility visibility,        // null → PRIVATE
        DeckPreset recommendedPreset,     // null → GAME
        @Size(max = 2000) String coverImageUrl,
        @Size(max = 2000) String backgroundImageUrl,
        String themeId,
        Integer estimatedDurationMinutes) {
}
