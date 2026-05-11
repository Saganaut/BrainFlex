/**
 * Request body for PUT /api/decks/{id}. Null fields leave the deck unchanged;
 * non-null fields are patched. Image URLs use the empty string to clear
 * (versus null = leave alone).
 */
package cephadex.brainflex.dto;

import java.util.List;

import cephadex.brainflex.model.enums.DeckPreset;
import cephadex.brainflex.model.enums.DeckVisibility;
import jakarta.validation.constraints.Size;

public record UpdateDeckRequest(
        @Size(min = 1, max = 100) String name,
        @Size(max = 500) String description,
        List<@Size(max = 50) String> tags,
        DeckVisibility visibility,
        DeckPreset recommendedPreset,
        @Size(max = 2000) String coverImageUrl,
        @Size(max = 2000) String backgroundImageUrl,
        String themeId,
        Integer estimatedDurationMinutes) {
}
