/**
 * Unified image reference used by every image-bearing field on a deck (option
 * thumbnails, slide media, element backgrounds, deck cover, grid backing,
 * place-on-image target, ranking item images, ...).
 *
 * Source of truth depends on `useExternalImg`:
 *   - true  → author pasted an external URL. `imgUrl` is the literal URL to
 *             render and is persisted as-is. `internalImgId` is ignored.
 *   - false → image was chosen from the user's gallery. `internalImgId` is
 *             the GalleryImage id and is the source of truth; `imgUrl` is a
 *             transport-only presigned URL that DeckImageHydrationService
 *             refreshes on every response. Whatever the client sends for
 *             `imgUrl` on a write is discarded by DeckService before save.
 *
 * Both `imgUrl=null` and `imgUrl=""` mean "no image" — renderers fall back
 * to a Lorem Picsum placeholder seeded on the parent element/option id.
 */
package cephadex.brainflex.model.element;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record Image(
        boolean useExternalImg,
        String internalImgId,
        String imgUrl
) {

    /** Replace `imgUrl` (used by the read-time hydrator). */
    public Image withImgUrl(String url) {
        return new Image(useExternalImg, internalImgId, url);
    }

    /** Externally-hosted URL (paste-link, picsum placeholder, etc.). */
    public static Image external(String url) {
        return new Image(true, null, url);
    }

    /** Gallery-backed image. `imgUrl` is populated by the hydrator on read. */
    public static Image internal(String galleryImageId) {
        return new Image(false, galleryImageId, null);
    }

    /** Neutral "no image set" value. */
    public static Image empty() {
        return new Image(true, null, null);
    }

    /** True when the image carries no renderable URL. */
    public boolean isBlank() {
        return imgUrl == null || imgUrl.isBlank();
    }
}
