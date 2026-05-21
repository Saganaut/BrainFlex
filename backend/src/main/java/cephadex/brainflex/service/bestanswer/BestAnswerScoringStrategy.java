/**
 * Pluggable scoring step for the VOTE phase of a Best Answer round. The
 * service hands the strategy each player's submission for the round and the
 * full vote list; the strategy decides who earns what.
 *
 * Strategies are selected per-element via {@link cephadex.brainflex.model.element.DeckElement#bestAnswerScoring()}.
 * Adding a new strategy means dropping a new {@code @Component} that
 * implements this interface — {@code BestAnswerScoringRegistry} discovers it
 * automatically; no edit to {@code InteractiveSessionService} required.
 */
package cephadex.brainflex.service.bestanswer;

import java.util.List;
import java.util.Map;

import cephadex.brainflex.model.PlayerAnswer;
import cephadex.brainflex.model.RoundVote;
import cephadex.brainflex.model.enums.BestAnswerScoring;

public interface BestAnswerScoringStrategy {

    /** Which value of {@link BestAnswerScoring} this strategy implements. */
    BestAnswerScoring kind();

    /**
     * @param submissionsByUserId  one PlayerAnswer per voting-eligible player, keyed by userId.
     *                             Each {@code PlayerAnswer.getSubmissionId()} is what voters
     *                             reference; timed-out / unsubmitted entries are excluded
     *                             by the caller before invocation.
     * @param votes                every vote cast this round (across all players).
     * @param configuredPoints     the element's {@code bestAnswerPoints} value. Strategies
     *                             interpret it differently — per-vote multiplier under
     *                             POINTS_PER_VOTE, flat winner amount under FLAT_WINNER.
     * @return map of userId → points to award. Entries with zero may be omitted.
     */
    Map<String, Integer> award(
            Map<String, PlayerAnswer> submissionsByUserId,
            List<RoundVote> votes,
            int configuredPoints);
}
