/**
 * Wire representation of an InteractiveSessionInvite. Used by the schedule
 * detail page so the host can see who has redeemed and who hasn't. The token
 * is *not* exposed here — it's only ever sent to the invitee's email.
 */
package cephadex.brainflex.dto;

import java.time.Instant;

import cephadex.brainflex.model.session.InteractiveSessionInvite;

public record InteractiveSessionInviteResponse(
        String id,
        String email,
        String resolvedUserId,
        Instant sentAt,
        Instant redeemedAt,
        Instant expiresAt) {

    public static InteractiveSessionInviteResponse from(InteractiveSessionInvite invite) {
        return new InteractiveSessionInviteResponse(
                invite.getId(),
                invite.getEmail(),
                invite.getResolvedUserId(),
                invite.getSentAt(),
                invite.getRedeemedAt(),
                invite.getExpiresAt());
    }
}
