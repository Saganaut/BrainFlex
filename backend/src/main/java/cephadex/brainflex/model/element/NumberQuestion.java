/**
 * Numeric guess with an optional tolerance window. Within tolerance counts as
 * correct (binary scoring for now). Results render as a histogram of player
 * guesses around the correct value.
 */
package cephadex.brainflex.model.element;

import java.util.Map;

import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.ElementKind;
import cephadex.brainflex.model.enums.MediaPosition;
import cephadex.brainflex.model.enums.ResponseMode;

public record NumberQuestion(
        String id,
        String publicKey,
        String privateKey,
        String title,
        Map<String, Object> styledTitle,
        String prompt,
        double correctValue,
        double tolerance,           // |submitted - correct| <= tolerance counts as correct
        String unitLabel,           // display only (e.g. " planets", " km", "$")
        int decimalPlaces,          // 0 = integer entry; 2 = currency-ish
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
        Image background,
        Image image,
        String videoUrl,
        String audioUrl,
        MediaPosition mediaPosition
) implements DeckElement {

    @Override
    public ElementKind kind() {
        return ElementKind.NUMBER;
    }
}
