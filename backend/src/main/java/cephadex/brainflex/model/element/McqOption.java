/**
 * One option in an MCQ question.
 *
 * Stable per-option id lets clients submit the chosen option by id rather than
 * by index — so the editor / runtime can reorder options freely without
 * touching the `correctOptionIds` reference on the parent question.
 *
 * `image` carries an optional thumbnail. When `image.useExternalImg=false`
 * the backend rehydrates `image.imgUrl` from S3 on every read; clients render
 * `image.imgUrl` directly. See `Image` for the full contract.
 */
package cephadex.brainflex.model.element;

public record McqOption(
        String id,
        String text,
        Image image,
        String color
) {

    /** Returns a copy with `image` replaced — used by the read-time hydrator. */
    public McqOption withImage(Image image) {
        return new McqOption(id, text, image, color);
    }
}
