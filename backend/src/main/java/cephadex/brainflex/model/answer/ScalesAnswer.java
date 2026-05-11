/** Map of statementId → chosen rating (within [scaleMin, scaleMax]). */
package cephadex.brainflex.model.answer;

import java.util.Map;

public record ScalesAnswer(Map<String, Integer> ratings) implements AnswerPayload {
}
