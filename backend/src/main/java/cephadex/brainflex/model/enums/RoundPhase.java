/**
 * Phase within a single round of a InteractiveSession. Most question types live entirely
 * in SUBMIT; Best-Answer-mode questions add a VOTE + REVEAL pass.
 *
 *   SUBMIT — players answer the question
 *   VOTE   — players see all submissions anonymized and vote on the best one
 *   REVEAL — round result is broadcast; scoreboard updates; auto-advances
 */
package cephadex.brainflex.model.enums;

public enum RoundPhase {
    SUBMIT,
    VOTE,
    REVEAL
}
