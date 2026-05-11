/**
 * One option in an MCQ or IMAGE_CHOICE question.
 *
 * Stable per-option id lets clients submit the chosen option by id rather than
 * by index — so the editor / runtime can reorder options freely without
 * touching the `correctOptionId` reference on the parent question.
 */
package cephadex.brainflex.model.element;

public record McqOption(
        String id,
        String text,
        String imageUrl
) {
}
