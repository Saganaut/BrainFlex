/**
 * Wire shape for a single deck-collaborator row. Carries display fields so the
 * Share modal can render the user's name and avatar without an extra round-trip.
 *
 * For pending invites to a not-yet-registered email, {@code userId},
 * {@code userName} and {@code pictureUrl} are null and {@code email} holds the
 * invited address. {@code acceptedAt} is null for pending invites.
 */
package cephadex.brainflex.dto.deck;

import java.time.Instant;

import cephadex.brainflex.model.shared.UserSnapshot;
import cephadex.brainflex.model.deck.DeckCollaborator;
import cephadex.brainflex.model.enums.CollaboratorRole;
import cephadex.brainflex.model.user.User;

public record DeckCollaboratorResponse(
                String id,
                String deckId,
                String email,
                UserSnapshot user,
                String userName,
                CollaboratorRole role,
                String invitedByUserId,
                Instant invitedAt,
                Instant acceptedAt) {

        public static DeckCollaboratorResponse of(DeckCollaborator row, User user, String pictureUrl) {
                UserSnapshot snapshot = user == null
                                ? null
                                : UserSnapshot.of(row.getUserId(), user.getName(), pictureUrl);
                return new DeckCollaboratorResponse(
                                row.getId(),
                                row.getDeckId(),
                                row.getEmail(),
                                snapshot,
                                user == null ? null : user.getUserName(),
                                row.getRole(),
                                row.getInvitedByUserId(),
                                row.getInvitedAt(),
                                row.getAcceptedAt());
        }
}
