/**
 * Unified image reference used by every image-bearing field on a deck (option
 * thumbnails, slide media, element backgrounds, deck cover, grid backing,
 * place-on-image target, ranking item images, ...) and on user/theme records.
 *
 * Source of truth depends on `useExternalImg`:
 *   - true  → author pasted an external URL. `externalUrl` holds the literal
 *             URL and is persisted as-is. `internalImgId` is ignored and
 *             `variants` is empty.
 *   - false → image was chosen from the user's gallery (or uploaded as an
 *             avatar / theme image, where the entity id plays the same role).
 *             `internalImgId` is the GalleryImage id and is the source of
 *             truth; `variants` is transport-only — the hydrators refresh all
 *             five presigned URLs on every response. Whatever the client
 *             sends for `variants` on a write is discarded by the persistence
 *             layer. `externalUrl` is null.
 *
 * `variants` is a map keyed by `ImageSize` (XS/SM/MD/LG/XL) for internal
 * images. An empty map plus null `externalUrl` means "no image" and
 * renderers fall back to a Lorem Picsum placeholder seeded on the parent
 * element/option id.
 */
package cephadex.brainflex.model.image;

import java.util.EnumMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record Image(
        boolean useExternalImg,
        String internalImgId,
        String externalUrl,
        Map<ImageSize, ImageVariant> variants) {

    /** Replace `variants` (used by the read-time hydrators). */
    public Image withVariants(Map<ImageSize, ImageVariant> variants) {
        return new Image(useExternalImg, internalImgId, externalUrl, variants);
    }

    /** Externally-hosted URL (paste-link, picsum placeholder, etc.). */
    public static Image external(String url) {
        return new Image(true, null, url, Map.of());
    }

    /** Gallery-backed image. `variants` is populated by the hydrator on read. */
    public static Image internal(String galleryImageId) {
        return new Image(false, galleryImageId, null, Map.of());
    }

    /** Neutral "no image set" value. */
    public static Image empty() {
        return new Image(true, null, null, Map.of());
    }

    /** True when the image carries no renderable URL (no external URL and no
     *  hydrated variants with a usable URL). */
    public boolean isBlank() {
        if (useExternalImg) {
            return externalUrl == null || externalUrl.isBlank();
        }
        if (variants == null || variants.isEmpty()) return true;
        for (ImageVariant v : variants.values()) {
            if (v != null && v.url() != null && !v.url().isBlank()) return false;
        }
        return true;
    }

    /** Largest renderable URL we have. For external images this is the
     *  externalUrl; for internal images it walks XL→XS and returns the first
     *  hydrated variant. Null when the image is blank. */
    public String largestUrl() {
        if (useExternalImg) {
            return (externalUrl != null && !externalUrl.isBlank()) ? externalUrl : null;
        }
        if (variants == null || variants.isEmpty()) return null;
        ImageSize[] sizes = ImageSize.values();
        for (int i = sizes.length - 1; i >= 0; i--) {
            ImageVariant v = variants.get(sizes[i]);
            if (v != null && v.url() != null && !v.url().isBlank()) return v.url();
        }
        return null;
    }

    /** Build a fresh EnumMap-backed variants container. EnumMap preserves
     *  enum ordinal order in iteration, so hydrators that walk it produce
     *  xs→xl output naturally. */
    public static Map<ImageSize, ImageVariant> newVariants() {
        return new EnumMap<>(ImageSize.class);
    }
}
