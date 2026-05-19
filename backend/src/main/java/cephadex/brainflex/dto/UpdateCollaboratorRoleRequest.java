/**
 * Request body for changing an existing collaborator's role. Cannot promote to
 * OWNER — use the transfer endpoint for that.
 */
package cephadex.brainflex.dto;

import cephadex.brainflex.model.enums.CollaboratorRole;
import jakarta.validation.constraints.NotNull;

public record UpdateCollaboratorRoleRequest(
        @NotNull CollaboratorRole role) {
}
