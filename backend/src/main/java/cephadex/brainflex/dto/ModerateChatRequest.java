/**
 * Host → server request to hide a chat message. Only the host can send this
 * (validated server-side); the request carries just the messageId so the
 * destination URL doesn't have to change shape.
 */
package cephadex.brainflex.dto;

import jakarta.validation.constraints.NotBlank;

public record ModerateChatRequest(
        @NotBlank String messageId) {
}
