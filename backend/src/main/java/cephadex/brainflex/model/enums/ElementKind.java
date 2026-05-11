/**
 * Top-level discriminator for items in a Deck.
 *
 * QUESTION  — has options/answer + scoring + per-player submission cycle.
 * SLIDE     — non-interactive content; auto-advances on its display timer (host can
 *             also skip in TURN_BASED). Used for title screens, section dividers,
 *             callouts, etc. — analogous to a PowerPoint slide between questions.
 *
 * Both kinds share the Question document so the existing draw / shuffle / broadcast
 * pipeline stays uniform; the runtime branches on kind for behavior.
 */
package cephadex.brainflex.model.enums;

public enum ElementKind {
    QUESTION,
    SLIDE
}
