/**
 * Wire shape for a single deck-collaborator row. Carries display fields so the
 * Share modal can render the user's name and avatar without an extra round-trip.
 *
 * For pending invites to a not-yet-registered email, {@code userId},
 * {@code userName} and {@code pictureUrl} are null and {@code email} holds the
 * invited address. {@code acceptedAt} is null for pending invites.
 */
package cephadex.brainflex.dto;

import java.time.LocalDateTime;

import cephadex.brainflex.model.DeckCollaborator;
import cephadex.brainflex.model.User;
import cephadex.brainflex.model.enums.CollaboratorRole;

public record DeckCollaboratorDTO(
        String id,
        String deckId,
        String userId,
        String email,
        String userName,
        String name,
        String pictureUrl,
        CollaboratorRole role,
        String invitedByUserId,
        LocalDateTime invitedAt,
        LocalDateTime acceptedAt) {

    public static DeckCollaboratorDTO of(DeckCollaborator row, User user, String pictureUrl) {
        return new DeckCollaboratorDTO(
                row.getId(),
                row.getDeckId(),
                row.getUserId(),
                row.getEmail(),
                user == null ? null : user.getUserName(),
                user == null ? null : user.getName(),
                pictureUrl,
                row.getRole(),
                row.getInvitedByUserId(),
                row.getInvitedAt(),
                row.getAcceptedAt());
    }
}
