/**
 * Kahoot-style "puzzle pairs" / Mentimeter "Word Match". Players pair items
 * between two columns; the authoritative correct pairing lives on
 * {@code pairs} (each {@link MatchingPair} declares one left/right twin).
 *
 * The runtime shuffles the right column independently of {@code pairs}
 * ordering before broadcasting (see {@link cephadex.brainflex.service.ElementRedactor})
 * so the answer key isn't deducible from positional order; scoring still
 * matches by pair id, so reorders never affect correctness.
 *
 * Scoring follows {@code scoring}: ALL_OR_NOTHING is binary (every pair
 * correct → full points), PARTIAL pro-rates by the count of correct pairings.
 */
package cephadex.brainflex.model.element;

import java.util.List;

import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.ElementKind;
import cephadex.brainflex.model.enums.MatchingScoring;
import cephadex.brainflex.model.element.parts.MatchingPair;

public record MatchingQuestion(
        String id,
        String prompt,
        List<MatchingPair> pairs,
        MatchingScoring scoring,
        // scoring (question-only — not chrome)
        int pointValue,
        Difficulty difficulty,
        String explanation,
        // shared chrome (chunk 25)
        ElementChrome chrome
) implements DeckElement {

    @Override
    public ElementKind kind() {
        return ElementKind.MATCHING;
    }
}
