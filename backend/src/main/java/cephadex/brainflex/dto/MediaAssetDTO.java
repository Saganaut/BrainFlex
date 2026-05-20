// Wire types for the media asset API. MediaAssetResponse carries the hydrated
// presentation: presigned URL list (image variants), single presigned URL
// (audio/video file), or the literal embed URL (video embed). Callers should
// always re-read after a mutation to pick up fresh URLs.
package cephadex.brainflex.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import cephadex.brainflex.model.MediaAsset;
import cephadex.brainflex.model.element.ImageVariant;
import cephadex.brainflex.model.enums.MediaKind;

public class MediaAssetDTO {

    /** Presentation shape: variants list for IMAGE, single url for AUDIO / VIDEO_FILE / VIDEO_EMBED. */
    public record MediaAssetResponse(
            String id,
            MediaKind kind,
            String name,
            String ownerId,
            String organizationId,
            List<ImageVariant> variants,
            String url,
            long sizeBytes,
            Integer width,
            Integer height,
            Integer durationMs,
            String mimeType,
            String altText,
            String attribution,
            String sourceUrl,
            List<String> tags,
            LocalDateTime createdAt) {

        public MediaAssetResponse(MediaAsset asset, List<ImageVariant> variants, String url) {
            this(
                    asset.getId(),
                    asset.getKind(),
                    asset.getName(),
                    asset.getOwnerId(),
                    asset.getOrganizationId(),
                    variants == null ? List.of() : variants,
                    url,
                    asset.getSizeBytes(),
                    asset.getWidth(),
                    asset.getHeight(),
                    asset.getDurationMs(),
                    asset.getMimeType(),
                    asset.getAltText(),
                    asset.getAttribution(),
                    asset.getSourceUrl(),
                    asset.getTags() == null ? new ArrayList<>() : asset.getTags(),
                    asset.getCreatedAt());
        }
    }

    /** PUT /api/media/{id}. Null fields are left unchanged; empty organizationId clears sharing. */
    public record UpdateMediaAssetRequest(
            String name,
            List<String> tags,
            String organizationId,
            String altText,
            String attribution,
            String sourceUrl) {
    }

    /** POST /api/media/embed. URL is normalised to an iframe-ready form on the backend. */
    public record CreateEmbedRequest(
            String url,
            String name,
            List<String> tags,
            String organizationId) {
    }
}
