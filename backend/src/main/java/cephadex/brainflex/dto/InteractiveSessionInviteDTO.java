/**
 * Wire representation of an InteractiveSessionInvite. Used by the schedule
 * detail page so the host can see who has redeemed and who hasn't. The token
 * is *not* exposed here — it's only ever sent to the invitee's email.
 */
package cephadex.brainflex.dto;

import java.time.LocalDateTime;

import cephadex.brainflex.model.InteractiveSessionInvite;

public record InteractiveSessionInviteDTO(
        String id,
        String email,
        String resolvedUserId,
        LocalDateTime sentAt,
        LocalDateTime redeemedAt,
        LocalDateTime expiresAt) {

    public static InteractiveSessionInviteDTO from(InteractiveSessionInvite invite) {
        return new InteractiveSessionInviteDTO(
                invite.getId(),
                invite.getEmail(),
                invite.getResolvedUserId(),
                invite.getSentAt(),
                invite.getRedeemedAt(),
                invite.getExpiresAt());
    }
}
