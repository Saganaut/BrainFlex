/**
 * Default best-answer scoring: every player whose submission gathered at
 * least one vote earns {@code votesReceived * configuredPoints}. Ties are
 * handled naturally — each tied player keeps their own per-vote total.
 */
package cephadex.brainflex.service.bestanswer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import cephadex.brainflex.model.session.PlayerAnswer;
import cephadex.brainflex.model.session.RoundVote;
import cephadex.brainflex.model.enums.BestAnswerScoring;

@Component
public class PointsPerVoteStrategy implements BestAnswerScoringStrategy {

    @Override
    public BestAnswerScoring kind() {
        return BestAnswerScoring.POINTS_PER_VOTE;
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
        Map<String, Integer> result = new HashMap<>();
        for (Map.Entry<String, PlayerAnswer> entry : submissionsByUserId.entrySet()) {
            String submissionId = entry.getValue().getSubmissionId();
            if (submissionId == null) continue;
            int count = voteCounts.getOrDefault(submissionId, 0);
            if (count == 0) continue;
            result.put(entry.getKey(), count * configuredPoints);
        }
        return result;
    }
}
