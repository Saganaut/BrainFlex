// Persisted shape of one sized rendition. We deliberately do NOT store the
// S3 key on the document — keys are derived from the parent entity id and
// the size enum (e.g. `gallery-images/{id}/{size}.webp`) so a rename of the
// key scheme is a one-place change. Only the metadata callers need to render
// the URL (the size tier and the actual pixel dimensions after resize) lives
// here.
package cephadex.brainflex.model.media;

import cephadex.brainflex.model.image.ImageSize;

public record StoredImageVariant(
        ImageSize size,
        int width,
        int height) {
}
