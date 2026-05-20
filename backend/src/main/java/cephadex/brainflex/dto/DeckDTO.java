/**
 * Public-facing representation of a Deck for the create-interactiveSession + my-decks UIs.
 * The full `elements` list is included here — clients use it to preview decks
 * and (for owners) to drive the editor.
 */
package cephadex.brainflex.dto;

import java.time.LocalDateTime;
import java.util.List;

import cephadex.brainflex.model.Deck;
import cephadex.brainflex.model.InteractiveSessionSettings;
import cephadex.brainflex.model.element.DeckElement;
import cephadex.brainflex.model.element.Image;
import cephadex.brainflex.model.enums.CollaboratorRole;
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
        InteractiveSessionSettings defaultSettings,
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
        boolean isFavorited,
        double averageRating,
        int ratingCount,
        // Caller-specific rating snapshot. {@code myRating} is null when the
        // caller is anonymous or has not yet rated; {@code isRatedByMe} is
        // {@code myRating != null} but stored explicitly so clients can
        // branch on the flag without a null check on a number.
        Integer myRating,
        boolean isRatedByMe,
        // Caller-specific collaborator role on this deck. {@code null} when the
        // caller is anonymous or has no row in deck_collaborators (e.g. an
        // anonymous viewer of a PUBLIC deck). OWNER/EDITOR/VIEWER drive the
        // role pill on the "My Decks → Shared with me" tab.
        CollaboratorRole myRole,
        LocalDateTime lastPlayedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    /**
     * Convenience constructor for unauthenticated reads / write responses
     * where the caller-specific {@code isFavorited} flag is not meaningful;
     * defaults to {@code false}.
     */
    public DeckDTO(Deck deck) {
        this(deck, false, null, null);
    }

    public DeckDTO(Deck deck, boolean isFavorited) {
        this(deck, isFavorited, null, null);
    }

    public DeckDTO(Deck deck, boolean isFavorited, Integer myRating) {
        this(deck, isFavorited, myRating, null);
    }

    public DeckDTO(Deck deck, boolean isFavorited, Integer myRating, CollaboratorRole myRole) {
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
                isFavorited,
                deck.getAverageRating(),
                deck.getRatingCount(),
                myRating,
                myRating != null,
                myRole,
                deck.getLastPlayedAt(),
                deck.getCreatedAt(),
                deck.getUpdatedAt());
    }
}
