/**
 * Top-level discriminator for items in a Deck.
 *
 * SLIDE — non-interactive content (title screen, section divider, callout).
 * MCQ / TEXT / NUMBER — questions with a single concrete correct answer.
 * RANKING — order an item list correctly (covers chronological + lowest-to-highest).
 * SCALES — Likert-style rating of one or more statements (typically unscored).
 * Q_AND_A — Slido-style audience submission; host moderates; never scored.
 * GRID — select cells in an N×M grid (with optional backing image).
 * PLACE_ON_IMAGE — drop a point at coordinates on an image.
 *
 * The runtime branches on this value; clients render the appropriate component.
 */
package cephadex.brainflex.model.enums;

public enum ElementKind {
    SLIDE,
    MCQ,
    TEXT,
    NUMBER,
    RANKING,
    SCALES,
    Q_AND_A,
    GRID,
    PLACE_ON_IMAGE
}
