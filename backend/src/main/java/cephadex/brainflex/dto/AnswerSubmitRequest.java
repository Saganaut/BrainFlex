/**
 * Payload sent by a client over STOMP when submitting an answer during a game round.
 * selectedOption is the 0-based index into the question's options list;
 * -1 is reserved for a timeout (no answer submitted).
 */
package cephadex.brainflex.dto;

public record AnswerSubmitRequest(
        String questionId,
        int selectedOption   // 0-based index into Question.options; -1 = timed out
) {
}
