/**
 * Multiple-choice question with 2 to 6 options and one OR MORE correct answers.
 *
 * Scoring uses option ids rather than indexes so the editor can shuffle the
 * option list freely (and clients can render in any order) without recomputing
 * a "correct index". `correctOptionIds` is the set of option ids that count as
 * correct — an MCQ may have any number (zero excludes it from scored game
 * modes; one is the typical case; many means "any of these is acceptable").
 *
 * When `multipleSelections` is non-null the player may submit up to that many
 * option ids; the McqAnswer payload carries `optionIds: List<String>` and the
 * scorer checks set membership against `correctOptionIds`.
 */
package cephadex.brainflex.model.element;

import java.util.List;
import java.util.Map;

import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.ElementKind;
import cephadex.brainflex.model.enums.MediaPosition;
import cephadex.brainflex.model.enums.ResponseMode;

public record McqQuestion(
        String id,
        String publicKey,
        String privateKey,
        String title,
        Map<String, Object> styledTitle,
        String prompt,
        List<McqOption> options,
        List<String> correctOptionIds, // ids of every option that counts as correct
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
        return ElementKind.MCQ;
    }
}
