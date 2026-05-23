/**
 * Request body for transferring deck ownership to another user. The recipient
 * must already exist (no email-based transfers — invite them first).
 */
package cephadex.brainflex.dto.deck;

import jakarta.validation.constraints.NotBlank;

public record TransferOwnershipRequest(
        @NotBlank String userId) {
}
