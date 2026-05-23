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
    // submission during the VOTE phase. Null on non-best-answer rounds and on
    // TimeoutAnswers (timed-out submissions are not eligible to be voted on).
    private String submissionId;

    // Best Answer mode: set to true on REVEAL for the player(s) whose
    // submission received the most votes. Drives the "winner" indicator in
    // round result + review UIs.
    private boolean bestAnswerWinner;

    // Chunk 13 — timing + streak metadata. timeTakenMs is always populated
    // (answeredAt - roundStartedAt, in millis) regardless of whether
    // speedBonus is on — chunks 15 (game history) and 16 (analytics) need
    // it. streakBeforeAnswer captures the player's currentStreak as it was
    // immediately before this answer was scored, so the reveal can render
    // "5x streak!" without the client doing its own walking sum.
    // speedBonusAwarded is the bonus portion of pointsAwarded — non-zero
    // only on correct answers when settings.speedBonus is true.
    private long timeTakenMs;
    private int streakBeforeAnswer;
    private int speedBonusAwarded;

    // Placeholder for a future power-up chunk. Tracked here so historical
    // PlayerAnswer documents already carry the field once power-ups land.
    private boolean usedPowerUp;
    private String powerUpId;
}
