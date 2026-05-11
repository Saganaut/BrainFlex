/**
 * Multiple-choice question with 2–6 options and a single correct answer.
 *
 * Scoring uses option id rather than index so the editor can shuffle the option
 * list freely (and clients can render in any order) without recomputing a
 * "correct index". The runtime sends options in whatever order they're stored;
 * if the deck author runs the editor's Shuffle button before saving, that's
 * the order everyone sees.
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
        String correctOptionId,        // must match one of options[].id
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
        return ElementKind.MCQ;
    }
}
