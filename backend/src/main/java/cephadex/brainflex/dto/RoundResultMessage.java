/**
 * Broadcast on /topic/interactive-session/{roomCode}/roundResult when a round ends.
 * Carries the un-redacted element (so clients can render the correct answer)
 * plus per-player outcomes. For slide rounds we still send this so clients
 * have a uniform round-completion signal — but `playerResults` is empty and
 * the element has no correct-answer fields to reveal.
 *
 * For Best Answer rounds, `bestAnswer` carries the vote tallies + winner ids
 * so the REVEAL UI can render the de-anonymized submissions and the crown.
 * On non-best-answer rounds `bestAnswer` is null.
 */
package cephadex.brainflex.dto;

import java.util.List;

import cephadex.brainflex.model.answer.AnswerPayload;
import cephadex.brainflex.model.element.DeckElement;
import cephadex.brainflex.model.enums.SessionFormat;

public record RoundResultMessage(
        int round,
        // Chunk 24 — carries the session's chrome flavor on every round-end
        // broadcast so the client can mount the right shell (GAME →
        // RoundResult / leaderboard; PRESENTATION → RoundDataView) without
        // an extra session lookup. Frozen for the duration of the session.
        SessionFormat format,
        DeckElement element,                // un-redacted; carries answer key
        List<PlayerRoundResult> playerResults,
        BestAnswerOutcome bestAnswer) {

    public record PlayerRoundResult(
            String userId,
            String userName,
            AnswerPayload payload,          // what the player submitted (null = timeout via sentinel)
            boolean wasCorrect,
            int pointsAwarded,
            int totalScore) {
    }

    /**
     * REVEAL data for Best Answer rounds. `tallies` lists each submission with
     * its de-anonymized author + vote count; `winnerUserIds` is the set of
     * players who received the most votes (multiple on a tie). Each winner
     * receives `bonusAwarded` points, which is also already reflected in the
     * corresponding `playerResults[i].pointsAwarded` and `totalScore`.
     */
    public record BestAnswerOutcome(
            List<SubmissionTally> tallies,
            List<String> winnerUserIds,
            int bonusAwarded) {
    }

    public record SubmissionTally(
            String submissionId,
            String userId,
            String userName,
            AnswerPayload payload,
            int voteCount) {
    }
}
