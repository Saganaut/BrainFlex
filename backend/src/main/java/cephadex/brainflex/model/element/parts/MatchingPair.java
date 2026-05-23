/**
 * One correct pair in a MatchingQuestion. Stable per-pair id keeps the answer
 * payload small (left id to right id) and lets the editor / renderer reorder
 * the visible pair list without rewriting any references.
 *
 * Either side may carry text, an image, or both — same Image contract as MCQ
 * options (gallery-backed or external URL, hydrated read-time).
 */
package cephadex.brainflex.model.element.parts;

import cephadex.brainflex.model.image.Image;
import cephadex.brainflex.model.image.ImageContainer;

public record MatchingPair(
        String id,
        String leftLabel,
        String rightLabel,
        Image leftImage,
        Image rightImage
) implements ImageContainer {

    @Override
    public MatchingPair withImage(String fieldName, Image value) {
        return switch (fieldName) {
            case "leftImage" -> new MatchingPair(id, leftLabel, rightLabel, value, rightImage);
            case "rightImage" -> new MatchingPair(id, leftLabel, rightLabel, leftImage, value);
            default -> throw new IllegalArgumentException(
                    "MatchingPair has no image field named '" + fieldName + "'");
        };
    }
}
