/**
 * Pick cells in an N×M grid. Two scoring modes via `multipleCorrect`:
 *   false — player picks one cell, correct iff that cell is in correctCellIndexes
 *   true  — player picks any subset, correct iff the subset equals correctCellIndexes
 *
 * Cell indexes are row-major (top-left = 0, top-right = cols-1, next row = cols).
 */
package cephadex.brainflex.model.element;

import java.util.Set;

import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.ElementKind;
import cephadex.brainflex.model.element.parts.GridCellsConfig;

public record GridQuestion(
        String id,
        String prompt,
        int rows,
        int cols,
        GridCellsConfig cells,
        Set<Integer> correctCellIndexes,
        boolean multipleCorrect,
        // scoring (question-only — not chrome)
        int pointValue,
        Difficulty difficulty,
        String explanation,
        // shared chrome (chunk 25)
        ElementChrome chrome
) implements DeckElement {

    @Override
    public ElementKind kind() {
        return ElementKind.GRID;
    }
}
