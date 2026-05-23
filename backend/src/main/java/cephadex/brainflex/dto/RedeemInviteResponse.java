/**
 * Response for POST /api/invites/{token}/redeem. When the invitee clicks the
 * link before the session boots, `roomCode` is null and the frontend renders
 * a "waiting room" page that polls. Once booted, roomCode lets the client
 * jump straight into the lobby.
 */
package cephadex.brainflex.dto;

import java.time.Instant;

public record RedeemInviteResponse(
                String scheduledInteractiveSessionId,
                String interactiveSessionId,
                String roomCode,
                String deckName,
                String hostName,
                Instant scheduledStartAt) {
}
