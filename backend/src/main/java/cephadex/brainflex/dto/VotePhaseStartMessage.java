/**
 * Broadcast on /topic/showcase/{roomCode}/votePhase when a Best Answer round
 * transitions from SUBMIT to VOTE.
 *
 * Each submission carries only the anonymous `submissionId` + the polymorphic
 * `payload` — the server does NOT include `userId` or `userName` here so the
 * client can render submissions without revealing who authored which. The
 * mapping back to userId happens server-side at REVEAL time.
 *
 * `timePerVote` mirrors the SUBMIT-phase duration so clients can show a
 * consistent countdown; 0 means "host advances".
 */
package cephadex.brainflex.dto;

import java.time.LocalDateTime;
import java.util.List;

import cephadex.brainflex.model.answer.AnswerPayload;
import cephadex.brainflex.model.element.DeckElement;

public record VotePhaseStartMessage(
        int round,
        DeckElement element,                 // still redacted (no correct answer)
        List<AnonymizedSubmission> submissions,
        int timePerVote,
        LocalDateTime phaseStartedAt) {

    public record AnonymizedSubmission(
            String submissionId,
            AnswerPayload payload) {
    }
}
