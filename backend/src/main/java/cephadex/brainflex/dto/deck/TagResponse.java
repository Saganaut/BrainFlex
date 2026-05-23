// Wire shape for the /api/tags surface. Carries the stored fields plus the
// immediate children, populated only on the single-tag endpoint so list
// responses stay flat.
package cephadex.brainflex.dto.deck;

import java.time.Instant;
import java.util.List;

import cephadex.brainflex.model.deck.Tag;

public record TagResponse(
        String id,
        String displayName,
        String parentTagId,
        String description,
        String iconUrl,
        int deckCount,
        boolean curated,
        String createdByUserId,
        List<TagResponse> children,
        Instant createdAt,
        Instant updatedAt) {

    public TagResponse(Tag tag) {
        this(tag, List.of());
    }

    public TagResponse(Tag tag, List<TagResponse> children) {
        this(
                tag.getId(),
                tag.getDisplayName(),
                tag.getParentTagId(),
                tag.getDescription(),
                tag.getIconUrl(),
                tag.getDeckCount(),
                tag.isCurated(),
                tag.getCreatedByUserId(),
                children,
                tag.getCreatedAt(),
                tag.getUpdatedAt());
    }
}
