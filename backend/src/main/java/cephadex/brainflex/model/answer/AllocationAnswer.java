/**
 * Submission for an ALLOCATION element. Maps each option id to the number of
 * points the player chose to assign to that option.
 *
 * The aggregation step averages per-option points across all submissions
 * for the reveal-phase bar chart.
 */
package cephadex.brainflex.model.answer;

import java.util.Map;

public record AllocationAnswer(Map<String, Integer> optionIdToPoints) implements AnswerPayload {
}
