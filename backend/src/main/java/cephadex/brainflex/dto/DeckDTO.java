/**
 * Public-facing representation of a Deck for the create-showcase + my-decks UIs.
 * The full `elements` list is included here — clients use it to preview decks
 * and (for owners) to drive the editor.
 */
package cephadex.brainflex.dto;

import java.time.LocalDateTime;
import java.util.List;

import cephadex.brainflex.model.Deck;
import cephadex.brainflex.model.ShowcaseSettings;
import cephadex.brainflex.model.element.DeckElement;
import cephadex.brainflex.model.enums.DeckPreset;
import cephadex.brainflex.model.enums.DeckVisibility;

public record DeckDTO(
        String id,
        String name,
        String description,
        String creatorUserId,
        String organizationId,
        List<String> tags,
        boolean isSystem,
        DeckVisibility visibility,
        DeckPreset recommendedPreset,
        String coverImageUrl,
        String backgroundImageUrl,
        String themeId,
        ShowcaseSettings defaultSettings,
        Integer estimatedDurationMinutes,
        int elementCount,
        List<DeckElement> elements,
        String parentDeckId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public DeckDTO(Deck deck) {
        this(
                deck.getId(),
                deck.getName(),
                deck.getDescription(),
                deck.getCreatorUserId(),
                deck.getOrganizationId(),
                deck.getTags(),
                deck.isSystem(),
                deck.getVisibility(),
                deck.getRecommendedPreset(),
                deck.getCoverImageUrl(),
                deck.getBackgroundImageUrl(),
                deck.getThemeId(),
                deck.getDefaultSettings(),
                deck.getEstimatedDurationMinutes(),
                deck.getElements() == null ? 0 : deck.getElements().size(),
                deck.getElements(),
                deck.getParentDeckId(),
                deck.getCreatedAt(),
                deck.getUpdatedAt());
    }
}
