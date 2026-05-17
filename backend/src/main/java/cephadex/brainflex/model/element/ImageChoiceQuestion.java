/**
 * Choose-from-images variant of MCQ. Same data shape — each option carries an
 * imageUrl that the renderer uses as the primary visual (text becomes a caption).
 *
 * Like McqQuestion, `correctOptionIds` is the set of option ids that count as
 * correct; when `multipleSelections` is non-null the player may submit up to
 * that many option ids.
 */
package cephadex.brainflex.model.element;

import java.util.List;
import java.util.Map;

import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.ElementKind;
import cephadex.brainflex.model.enums.MediaPosition;
import cephadex.brainflex.model.enums.ResponseMode;

public record ImageChoiceQuestion(
        String id,
        String publicKey,
        String privateKey,
        String title,
        Map<String, Object> styledTitle,
        String prompt,
        List<McqOption> options,
        List<String> correctOptionIds,
        // scoring
        int pointValue,
        Difficulty difficulty,
        boolean scored,
        boolean survey,
        Integer multipleSelections,
        ResponseMode responseMode,
        // best-answer modifier
        boolean bestAnswerMode,
        String bestAnswerTitle,
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
