/**
 * Client → server payload for a single audience reaction. The server rejects
 * any emoji not on the codepoint allow-list (see {@code EmojiAllowList}).
 */
package cephadex.brainflex.dto.session;

import jakarta.validation.constraints.NotBlank;

public record ReactionSendRequest(
        @NotBlank String emoji) {
}
