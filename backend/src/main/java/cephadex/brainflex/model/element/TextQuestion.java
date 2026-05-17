/**
 * Free-text question. The submitted text is matched case-insensitively (unless
 * caseSensitive=true) against the canonical answer or any acceptedVariants, all
 * compared after trimming whitespace.
 */
package cephadex.brainflex.model.element;

import java.util.List;
import java.util.Map;

import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.ElementKind;
import cephadex.brainflex.model.enums.MediaPosition;
import cephadex.brainflex.model.enums.ResponseMode;

public record TextQuestion(
        String id,
        String publicKey,
        String privateKey,
        String title,
        Map<String, Object> styledTitle,
        String prompt,
        String correctAnswer,
        List<String> acceptedVariants,   // additional strings that also count as correct
        boolean caseSensitive,
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
        return ElementKind.TEXT;
    }
}
