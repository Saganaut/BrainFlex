/**
 * Numeric guess with an optional tolerance window. Within tolerance counts as
 * correct (binary scoring for now). Results render as a histogram of player
 * guesses around the correct value.
 *
 * Optional `minValue` / `maxValue` clamp the input range — payloads outside
 * the range are rejected by the scorer (no credit, distinct from a "wrong
 * answer"). `allowNegative` is a client-side flag for the input control; the
 * scorer ignores it (range bounds are the authoritative gate).
 */
package cephadex.brainflex.model.element;

import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.ElementKind;

public record NumberQuestion(
        String id,
        String prompt,
        double correctValue,
        double tolerance,           // |submitted - correct| <= tolerance counts as correct
        String unitLabel,           // display only (e.g. " planets", " km", "$")
        int decimalPlaces,          // 0 = integer entry; 2 = currency-ish
        // scoring (question-only — not chrome)
        int pointValue,
        Difficulty difficulty,
        String explanation,
        // per-kind ergonomics (chunk 10)
        Double minValue,
        Double maxValue,
        boolean allowNegative,
        // shared chrome (chunk 25)
        ElementChrome chrome
) implements DeckElement {

    @Override
    public ElementKind kind() {
        return ElementKind.NUMBER;
    }
}
