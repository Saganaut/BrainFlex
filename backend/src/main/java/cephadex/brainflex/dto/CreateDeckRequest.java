/**
 * Request body for POST /api/decks.
 * Creates a new user-owned deck with no elements; elements are added separately
 * via the element-CRUD endpoints.
 */
package cephadex.brainflex.dto;

import java.util.List;

import cephadex.brainflex.model.element.Image;
import cephadex.brainflex.model.enums.DeckPreset;
import cephadex.brainflex.model.enums.DeckVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDeckRequest(
        // Optional client-generated id (UUID). When present, the frontend has
        // already created the deck optimistically and the POST is idempotent.
        // When null, the server assigns a fresh UUID.
        @Size(max = 64) String id,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        List<@Size(max = 50) String> tags,
        DeckVisibility visibility,        // null → PRIVATE
        DeckPreset recommendedPreset,     // null → GAME
        Image cover,
        Image background,
        String themeId,
        Integer estimatedDurationMinutes) {
}
