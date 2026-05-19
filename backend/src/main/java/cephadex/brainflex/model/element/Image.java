/**
 * Unified image reference used by every image-bearing field on a deck (option
 * thumbnails, slide media, element backgrounds, deck cover, grid backing,
 * place-on-image target, ranking item images, ...) and on user/theme records.
 *
 * Source of truth depends on `useExternalImg`:
 *   - true  → author pasted an external URL. The single entry in `variants`
 *             holds the literal URL and is persisted as-is. `internalImgId`
 *             is ignored.
 *   - false → image was chosen from the user's gallery (or uploaded as an
 *             avatar / theme image, where the entity id plays the same role).
 *             `internalImgId` is the GalleryImage id and is the source of
 *             truth; `variants` is transport-only — DeckImageHydrationService
 *             (and the equivalent paths for avatars/themes) refreshes all five
 *             presigned URLs on every response. Whatever the client sends for
 *             `variants` on a write is discarded by the persistence layer.
 *
 * `variants` always carries one renderable URL per ImageSize tier (xs/sm/md/
 * lg/xl) for internal images, and exactly one entry for external images
 * (whose actual size is unknown). An empty `variants` list means "no image"
 * and renderers fall back to a Lorem Picsum placeholder seeded on the parent
 * element/option id.
 */
package cephadex.brainflex.model.element;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record Image(
        boolean useExternalImg,
        String internalImgId,
        List<ImageVariant> variants) {

    /** Replace `variants` (used by the read-time hydrator). */
    public Image withVariants(List<ImageVariant> variants) {
        return new Image(useExternalImg, internalImgId, variants);
    }

    /** Externally-hosted URL (paste-link, picsum placeholder, etc.). One
     *  variant, size unknown — we don't fetch the URL to measure it. */
    public static Image external(String url) {
        return new Image(true, null, List.of(new ImageVariant(null, url, 0, 0)));
    }

    /** Gallery-backed image. `variants` is populated by the hydrator on read. */
    public static Image internal(String galleryImageId) {
        return new Image(false, galleryImageId, List.of());
    }

    /** Neutral "no image set" value. */
    public static Image empty() {
        return new Image(true, null, List.of());
    }

    /** True when the image carries no renderable variants. */
    public boolean isBlank() {
        if (variants == null || variants.isEmpty()) return true;
        for (ImageVariant v : variants) {
            if (v != null && v.url() != null && !v.url().isBlank()) return false;
        }
        return true;
    }

    /** Largest available variant (xl when present, else the last in the list).
     *  Used by renderers that want the highest-quality URL available. */
    public ImageVariant largestVariant() {
        if (variants == null || variants.isEmpty()) return null;
        ImageVariant best = null;
        for (ImageVariant v : variants) {
            if (v == null || v.url() == null || v.url().isBlank()) continue;
            if (best == null) { best = v; continue; }
            ImageSize bestSize = best.size();
            ImageSize candidateSize = v.size();
            if (bestSize == null) { best = v; continue; }
            if (candidateSize != null && candidateSize.ordinal() > bestSize.ordinal()) best = v;
        }
        return best;
    }
}
