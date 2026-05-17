/**
 * One option in an MCQ question.
 *
 * Stable per-option id lets clients submit the chosen option by id rather than
 * by index — so the editor / runtime can reorder options freely without
 * touching the `correctOptionIds` reference on the parent question.
 *
 * Image sources are mutually exclusive (validated on save in DeckService):
 *   - `galleryImageId` references a GalleryImage in the gallery_images
 *     collection; on read, DeckImageHydrationService refreshes `imageUrl`
 *     into a fresh presigned URL so the renderer always has one URL to use.
 *   - `imageUrl` holds an externally-hosted URL (paste-link, picsum
 *     placeholder, etc.); it is rendered as-is.
 * At most one may be set on a write; both null means "no image".
 */
package cephadex.brainflex.model.element;

public record McqOption(
        String id,
        String text,
        String galleryImageId,
        String imageUrl,
        String color
) {

    /** Returns a copy with `imageUrl` replaced — used by the read-time hydrator. */
    public McqOption withImageUrl(String url) {
        return new McqOption(id, text, galleryImageId, url, color);
    }
}
