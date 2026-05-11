/**
 * Sentinel "no answer submitted" payload — filled in by the server at round
 * end for any player who didn't submit in time. Always scores zero.
 */
package cephadex.brainflex.model.answer;

public record TimeoutAnswer() implements AnswerPayload {
}
