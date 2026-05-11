/**
 * Free-text question. The submitted text is matched case-insensitively (unless
 * caseSensitive=true) against the canonical answer or any acceptedVariants, all
 * compared after trimming whitespace.
 */
package cephadex.brainflex.model.element;

import java.util.List;

import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.ElementKind;
import cephadex.brainflex.model.enums.MediaPosition;

public record TextQuestion(
        String id,
        String prompt,
        String correctAnswer,
        List<String> acceptedVariants,   // additional strings that also count as correct
        boolean caseSensitive,
        // scoring
        int pointValue,
        Difficulty difficulty,
        // best-answer modifier
        boolean bestAnswerMode,
        int bestAnswerBonus,
        String explanation,
        // shared chrome
        int displaySeconds,
        String hostNotes,
        String backgroundImageUrl,
        String imageUrl,
        String videoUrl,
        String audioUrl,
        MediaPosition mediaPosition
) implements DeckElement {

    @Override
    public ElementKind kind() {
        return ElementKind.TEXT;
    }
}
