/**
 * Broadcast on /topic/interactive-session/{roomCode}/round when a new round begins.
 *
 * The `element` is the redacted view — correct-answer fields are nulled out so
 * a clever client can't read them off the wire before submitting. The full
 * element (with answers) is sent in RoundResultMessage during REVEAL.
 */
package cephadex.brainflex.dto;

import java.time.Instant;

import cephadex.brainflex.model.element.DeckElement;

public record RoundStartMessage(
                int round,
                int totalRounds,
                DeckElement element,
                Instant startedAt) {
}
