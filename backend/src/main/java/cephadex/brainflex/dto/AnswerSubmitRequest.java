/**
 * Payload sent by a client over STOMP when submitting an answer during a game round.
 *
 * For MULTIPLE_CHOICE rounds: selectedOption is the 0-based index into the question's options
 * (-1 reserved for timeout).
 * For TEXT_INPUT rounds: textAnswer holds the player's free-text answer; selectedOption is ignored.
 *
 * Both fields are accepted on every payload so the WebSocket schema stays uniform — the
 * server uses Question.type to decide which one to consume.
 */
package cephadex.brainflex.dto;

public record AnswerSubmitRequest(
        String questionId,
        int selectedOption,
        String textAnswer) {
}
