/**
 * Payload sent by a client over STOMP when submitting an answer during a round.
 *
 * `elementId` identifies which element this answer is for (stable across deck
 * snapshots); `payload` is the polymorphic AnswerPayload — its `kind`
 * discriminator on the wire picks the right subtype on deserialization.
 */
package cephadex.brainflex.dto.session;

import cephadex.brainflex.model.answer.AnswerPayload;

public record AnswerSubmitRequest(
        String elementId,
        AnswerPayload payload) {
}
