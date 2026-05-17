/**
 * Answer for MCQ — the chosen option ids (stable across shuffles).
 *
 * Carries a list to support the question's `multipleSelections` setting:
 * with single-select, the list holds exactly one id; with multi-select, up
 * to N ids matching the question's cap. The scorer checks set membership
 * against the question's `correctOptionIds`.
 */
package cephadex.brainflex.model.answer;

import java.util.List;

public record McqAnswer(List<String> optionIds) implements AnswerPayload {
}
