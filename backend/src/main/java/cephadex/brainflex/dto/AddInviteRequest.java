/**
 * Request body for POST /api/scheduled-interactive-sessions/{id}/invite.
 * Allows the host to add a single additional invitee after creation.
 */
package cephadex.brainflex.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AddInviteRequest(
        @NotBlank @Email String email) {
}
