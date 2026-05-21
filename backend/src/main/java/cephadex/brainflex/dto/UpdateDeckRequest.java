/**
 * Request body for PUT /api/decks/{id}. Null fields leave the deck unchanged;
 * non-null fields are patched. To clear an image, send `Image.empty()`
 * (a record with no variants).
 */
package cephadex.brainflex.dto;

import java.util.List;

import cephadex.brainflex.model.element.Image;
import cephadex.brainflex.model.enums.DeckVisibility;
import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.License;
import cephadex.brainflex.model.enums.SessionFormat;
import cephadex.brainflex.model.enums.ShowResponsesMode;
import jakarta.validation.constraints.Size;

public record UpdateDeckRequest(
        @Size(min = 1, max = 100) String name,
        @Size(max = 500) String description,
        List<@Size(max = 50) String> tags,
        List<@Size(max = 64) String> tagIds,
        // Pass an empty string to clear the subject (or pass null to leave it).
        @Size(max = 64) String subjectTagId,
        DeckVisibility visibility,
        SessionFormat defaultSessionFormat,
        ShowResponsesMode defaultShowResponses,
        Image cover,
        Image background,
        String themeId,
        Integer estimatedDurationMinutes,
        // Editor-controlled discovery metadata. Publish-status transitions go
        // through the dedicated /publish, /unpublish, /archive endpoints
        // (which stamp publishedAt); this body is for author-facing data.
        @Size(max = 20) String language,
        Difficulty difficulty,
        @Size(max = 16) String ageRange,
        License license) {
}
