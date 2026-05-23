/**
 * Static allow-list of single-codepoint emojis the audience may send as
 * reactions. Keeping the list small and explicit lets us validate submissions
 * without a regex over the full Unicode emoji table and gives hosts a
 * predictable set of bursts to render.
 *
 * Two layers of validation, both required before persisting a reaction:
 *   - The submitted string must equal one of {@link #DEFAULT_EMOJIS} exactly
 *     (no zero-width joiners, no skin-tone modifiers, no trailing text).
 *   - Implicitly that bounds the length too — every entry is one or two
 *     {@code char}s (the variation-selector form {@code ️} counts as
 *     a second char on the BMP).
 *
 * Add new entries when product asks for them; never reach for a regex/look-up
 * library here unless the list grows past a couple dozen.
 */
package cephadex.brainflex.service;

import java.util.List;
import java.util.Set;

public final class EmojiAllowList {

    private EmojiAllowList() {
    }

    /** Ordered list — clients display the first six as the default ReactionBar. */
    public static final List<String> DEFAULT_EMOJIS = List.of(
            "👍", // 👍 thumbs up
            "❤️", // ❤️ heart
            "😂", // 😂 joy
            "😮", // 😮 wow
            "😢", // 😢 cry
            "🎉", // 🎉 party
            "🔥", // 🔥 fire
            "🚀", // 🚀 rocket
            "👏", // 👏 clap
            "🤔", // 🤔 thinking
            "😡", // 😡 angry
            "✅", // ✅ check
            "❌" // ❌ cross
    );

    private static final Set<String> ALLOWED = Set.copyOf(DEFAULT_EMOJIS);

    public static boolean isAllowed(String emoji) {
        return emoji != null && ALLOWED.contains(emoji);
    }
}
