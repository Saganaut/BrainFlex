/**
 * Public-facing representation of a {@link cephadex.brainflex.model.DeckCollection}.
 *
 * Two read shapes share this type: the {@code /api/collections/mine} list
 * endpoint returns the metadata with a deck-id list only, while the
 * {@code /api/collections/{id}} detail endpoint additionally hydrates each
 * id into a {@link DeckDTO}. The {@code decks} field is null on the list
 * response and populated on the detail response; clients branch on null.
 */
package cephadex.brainflex.dto;

import java.time.LocalDateTime;
import java.util.List;

import cephadex.brainflex.model.DeckCollection;
import cephadex.brainflex.model.element.Image;
import cephadex.brainflex.model.enums.DeckVisibility;

public record DeckCollectionDTO(
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
        List<DeckDTO> decks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static DeckCollectionDTO summary(DeckCollection c) {
        return new DeckCollectionDTO(
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

    public static DeckCollectionDTO detail(DeckCollection c, List<DeckDTO> decks) {
        return new DeckCollectionDTO(
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
