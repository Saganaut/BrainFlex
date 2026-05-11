/**
 * Public-facing representation of a Deck shown in the deck-browse / create UI.
 * Omits internal fields like generatorType and creatorUserId that are not
 * relevant to players browsing available packs.
 */
package cephadex.brainflex.dto;

import java.time.LocalDateTime;

import cephadex.brainflex.model.Deck;

public record DeckDTO(
        String id,
        String name,
        String description,
        String category,
        int questionCount,
        boolean isSystem,
        String coverImageUrl,
        String backgroundImageUrl,
        LocalDateTime createdAt) {

    public DeckDTO(Deck pack) {
        this(
                pack.getId(),
                pack.getName(),
                pack.getDescription(),
                pack.getCategory(),
                pack.getQuestionCount(),
                pack.isSystem(),
                pack.getCoverImageUrl(),
                pack.getBackgroundImageUrl(),
                pack.getCreatedAt());
    }
}
