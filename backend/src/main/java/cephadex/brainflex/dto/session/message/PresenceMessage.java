/**
 * Broadcast on /topic/presence whenever a player's online status changes.
 * Clients merge these into local state to dim/disable players who've disconnected.
 *
 * `userId` is the database id of the User (NOT the principal name) so clients can
 * cross-reference against InteractiveSession.players[].userId without translation.
 */
package cephadex.brainflex.dto.session.message;

public record PresenceMessage(
        String userId,
        boolean online) {
}
