/**
 * A single player's submission for one element. The polymorphic `payload`
 * carries the type-specific response (option id, text, ranking, coordinates,
 * etc.) — the scorer branches on the payload variant to compute correctness
 * and point value.
 *
 * Embedded inside ShowcasePlayer so all answers for a player live with their
 * session record.
 */
package cephadex.brainflex.model;

import java.time.LocalDateTime;

import cephadex.brainflex.model.answer.AnswerPayload;
import lombok.Data;

@Data
public class PlayerAnswer {
    private String elementId;
    private AnswerPayload payload; // see model.answer.* for variants
    private boolean correct;
    private int pointsAwarded;
    private LocalDateTime answeredAt;

    // Best Answer mode: server-generated id used to anonymously identify this
    // submission during the VOTE phase. Null on non-best-answer rounds and on
    // TimeoutAnswers (timed-out submissions are not eligible to be voted on).
    private String submissionId;

    // Best Answer mode: set to true on REVEAL for the player(s) whose
    // submission received the most votes. Drives the "winner" indicator in
    // round result + review UIs.
    private boolean bestAnswerWinner;

    // TODO: Worth adding a fastest response? or resposne time?
}
