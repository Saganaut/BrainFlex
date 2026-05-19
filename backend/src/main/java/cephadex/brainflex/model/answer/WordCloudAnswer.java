/**
 * Submission for a WORD_CLOUD element. Each player may submit up to
 * {@code WordCloudQuestion.maxSubmissionsPerPlayer} short words/phrases per
 * round; the aggregator normalizes (trim, lower-case, banned-words drop) and
 * tallies them across all players.
 */
package cephadex.brainflex.model.answer;

import java.util.List;

public record WordCloudAnswer(List<String> words) implements AnswerPayload {
}
