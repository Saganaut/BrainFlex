/**
 * Broadcast on /topic/showcase/{roomCode}/roundResult when a round ends.
 * Carries the un-redacted element (so clients can render the correct answer)
 * plus per-player outcomes. For slide rounds we still send this so clients
 * have a uniform round-completion signal — but `playerResults` is empty and
 * the element has no correct-answer fields to reveal.
 */
package cephadex.brainflex.dto;

import java.util.List;

import cephadex.brainflex.model.answer.AnswerPayload;
import cephadex.brainflex.model.element.DeckElement;

public record RoundResultMessage(
        int round,
        DeckElement element,                // un-redacted; carries answer key
        List<PlayerRoundResult> playerResults) {

    public record PlayerRoundResult(
            String userId,
            String userName,
            AnswerPayload payload,          // what the player submitted (null = timeout via sentinel)
            boolean wasCorrect,
            int pointsAwarded,
            int totalScore) {
    }
}
