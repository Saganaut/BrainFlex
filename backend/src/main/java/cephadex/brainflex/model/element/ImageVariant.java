// One sized rendition of an image. Every Image carries a list of these so
// renderers can pick the right resolution for the slot they're filling
// (xs for tiny inline thumbnails, xl for the full-screen preview).
//
// `size` is null only for legacy or external images whose dimensions we
// don't know. `width`/`height` are 0 when unknown. `url` is always either a
// fresh presigned S3 URL (internal images, regenerated on read) or the
// literal author-pasted URL (external images, persisted as-is).
package cephadex.brainflex.model.element;

public record ImageVariant(
        ImageSize size,
        String url,
        int width,
        int height) {
}
