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
import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.License;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDeckRequest(
        // Optional client-generated id (UUID). When present, the frontend has
        // already created the deck optimistically and the POST is idempotent.
        // When null, the server assigns a fresh UUID.
        @Size(max = 64) String id,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        // Legacy free-form tag strings, kept for the duration of the
        // tagIds migration. New clients send tagIds + subjectTagId.
        List<@Size(max = 50) String> tags,
        List<@Size(max = 64) String> tagIds,
        @Size(max = 64) String subjectTagId,
        DeckVisibility visibility,        // null → PRIVATE
        DeckPreset recommendedPreset,     // null → GAME
        Image cover,
        Image background,
        String themeId,
        Integer estimatedDurationMinutes,
        // Discovery metadata. publishStatus is intentionally NOT here — new
        // decks always start as DRAFT and the author flips them via the
        // dedicated /publish endpoint once they're ready.
        @Size(max = 20) String language,
        Difficulty difficulty,
        @Size(max = 16) String ageRange,
        License license) {
}
