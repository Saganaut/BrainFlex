// One sized rendition of an image. Carried as the value of the map on
// `Image.variants`, keyed by `ImageSize` — the key is the source of truth
// for the tier, so this record only stores the renderable URL and the
// actual pixel dimensions of the resized asset (which may be smaller than
// the tier's nominal target if the source was smaller).
//
// `url` is always a fresh presigned S3 URL (regenerated on every read by
// the hydrators). `width`/`height` are 0 only when unknown.
package cephadex.brainflex.model.element;

public record ImageVariant(
        String url,
        int width,
        int height) {
}
