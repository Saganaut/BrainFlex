/**
 * Legacy best-answer scoring preserved as a strategy: the submission(s) with
 * the most votes each earn the configured points value. Ties → every tied
 * player gets the full bonus (matching the pre-chunk-24 hard-coded behavior).
 *
 * "Most votes" is strictly positive — a round where every submission got
 * zero votes awards nothing.
 */
package cephadex.brainflex.service.bestanswer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import cephadex.brainflex.model.PlayerAnswer;
import cephadex.brainflex.model.RoundVote;
import cephadex.brainflex.model.enums.BestAnswerScoring;

@Component
public class FlatWinnerStrategy implements BestAnswerScoringStrategy {

    @Override
    public BestAnswerScoring kind() {
        return BestAnswerScoring.FLAT_WINNER;
    }

    @Override
    public Map<String, Integer> award(
            Map<String, PlayerAnswer> submissionsByUserId,
            List<RoundVote> votes,
            int configuredPoints) {
        if (configuredPoints <= 0 || submissionsByUserId.isEmpty() || votes.isEmpty()) {
            return Map.of();
        }
        Map<String, Integer> voteCounts = new HashMap<>();
        for (RoundVote v : votes) {
            if (v.getVotedSubmissionId() == null) continue;
            voteCounts.merge(v.getVotedSubmissionId(), 1, Integer::sum);
        }
        int maxVotes = voteCounts.values().stream().max(Integer::compareTo).orElse(0);
        if (maxVotes <= 0) {
            return Map.of();
        }
        Map<String, Integer> result = new HashMap<>();
        for (Map.Entry<String, PlayerAnswer> entry : submissionsByUserId.entrySet()) {
            String submissionId = entry.getValue().getSubmissionId();
            if (submissionId == null) continue;
            int count = voteCounts.getOrDefault(submissionId, 0);
            if (count == maxVotes) {
                result.put(entry.getKey(), configuredPoints);
            }
        }
        return result;
    }
}
