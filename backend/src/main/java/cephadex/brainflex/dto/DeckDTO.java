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
import cephadex.brainflex.model.element.Image;
import cephadex.brainflex.model.enums.DeckPreset;
import cephadex.brainflex.model.enums.DeckVisibility;
import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.License;
import cephadex.brainflex.model.enums.PublishStatus;

public record DeckDTO(
        String id,
        String name,
        String description,
        String creatorUserId,
        String organizationId,
        List<String> tags,
        List<String> tagIds,
        String subjectTagId,
        boolean isSystem,
        DeckVisibility visibility,
        DeckPreset recommendedPreset,
        Image cover,
        Image background,
        String themeId,
        ShowcaseSettings defaultSettings,
        Integer estimatedDurationMinutes,
        int elementCount,
        List<DeckElement> elements,
        String parentDeckId,
        String originalAuthorUserId,
        PublishStatus publishStatus,
        LocalDateTime publishedAt,
        String language,
        Difficulty difficulty,
        String ageRange,
        License license,
        int playCount,
        int viewCount,
        int favoriteCount,
        double averageRating,
        int ratingCount,
        LocalDateTime lastPlayedAt,
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
                deck.getTagIds(),
                deck.getSubjectTagId(),
                deck.isSystem(),
                deck.getVisibility(),
                deck.getRecommendedPreset(),
                deck.getCover(),
                deck.getBackground(),
                deck.getThemeId(),
                deck.getDefaultSettings(),
                deck.getEstimatedDurationMinutes(),
                deck.getElements() == null ? 0 : deck.getElements().size(),
                deck.getElements(),
                deck.getParentDeckId(),
                deck.getOriginalAuthorUserId(),
                deck.getPublishStatus(),
                deck.getPublishedAt(),
                deck.getLanguage(),
                deck.getDifficulty(),
                deck.getAgeRange(),
                deck.getLicense(),
                deck.getPlayCount(),
                deck.getViewCount(),
                deck.getFavoriteCount(),
                deck.getAverageRating(),
                deck.getRatingCount(),
                deck.getLastPlayedAt(),
                deck.getCreatedAt(),
                deck.getUpdatedAt());
    }
}
