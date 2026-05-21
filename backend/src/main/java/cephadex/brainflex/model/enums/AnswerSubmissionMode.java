/**
 * Determines how players answer within an interactive session.
 *   SIMULTANEOUS — all players answer at the same time (speed matters).
 *   TURN_BASED   — players answer in sequence (host controls advancement).
 *
 * Renamed from {@code InteractiveSessionMode} in chunk 24 to make room for
 * the orthogonal {@link SessionFormat} (GAME / PRESENTATION) concept. The
 * settings field renamed alongside ({@code mode} → {@code answerSubmissionMode}).
 */
package cephadex.brainflex.model.enums;

public enum AnswerSubmissionMode {
    SIMULTANEOUS,
    TURN_BASED
}
