/**
 * One correct pair in a MatchingQuestion. Stable per-pair id keeps the answer
 * payload small (left id to right id) and lets the editor / renderer reorder
 * the visible pair list without rewriting any references.
 *
 * Either side may carry text, an image, or both — same Image contract as MCQ
 * options (gallery-backed or external URL, hydrated read-time).
 */
package cephadex.brainflex.model.element;

public record MatchingPair(
        String id,
        String leftLabel,
        String rightLabel,
        Image leftImage,
        Image rightImage
) {

    public MatchingPair withLeftImage(Image leftImage) {
        return new MatchingPair(id, leftLabel, rightLabel, leftImage, rightImage);
    }

    public MatchingPair withRightImage(Image rightImage) {
        return new MatchingPair(id, leftLabel, rightLabel, leftImage, rightImage);
    }
}
