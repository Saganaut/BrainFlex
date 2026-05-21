/**
 * Likert-style: rate each statement on a numeric scale. Default unscored
 * (Pulse use case — show the distribution). When `scored=true` and
 * `correctRatings` is provided, the question becomes "guess the average
 * rating" — submitted rating compared against the correct one per statement.
 *
 * `scored` lives on {@link ElementChrome} — same semantic, just lifted to
 * shared chrome so every element kind exposes it uniformly.
 */
package cephadex.brainflex.model.element;

import java.util.List;

import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.ElementKind;

public record ScalesQuestion(
        String id,
        String prompt,
        List<ScaleStatement> statements,
        int scaleMin,                  // typically 1
        int scaleMax,                  // typically 5 or 7
        String minLabel,               // anchor text, e.g. "Strongly disagree"
        String maxLabel,               // anchor text, e.g. "Strongly agree"
        List<Integer> correctRatings,  // when scored: one rating per statement, in order
        // scoring (question-only — not chrome)
        int pointValue,
        Difficulty difficulty,
        String explanation,
        // shared chrome (chunk 25)
        ElementChrome chrome
) implements DeckElement {

    @Override
    public ElementKind kind() {
        return ElementKind.SCALES;
    }
}
