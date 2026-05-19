package cephadex.brainflex.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import cephadex.brainflex.config.S3Properties;
import cephadex.brainflex.model.StoredImageVariant;
import cephadex.brainflex.model.element.ImageSize;
import cephadex.brainflex.model.element.ImageVariant;
import cephadex.brainflex.service.ImageProcessingService.ProcessedVariant;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/**
 * S3 access for the four image-bearing entities: gallery images, user
 * avatars, theme logos, theme backgrounds. Every uploaded image stores one
 * WebP rendition per {@link ImageSize} tier under deterministic keys, so
 * re-uploads always overwrite the same objects and read paths can refresh
 * the presigned URL for any rendition by id+size alone.
 *
 * Key shape: {@code <prefix>/<id>/<size>.webp} where {@code size} is the
 * lower-cased enum name (xs/sm/md/lg/xl). The prefix is per-entity.
 */
@Service
public class S3Service {

    private static final String GALLERY_PREFIX = "gallery-images";
    private static final String AVATAR_PREFIX = "profile-images";
    private static final String THEME_LOGO_PREFIX = "theme-logos";
    private static final String THEME_BG_PREFIX = "theme-backgrounds";

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties props;

    public S3Service(S3Client s3Client, S3Presigner s3Presigner, S3Properties props) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.props = props;
    }

    // ── Gallery images ────────────────────────────────────────────────────────

    public List<StoredImageVariant> uploadGalleryImage(String imageId, Map<ImageSize, ProcessedVariant> variants) {
        return uploadAll(GALLERY_PREFIX, imageId, variants);
    }

    public List<ImageVariant> refreshGalleryImage(String imageId, List<StoredImageVariant> stored) {
        return refreshAll(GALLERY_PREFIX, imageId, stored);
    }

    public void deleteGalleryImage(String imageId, List<StoredImageVariant> stored) {
        deleteAll(GALLERY_PREFIX, imageId, stored);
    }

    // ── Avatars ───────────────────────────────────────────────────────────────

    public List<StoredImageVariant> uploadAvatar(String userId, Map<ImageSize, ProcessedVariant> variants) {
        return uploadAll(AVATAR_PREFIX, userId, variants);
    }

    public List<ImageVariant> refreshAvatar(String userId, List<StoredImageVariant> stored) {
        return refreshAll(AVATAR_PREFIX, userId, stored);
    }

    // ── Theme logo ────────────────────────────────────────────────────────────

    public List<StoredImageVariant> uploadThemeLogo(String themeId, Map<ImageSize, ProcessedVariant> variants) {
        return uploadAll(THEME_LOGO_PREFIX, themeId, variants);
    }

    public List<ImageVariant> refreshThemeLogo(String themeId, List<StoredImageVariant> stored) {
        return refreshAll(THEME_LOGO_PREFIX, themeId, stored);
    }

    public void deleteThemeLogo(String themeId, List<StoredImageVariant> stored) {
        deleteAll(THEME_LOGO_PREFIX, themeId, stored);
    }

    // ── Theme background ──────────────────────────────────────────────────────

    public List<StoredImageVariant> uploadThemeBackground(String themeId, Map<ImageSize, ProcessedVariant> variants) {
        return uploadAll(THEME_BG_PREFIX, themeId, variants);
    }

    public List<ImageVariant> refreshThemeBackground(String themeId, List<StoredImageVariant> stored) {
        return refreshAll(THEME_BG_PREFIX, themeId, stored);
    }

    public void deleteThemeBackground(String themeId, List<StoredImageVariant> stored) {
        deleteAll(THEME_BG_PREFIX, themeId, stored);
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private List<StoredImageVariant> uploadAll(String prefix, String entityId,
            Map<ImageSize, ProcessedVariant> variants) {
        List<StoredImageVariant> stored = new ArrayList<>(variants.size());
        // Walk in enum order so the persisted list is xs→xl regardless of how
        // the producer built the map.
        for (ImageSize size : ImageSize.values()) {
            ProcessedVariant v = variants.get(size);
            if (v == null) continue;
            String key = variantKey(prefix, entityId, size);
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(props.bucket())
                            .key(key)
                            .contentType("image/webp")
                            .build(),
                    RequestBody.fromBytes(v.bytes()));
            stored.add(new StoredImageVariant(size, v.width(), v.height()));
        }
        return stored;
    }

    private List<ImageVariant> refreshAll(String prefix, String entityId, List<StoredImageVariant> stored) {
        if (stored == null || stored.isEmpty()) return List.of();
        List<ImageVariant> out = new ArrayList<>(stored.size());
        for (StoredImageVariant v : stored) {
            if (v == null || v.size() == null) continue;
            String url = generatePresignedUrl(variantKey(prefix, entityId, v.size()));
            out.add(new ImageVariant(v.size(), url, v.width(), v.height()));
        }
        return out;
    }

    private void deleteAll(String prefix, String entityId, List<StoredImageVariant> stored) {
        if (stored == null) return;
        for (StoredImageVariant v : stored) {
            if (v == null || v.size() == null) continue;
            deleteObject(variantKey(prefix, entityId, v.size()));
        }
    }

    public void deleteObject(String key) {
        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(props.bucket())
                        .key(key)
                        .build());
    }

    private static String variantKey(String prefix, String entityId, ImageSize size) {
        return prefix + "/" + entityId + "/" + size.name().toLowerCase(Locale.ROOT) + ".webp";
    }

    private String generatePresignedUrl(String key) {
        return s3Presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofSeconds(props.presignedUrlExpiry()))
                        .getObjectRequest(GetObjectRequest.builder()
                                .bucket(props.bucket())
                                .key(key)
                                .build())
                        .build()).url().toString();
    }
}
