/**
 * Submission for a MATCHING element. Maps each {@code MatchingPair.id} on the
 * left column to the {@code MatchingPair.id} the player paired it with on the
 * right column.
 *
 * Stored as id-to-id (not label-to-label) so the runtime can shuffle the right
 * column without invalidating answers.
 */
package cephadex.brainflex.model.answer;

import java.util.Map;

public record MatchingAnswer(Map<String, String> leftIdToRightId) implements AnswerPayload {
}
