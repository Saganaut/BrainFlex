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
package cephadex.brainflex.model.element.parts;

import cephadex.brainflex.model.image.Image;
import cephadex.brainflex.model.image.ImageContainer;

public record McqOption(
        String id,
        String text,
        Image image,
        String color
) implements ImageContainer {

    @Override
    public McqOption withImage(String fieldName, Image value) {
        return switch (fieldName) {
            case "image" -> new McqOption(id, text, value, color);
            default -> throw new IllegalArgumentException(
                    "McqOption has no image field named '" + fieldName + "'");
        };
    }
}
