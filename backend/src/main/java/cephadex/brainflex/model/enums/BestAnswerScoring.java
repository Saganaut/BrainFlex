/**
 * Strategy selector for the best-answer vote phase scoring step. Decoupled
 * from {@link cephadex.brainflex.service.bestanswer.BestAnswerScoringStrategy}
 * so a deck author can pick the strategy declaratively without the runtime
 * having to switch on the strategy class.
 *
 *   POINTS_PER_VOTE — every player whose submission gathered at least one
 *                     vote earns {@code votesReceived * bestAnswerPoints}.
 *                     Default; rewards every submission proportionally.
 *   FLAT_WINNER     — legacy behavior. The submission(s) with the most votes
 *                     each earn {@code bestAnswerPoints}. Ties: every tied
 *                     player gets the full bonus.
 *
 * Renamed parameter context: chunk 24 also renamed {@code bestAnswerBonus} →
 * {@code bestAnswerPoints} and bumped the default from 0 to 50. The int no
 * longer means "flat bonus" universally — the resolved strategy decides what
 * it means (per-vote multiplier vs. flat amount vs. decay weight).
 */
package cephadex.brainflex.model.enums;

public enum BestAnswerScoring {
    POINTS_PER_VOTE,
    FLAT_WINNER
}
