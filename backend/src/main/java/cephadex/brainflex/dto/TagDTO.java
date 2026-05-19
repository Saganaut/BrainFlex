/**
 * API shapes for the /api/tags surface. {@link TagResponse} carries the
 * stored fields plus its immediate children, populated only on the single-tag
 * endpoint so list responses stay flat.
 */
package cephadex.brainflex.dto;

import java.time.LocalDateTime;
import java.util.List;

import cephadex.brainflex.model.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TagDTO {

    public record TagResponse(
            String id,
            String displayName,
            String parentTagId,
            String description,
            String iconUrl,
            int deckCount,
            boolean curated,
            List<TagResponse> children,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {

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
                    children,
                    tag.getCreatedAt(),
                    tag.getUpdatedAt());
        }
    }

    /**
     * When {@code id} is null/blank the server slugifies {@code displayName}.
     * Otherwise it must be kebab-case (validated server-side).
     */
    public record CreateTagRequest(
            @Size(max = 64) String id,
            @NotBlank @Size(max = 80) String displayName,
            @Size(max = 64) String parentTagId,
            @Size(max = 500) String description,
            @Size(max = 500) String iconUrl,
            Boolean curated) {
    }

    public record UpdateTagRequest(
            @Size(max = 80) String displayName,
            @Size(max = 64) String parentTagId,
            @Size(max = 500) String description,
            @Size(max = 500) String iconUrl,
            Boolean curated) {
    }
}
