/**
 * Broadcast on /topic/interactive-session/{roomCode}/submissionsClosing when
 * the host ends the submit phase for the current round (chunk 25).
 *
 * Unlike a normal round end, the host wants any answer a player has *typed but
 * not yet submitted* to count. The host can't see those local drafts, so this
 * message is the coordination signal: every participant device that holds an
 * unsubmitted draft for {@code elementId} immediately submits it via the normal
 * answer path. {@code graceMillis} is how long the server will keep accepting
 * those in-flight flush submissions before it freezes the round and reveals
 * results — see {@code InteractiveSessionService.endSubmitPhase}.
 */
package cephadex.brainflex.dto.session.message;

public record SubmissionsClosingMessage(int round, String elementId, int graceMillis) {
}
