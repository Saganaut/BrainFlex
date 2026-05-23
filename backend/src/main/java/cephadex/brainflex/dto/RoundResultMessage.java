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
        // Per-player outcomes. Shared shape with InteractiveSessionReviewResponse —
        // see PlayerRoundResponse for the field semantics. Empty for slide rounds.
        List<PlayerRoundResponse> playerResults,
        BestAnswerOutcome bestAnswer) {

    /**
     * REVEAL data for Best Answer rounds. `tallies` lists each submission with
     * its de-anonymized author + vote count; `winnerPlayerIds` is the set of
     * players who received the most votes (multiple on a tie). Each winner
     * receives `bonusAwarded` points, which is also already reflected in the
     * corresponding `playerResults[i].pointsAwarded` and `totalScore`.
     *
     * `winnerPlayerIds` carries session-scoped {@code playerId}s, never the
     * underlying userId — see {@link cephadex.brainflex.model.session.InteractiveSessionPlayer#playerId}.
     */
    public record BestAnswerOutcome(
            List<SubmissionTally> tallies,
            List<String> winnerPlayerIds,
            int bonusAwarded) {
    }

    /**
     * One row per submission for the REVEAL phase of a Best Answer round.
     * `playerId` is the session-scoped public handle (never userId) — see
     * {@link cephadex.brainflex.model.session.InteractiveSessionPlayer#playerId}.
     */
    public record SubmissionTally(
            String submissionId,
            String playerId,
            String userName,
            AnswerPayload payload,
            int voteCount) {
    }
}
