// Wire types for the gallery API. Single response record carries everything
// the picker needs to render a tile (name, tags, current presigned URL) plus
// the ownership fields the client uses to gate the edit/delete affordances.
package cephadex.brainflex.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import cephadex.brainflex.model.GalleryImage;

public class GalleryImageDTO {

    public record GalleryImageResponse(
            String id,
            String name,
            String ownerId,
            String organizationId,
            List<String> tags,
            String imageUrl,
            LocalDateTime createdAt) {

        public GalleryImageResponse(GalleryImage image) {
            this(
                    image.getId(),
                    image.getName(),
                    image.getOwnerId(),
                    image.getOrganizationId(),
                    image.getTags() == null ? new ArrayList<>() : image.getTags(),
                    image.getImageUrl(),
                    image.getCreatedAt());
        }
    }

    public record UpdateGalleryImageRequest(
            String name,
            List<String> tags,
            String organizationId) {
    }
}
