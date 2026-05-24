/**
 * A single player's submission for one element. The polymorphic `payload`
 * carries the type-specific response (option id, text, ranking, coordinates,
 * etc.) — the scorer branches on the payload variant to compute correctness
 * and point value.
 *
 * Embedded inside InteractiveSessionPlayer so all answers for a player live with their
 * session record.
 */
package cephadex.brainflex.model.session;

import java.time.Instant;

import cephadex.brainflex.model.answer.AnswerPayload;
import lombok.Data;

@Data
public class PlayerAnswer {
    private String elementId;
    private AnswerPayload payload; // see model.answer.* for variants
    private boolean correct;
    private int pointsAwarded;
    private Instant answeredAt;

    // Best Answer mode: server-generated id used to anonymously identify this
    // submission during the VOTE phase. Null on non-best-answer rounds
    private String submissionId;

    // Best Answer mode: set to true on REVEAL for the player(s) whose
    // submission received the most votes.
    private boolean bestAnswerWinner;

    private long timeTakenMs;
    private int streakBeforeAnswer;
    private int speedBonusAwarded;

    // Placeholder for a future power-up chunk.
    private boolean usedPowerUp;
    private String powerUpId;
}
