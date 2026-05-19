// Wire types for the gallery API. Single response record carries everything
// the picker needs to render a tile (name, tags, current presigned URLs at
// every size tier) plus the ownership fields the client uses to gate the
// edit/delete affordances.
package cephadex.brainflex.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import cephadex.brainflex.model.GalleryImage;
import cephadex.brainflex.model.element.ImageVariant;

public class GalleryImageDTO {

    public record GalleryImageResponse(
            String id,
            String name,
            String ownerId,
            String organizationId,
            List<String> tags,
            List<ImageVariant> variants,
            LocalDateTime createdAt) {

        public GalleryImageResponse(GalleryImage image, List<ImageVariant> variants) {
            this(
                    image.getId(),
                    image.getName(),
                    image.getOwnerId(),
                    image.getOrganizationId(),
                    image.getTags() == null ? new ArrayList<>() : image.getTags(),
                    variants == null ? List.of() : variants,
                    image.getCreatedAt());
        }
    }

    public record UpdateGalleryImageRequest(
            String name,
            List<String> tags,
            String organizationId) {
    }
}
