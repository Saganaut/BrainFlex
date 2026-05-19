/**
 * Submission for a DRAWING element. Carries the player's full stroke list;
 * never scored — the host may run a Best Answer vote on top to crown a
 * favourite, but the base scorer always returns ZERO.
 *
 * Storage strategy is inline on {@link cephadex.brainflex.model.PlayerAnswer}.
 * The size of one submission is bounded by the question's
 * {@code maxStrokesPerPlayer} + {@code maxPointsPerStroke} caps and an
 * additional per-answer byte cap enforced inside ShowcaseService — submissions
 * past the byte cap are rejected outright so a single document can't blow
 * past the Mongo 16 MB limit.
 */
package cephadex.brainflex.model.answer;

import java.util.List;

public record DrawingAnswer(List<Stroke> strokes) implements AnswerPayload {
}
