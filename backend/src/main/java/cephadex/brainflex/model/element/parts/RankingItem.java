/**
 * One item in a RankingQuestion. Stable per-item id means the editor / runtime
 * can shuffle items for presentation without rewriting `correctOrder`.
 */
package cephadex.brainflex.model.element.parts;

import cephadex.brainflex.model.image.Image;
import cephadex.brainflex.model.image.ImageContainer;

public record RankingItem(
        String id,
        String label,
        Image image
) implements ImageContainer {

    @Override
    public RankingItem withImage(String fieldName, Image value) {
        return switch (fieldName) {
            case "image" -> new RankingItem(id, label, value);
            default -> throw new IllegalArgumentException(
                    "RankingItem has no image field named '" + fieldName + "'");
        };
    }
}
