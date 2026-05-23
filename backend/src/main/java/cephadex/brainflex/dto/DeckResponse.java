/**
 * Public-facing representation of a Deck for the create-interactiveSession + my-decks UIs.
 * The full `elements` list is included here — clients use it to preview decks
 * and (for owners) to drive the editor.
 */
package cephadex.brainflex.dto;

import java.time.Instant;
import java.util.List;

import cephadex.brainflex.model.deck.Deck;
import cephadex.brainflex.model.element.DeckElement;
import cephadex.brainflex.model.image.Image;
import cephadex.brainflex.model.enums.CollaboratorRole;
import cephadex.brainflex.model.enums.DeckVisibility;
import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.License;
import cephadex.brainflex.model.enums.PublishStatus;
import cephadex.brainflex.model.enums.SessionFormat;
import cephadex.brainflex.model.enums.ShowResponsesMode;
import cephadex.brainflex.model.session.InteractiveSessionSettings;

public record DeckResponse(
        String id,
        String name,
        String description,
        String creatorUserId,
        String organizationId,
        java.util.Set<String> tags,
        java.util.Set<String> tagIds,
        String subjectTagId,
        boolean isSystem,
        DeckVisibility visibility,
        SessionFormat defaultSessionFormat,
        ShowResponsesMode defaultShowResponses,
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
        Instant publishedAt,
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
        Instant lastPlayedAt,
        Instant createdAt,
        Instant updatedAt) {

    /**
     * Convenience constructor for unauthenticated reads / write responses
     * where the caller-specific {@code isFavorited} flag is not meaningful;
     * defaults to {@code false}.
     */
    public DeckResponse(Deck deck) {
        this(deck, false, null, null);
    }

    public DeckResponse(Deck deck, boolean isFavorited) {
        this(deck, isFavorited, null, null);
    }

    public DeckResponse(Deck deck, boolean isFavorited, Integer myRating) {
        this(deck, isFavorited, myRating, null);
    }

    public DeckResponse(Deck deck, boolean isFavorited, Integer myRating, CollaboratorRole myRole) {
        this(
                deck.getId(),
                deck.getContent().getName(),
                deck.getContent().getDescription(),
                deck.getCreatorUserId(),
                deck.getOrganizationId(),
                deck.getTags(),
                deck.getTagIds(),
                deck.getSubjectTagId(),
                deck.isSystem(),
                deck.getVisibility(),
                deck.getContent().getFormat(),
                deck.getContent().getShowResponses(),
                deck.getContent().getCover(),
                deck.getContent().getBackground(),
                deck.getContent().getThemeId(),
                deck.getContent().getSettings(),
                deck.getEstimatedDurationMinutes(),
                deck.getContent().getElements() == null ? 0 : deck.getContent().getElements().size(),
                deck.getContent().getElements(),
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
