/**
 * Multiple-choice question with 2–6 options and one OR MORE correct answers.
 *
 * Scoring uses option ids rather than indexes so the editor can shuffle the
 * option list freely (and clients can render in any order) without recomputing
 * a "correct index". `correctOptionIds` is the set of option ids that count as
 * correct — an MCQ may have any number (zero excludes it from scored game
 * modes; one is the typical case; many means "any of these is acceptable").
 */
package cephadex.brainflex.model.element;

import java.util.List;

import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.ElementKind;
import cephadex.brainflex.model.enums.MediaPosition;

public record McqQuestion(
        String id,
        String prompt,
        List<McqOption> options,
        List<String> correctOptionIds, // ids of every option that counts as correct
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
        return ElementKind.MCQ;
    }
}
