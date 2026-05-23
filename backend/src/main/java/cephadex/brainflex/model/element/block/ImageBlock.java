/**
 * Image block with optional caption. Holds an {@link Image} — same gallery /
 * external URL contract as MCQ-option / RankingItem / matching-pair images.
 * {@link cephadex.brainflex.service.DeckImageMapper} walks blocks so hydration
 * and S3-url rewriting reach every {@code image} reference automatically.
 */
package cephadex.brainflex.model.element.block;

import cephadex.brainflex.model.enums.SlideBlockKind;
import cephadex.brainflex.model.image.Image;
import cephadex.brainflex.model.image.ImageContainer;

public record ImageBlock(
        String id,
        Image image,
        String caption
) implements SlideBlock, ImageContainer {

    @Override
    public SlideBlockKind kind() {
        return SlideBlockKind.IMAGE;
    }

    @Override
    public ImageBlock withImage(String fieldName, Image value) {
        return switch (fieldName) {
            case "image" -> new ImageBlock(id, value, caption);
            default -> throw new IllegalArgumentException(
                    "ImageBlock has no image field named '" + fieldName + "'");
        };
    }
}
