/**
 * Request body for PUT /api/decks/{id}. Null fields leave the deck unchanged;
 * non-null fields are patched. To clear an image, send an `Image` with a
 * blank/null `imgUrl` (or use `Image.empty()` on the client side).
 */
package cephadex.brainflex.dto;

import java.util.List;

import cephadex.brainflex.model.element.Image;
import cephadex.brainflex.model.enums.DeckPreset;
import cephadex.brainflex.model.enums.DeckVisibility;
import jakarta.validation.constraints.Size;

public record UpdateDeckRequest(
        @Size(min = 1, max = 100) String name,
        @Size(max = 500) String description,
        List<@Size(max = 50) String> tags,
        DeckVisibility visibility,
        DeckPreset recommendedPreset,
        Image cover,
        Image background,
        String themeId,
        Integer estimatedDurationMinutes) {
}
