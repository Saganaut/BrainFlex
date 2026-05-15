/**
 * Choose-from-images variant of MCQ. Same data shape — each option carries an
 * imageUrl that the renderer uses as the primary visual (text becomes a caption).
 */
package cephadex.brainflex.model.element;

import java.util.List;

import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.ElementKind;
import cephadex.brainflex.model.enums.MediaPosition;

public record ImageChoiceQuestion(
        String id,
        String prompt,
        List<McqOption> options,
        String correctOptionId,
        // scoring
        int pointValue,
        Difficulty difficulty,
        // best-answer modifier
        boolean bestAnswerMode,
        int bestAnswerBonus,
        String explanation,
        // shared chrome
        int displaySeconds,
        String speakerNotes,
        String backgroundImageUrl,
        String imageUrl,
        String videoUrl,
        String audioUrl,
        MediaPosition mediaPosition
) implements DeckElement {

    @Override
    public ElementKind kind() {
        return ElementKind.IMAGE_CHOICE;
    }
}
