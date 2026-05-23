/**
 * Public-facing representation of a {@link cephadex.brainflex.model.deck.DeckCollection}.
 *
 * Two read shapes share this type: the {@code /api/collections/mine} list
 * endpoint returns the metadata with a deck-id list only, while the
 * {@code /api/collections/{id}} detail endpoint additionally hydrates each
 * id into a {@link DeckResponse}. The {@code decks} field is null on the list
 * response and populated on the detail response; clients branch on null.
 */
package cephadex.brainflex.dto;

import java.time.Instant;
import java.util.List;

import cephadex.brainflex.model.deck.DeckCollection;
import cephadex.brainflex.model.image.Image;
import cephadex.brainflex.model.enums.DeckVisibility;

public record DeckCollectionResponse(
        String id,
        String ownerUserId,
        String organizationId,
        String name,
        String description,
        Image cover,
        List<String> deckIds,
        DeckVisibility visibility,
        int viewCount,
        int deckCount,
        // Only populated by the detail endpoint; null on list responses to
        // keep the my-collections payload small.
        List<DeckResponse> decks,
        Instant createdAt,
        Instant updatedAt) {

    public static DeckCollectionResponse summary(DeckCollection c) {
        return new DeckCollectionResponse(
                c.getId(),
                c.getOwnerUserId(),
                c.getOrganizationId(),
                c.getName(),
                c.getDescription(),
                c.getCover(),
                c.getDeckIds(),
                c.getVisibility(),
                c.getViewCount(),
                c.getDeckIds() == null ? 0 : c.getDeckIds().size(),
                null,
                c.getCreatedAt(),
                c.getUpdatedAt());
    }

    public static DeckCollectionResponse detail(DeckCollection c, List<DeckResponse> decks) {
        return new DeckCollectionResponse(
                c.getId(),
                c.getOwnerUserId(),
                c.getOrganizationId(),
                c.getName(),
                c.getDescription(),
                c.getCover(),
                c.getDeckIds(),
                c.getVisibility(),
                c.getViewCount(),
                c.getDeckIds() == null ? 0 : c.getDeckIds().size(),
                decks,
                c.getCreatedAt(),
                c.getUpdatedAt());
    }
}
