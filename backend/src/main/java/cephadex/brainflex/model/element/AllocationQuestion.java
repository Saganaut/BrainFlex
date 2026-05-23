/**
 * Mentimeter-style "100 Points" survey. Players distribute a fixed pool of
 * points across N options to express weighted preferences.
 *
 * Never scored by contract ({@code scored=false}, {@code survey=true} — both
 * on {@link ElementChrome}). Reuses {@link McqOption} so authors get the same
 * option-list editor (text + optional thumbnail + color) used for MCQ.
 *
 * {@code totalPointsToDistribute} caps each player's distribution (default 100).
 * {@code allowZeroOnItem} controls whether a per-option score of zero is valid.
 * {@code enforceExactTotal=true} rejects submissions whose sum != the pool;
 * {@code =false} lets partial distributions through (server clamps overruns).
 */
package cephadex.brainflex.model.element;

import java.util.List;

import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.ElementKind;
import cephadex.brainflex.model.element.parts.McqOption;

public record AllocationQuestion(
        String id,
        String prompt,
        List<McqOption> options,
        int totalPointsToDistribute,   // default 100
        boolean allowZeroOnItem,       // default true
        boolean enforceExactTotal,     // default true
        // scoring (question-only — not chrome; always 0 / unscored)
        int pointValue,
        Difficulty difficulty,
        String explanation,
        // shared chrome (chunk 25)
        ElementChrome chrome
) implements DeckElement {

    @Override
    public ElementKind kind() {
        return ElementKind.ALLOCATION;
    }
}
