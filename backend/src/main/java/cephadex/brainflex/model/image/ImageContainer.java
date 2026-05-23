/**
 * Implemented by every record that owns one or more {@link Image} components,
 * so {@link cephadex.brainflex.service.DeckImageMapper} (and any future
 * read-time / migration / normalization pass) can replace any image slot on
 * any container with a single uniform call, identifying the slot by the
 * record-component name.
 *
 * Adopted in §1f of {@code z-docs/to-do/java-model-issues.md} to retire the
 * one-off per-record withers ({@code withImage}, {@code withLeftImage},
 * {@code withRightImage}, {@code withBackingImage}, ...) that were trivially
 * different signatures of the same operation.
 *
 * Implementations match {@code fieldName} against their declared image
 * components and throw {@link IllegalArgumentException} for an unknown name —
 * a typo is a programmer error, not a runtime data condition.
 *
 * The return type is widened to {@code ImageContainer}, but every
 * implementation declares a covariant return of its own concrete type, so
 * callers operating on the concrete type keep the precise return without a
 * cast.
 *
 * Note: {@link cephadex.brainflex.model.element.ElementChrome} carries the
 * element-level {@code background} + {@code image} slots and keeps its own
 * atomic {@code withImages(Image, Image)} setter — it is deliberately not
 * part of this interface so the chrome's two slots stay paired in one
 * allocation.
 */
package cephadex.brainflex.model.image;

public interface ImageContainer {

    /** Returns a copy of this container with the image at {@code fieldName}
     *  replaced. {@code fieldName} matches the record-component name on the
     *  implementing type (e.g. {@code "image"}, {@code "leftImage"},
     *  {@code "backingImage"}). Throws {@link IllegalArgumentException} when
     *  the implementing type has no image slot by that name. */
    ImageContainer withImage(String fieldName, Image image);
}
