/** Ordered list of item ids — the player's attempt at the correct sequence. */
package cephadex.brainflex.model.answer;

import java.util.List;

public record RankingAnswer(List<String> orderedItemIds) implements AnswerPayload {
}
