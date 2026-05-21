/**
 * Free-text question. The submitted text is matched case-insensitively (unless
 * caseSensitive=true) against the canonical answer or any acceptedVariants.
 * Trimming is governed by `trimWhitespace` (default true). When `fuzzyMatch`
 * is enabled the scorer also accepts answers within `fuzzyDistance` Levenshtein
 * edits of the canonical or any variant — useful for typos and minor spelling
 * slips. `maxLength` is a client-side cap on the input (default 80).
 */
package cephadex.brainflex.model.element;

import java.util.List;

import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.ElementKind;

public record TextQuestion(
        String id,
        String prompt,
        String correctAnswer,
        List<String> acceptedVariants,   // additional strings that also count as correct
        boolean caseSensitive,
        // scoring (question-only — not chrome)
        int pointValue,
        Difficulty difficulty,
        String explanation,
        // per-kind ergonomics (chunk 10)
        int maxLength,
        boolean trimWhitespace,
        boolean fuzzyMatch,
        int fuzzyDistance,
        // shared chrome (chunk 25)
        ElementChrome chrome
) implements DeckElement {

    @Override
    public ElementKind kind() {
        return ElementKind.TEXT;
    }
}
