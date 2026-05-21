/**
 * When the host display should reveal participant responses.
 *
 *   INHERIT   — defer to the next level up the cascade (element → deck →
 *               session → format default). Default everywhere; the runtime
 *               cascade collapses it to one of the concrete values below.
 *   INSTANT   — responses stream in live as players submit.
 *   ON_CLICK  — responses stay hidden until the host explicitly reveals them
 *               (matches Mentimeter's "click to show responses" feel).
 *   PRIVATE   — responses are never shown on the host display (results visible
 *               only in post-game review / private moderation).
 *
 * Per-format defaults when resolution bottoms out on INHERIT everywhere:
 *   GAME         → INSTANT (reveal fires when the round closes).
 *   PRESENTATION → ON_CLICK (host paces the reveal manually).
 */
package cephadex.brainflex.model.enums;

public enum ShowResponsesMode {
    INHERIT,
    INSTANT,
    ON_CLICK,
    PRIVATE
}
