// Wire type for the media asset API. Carries the hydrated presentation:
// presigned URL map (image variants), single presigned URL (audio/video file),
// or the literal embed URL (video embed). Callers should always re-read after
// a mutation to pick up fresh URLs.
package cephadex.brainflex.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import cephadex.brainflex.model.enums.MediaKind;
import cephadex.brainflex.model.image.ImageSize;
import cephadex.brainflex.model.image.ImageVariant;
import cephadex.brainflex.model.media.MediaAsset;

/**
 * Presentation shape: variants map for IMAGE, single url for AUDIO / VIDEO_FILE
 * / VIDEO_EMBED.
 */
public record MediaAssetResponse(
        String id,
        MediaKind kind,
        String name,
        String ownerId,
        String organizationId,
        Map<ImageSize, ImageVariant> variants,
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
        Instant createdAt) {

    public MediaAssetResponse(MediaAsset asset, Map<ImageSize, ImageVariant> variants, String url) {
        this(
                asset.getId(),
                asset.getKind(),
                asset.getName(),
                asset.getOwnerId(),
                asset.getOrganizationId(),
                variants == null ? Collections.emptyMap() : variants,
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
