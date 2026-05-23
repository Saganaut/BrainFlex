/**
 * Per-player outcome for a single round — the shared shape that flows out on
 * both the live round-end broadcast and the post-session review.
 *
 * This is a nested component record: it is never a top-level wire root, only
 * ever embedded in an outbound payload, so it takes the {@code Response} suffix
 * (see DTO-NAMING-RULES §9). Two callers exist and they read it differently:
 *
 *  - {@link RoundResultMessage}.playerResults — broadcast on
 *    /topic/interactive-session/{roomCode}/roundResult the moment a round
 *    ends. `totalScore` is the player's cumulative score at broadcast time
 *    (i.e. {@code InteractiveSessionPlayer.getScore()} after the round was
 *    scored). `payload` may be null when the live broadcast is redacted.
 *
 *  - {@link InteractiveSessionReviewResponse}.rounds[i].playerAnswers — post-game
 *    review. `totalScore` is the running cumulative total after round i,
 *    computed in snapshot order in
 *    {@code InteractiveSessionService.buildReview}.
 *
 * `playerId` is always the session-scoped public handle
 * ({@code InteractiveSessionPlayer.playerId}) — never the user's real userId.
 * `userName` is the denormalized display name frozen at join time.
 */
package cephadex.brainflex.dto.session;

import cephadex.brainflex.dto.session.message.RoundResultMessage;

import cephadex.brainflex.model.answer.AnswerPayload;

public record PlayerRoundResponse(
                String playerId,
                String userName,
                AnswerPayload payload,
                boolean wasCorrect,
                int pointsAwarded,
                int totalScore) {
}
