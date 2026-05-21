/**
 * Verifies the POINTS_PER_VOTE strategy across the fixtures called out in
 * chunk 24: single-winner, tie, no-votes-cast, single-voter,
 * every-player-gets-one. Same fixtures are mirrored in
 * {@link FlatWinnerStrategyTest} so the two strategies' divergent behavior
 * is obvious by diff.
 */
package cephadex.brainflex.service.bestanswer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import cephadex.brainflex.model.PlayerAnswer;
import cephadex.brainflex.model.RoundVote;

class PointsPerVoteStrategyTest {

    private final PointsPerVoteStrategy strategy = new PointsPerVoteStrategy();

    @Test
    void singleWinner_awardsVoteCountTimesPoints() {
        Map<String, PlayerAnswer> submissions = new HashMap<>();
        submissions.put("u1", answer("sub-1"));
        submissions.put("u2", answer("sub-2"));

        List<RoundVote> votes = List.of(vote("sub-1"), vote("sub-1"), vote("sub-2"));

        Map<String, Integer> awards = strategy.award(submissions, votes, 50);

        assertEquals(2, awards.size());
        assertEquals(100, awards.get("u1"));
        assertEquals(50, awards.get("u2"));
    }

    @Test
    void tie_eachTiedPlayerKeepsTheirOwnTotal() {
        Map<String, PlayerAnswer> submissions = new HashMap<>();
        submissions.put("u1", answer("sub-1"));
        submissions.put("u2", answer("sub-2"));

        List<RoundVote> votes = List.of(vote("sub-1"), vote("sub-2"));

        Map<String, Integer> awards = strategy.award(submissions, votes, 50);

        assertEquals(50, awards.get("u1"));
        assertEquals(50, awards.get("u2"));
    }

    @Test
    void noVotesCast_awardsNothing() {
        Map<String, PlayerAnswer> submissions = new HashMap<>();
        submissions.put("u1", answer("sub-1"));

        Map<String, Integer> awards = strategy.award(submissions, List.of(), 50);
        assertTrue(awards.isEmpty());
    }

    @Test
    void singleVoter_onlyTheirChoiceAwarded() {
        Map<String, PlayerAnswer> submissions = new HashMap<>();
        submissions.put("u1", answer("sub-1"));
        submissions.put("u2", answer("sub-2"));

        List<RoundVote> votes = List.of(vote("sub-2"));

        Map<String, Integer> awards = strategy.award(submissions, votes, 50);

        assertEquals(1, awards.size());
        assertEquals(50, awards.get("u2"));
    }

    @Test
    void everyPlayerGetsOne_everyPlayerEarnsTheBaseAward() {
        Map<String, PlayerAnswer> submissions = new HashMap<>();
        submissions.put("u1", answer("sub-1"));
        submissions.put("u2", answer("sub-2"));
        submissions.put("u3", answer("sub-3"));

        List<RoundVote> votes = List.of(vote("sub-1"), vote("sub-2"), vote("sub-3"));

        Map<String, Integer> awards = strategy.award(submissions, votes, 50);

        assertEquals(50, awards.get("u1"));
        assertEquals(50, awards.get("u2"));
        assertEquals(50, awards.get("u3"));
    }

    @Test
    void zeroPoints_noAwards() {
        Map<String, PlayerAnswer> submissions = new HashMap<>();
        submissions.put("u1", answer("sub-1"));

        Map<String, Integer> awards = strategy.award(submissions, List.of(vote("sub-1")), 0);
        assertTrue(awards.isEmpty());
    }

    private static PlayerAnswer answer(String submissionId) {
        PlayerAnswer pa = new PlayerAnswer();
        pa.setSubmissionId(submissionId);
        return pa;
    }

    private static RoundVote vote(String submissionId) {
        RoundVote v = new RoundVote();
        v.setVotedSubmissionId(submissionId);
        return v;
    }
}
