/**
 * Client → server payload for a single chat message. The body is validated
 * server-side (non-blank, ≤ 500 chars after trim).
 */
package cephadex.brainflex.dto.session;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatSendRequest(
        @NotBlank @Size(max = 500) String body) {
}
