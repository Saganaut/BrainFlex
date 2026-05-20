/**
 * Image block with optional caption. Holds an {@link Image} — same gallery /
 * external URL contract as MCQ-option / RankingItem / matching-pair images.
 * {@link cephadex.brainflex.service.DeckImageMapper} walks blocks so hydration
 * and S3-url rewriting reach every {@code image} reference automatically.
 */
package cephadex.brainflex.model.element;

import cephadex.brainflex.model.enums.SlideBlockKind;

public record ImageBlock(
        String id,
        Image image,
        String caption
) implements SlideBlock {

    @Override
    public SlideBlockKind kind() {
        return SlideBlockKind.IMAGE;
    }

    /** Returns a copy with `image` replaced — used by the read-time hydrator. */
    public ImageBlock withImage(Image image) {
        return new ImageBlock(id, image, caption);
    }
}
