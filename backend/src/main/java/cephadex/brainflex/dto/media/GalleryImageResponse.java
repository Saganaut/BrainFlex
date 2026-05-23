// Wire type for the gallery API. Carries everything the picker needs to render
// a tile (name, tags, current presigned URLs at every size tier) plus the
// ownership fields the client uses to gate the edit/delete affordances.
package cephadex.brainflex.dto.media;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import cephadex.brainflex.model.media.GalleryImage;
import cephadex.brainflex.model.image.ImageSize;
import cephadex.brainflex.model.image.ImageVariant;

public record GalleryImageResponse(
        String id,
        String name,
        String ownerId,
        String organizationId,
        List<String> tags,
        Map<ImageSize, ImageVariant> variants,
        Instant createdAt) {

    public GalleryImageResponse(GalleryImage image, Map<ImageSize, ImageVariant> variants) {
        this(
                image.getId(),
                image.getName(),
                image.getOwnerId(),
                image.getOrganizationId(),
                image.getTags() == null ? new ArrayList<>() : image.getTags(),
                variants == null ? Collections.emptyMap() : variants,
                image.getCreatedAt());
    }
}
