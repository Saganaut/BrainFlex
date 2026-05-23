/**
 * Request body for inviting a collaborator. {@code userIdOrEmail} may be a
 * registered userId, a userName, or an email. {@code role} is required and must
 * be EDITOR or VIEWER — OWNER is reserved for the transfer endpoint.
 */
package cephadex.brainflex.dto.deck;

import cephadex.brainflex.model.enums.CollaboratorRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InviteCollaboratorRequest(
        @NotBlank String userIdOrEmail,
        @NotNull CollaboratorRole role) {
}
