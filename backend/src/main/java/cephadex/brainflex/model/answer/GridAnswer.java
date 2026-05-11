/** Set of selected cell indexes (row-major) for a GRID question. */
package cephadex.brainflex.model.answer;

import java.util.Set;

public record GridAnswer(Set<Integer> selectedCellIndexes) implements AnswerPayload {
}
