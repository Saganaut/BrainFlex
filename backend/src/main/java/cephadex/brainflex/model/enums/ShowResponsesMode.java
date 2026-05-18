/**
 * When the host display should reveal participant responses.
 *   INSTANT   — responses stream in live as players submit.
 *   ON_CLICK  — responses stay hidden until the host explicitly reveals them.
 *   PRIVATE   — responses are never shown on the host display (results visible
 *               only in post-game review / private moderation).
 */
package cephadex.brainflex.model.enums;

public enum ShowResponsesMode {
    INSTANT,
    ON_CLICK,
    PRIVATE
}
