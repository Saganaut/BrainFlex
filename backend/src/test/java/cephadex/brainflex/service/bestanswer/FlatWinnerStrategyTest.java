/**
 * Verifies the FLAT_WINNER (legacy) strategy: only the top-vote-getter(s)
 * receive the configured points; ties award the full bonus to every tied
 * player; zero votes anywhere produces no awards.
 *
 * Mirrors the {@link PointsPerVoteStrategyTest} fixtures so the behavioral
 * delta between the two strategies is easy to read off the diff.
 */
package cephadex.brainflex.service.bestanswer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import cephadex.brainflex.model.session.PlayerAnswer;
import cephadex.brainflex.model.session.RoundVote;
class FlatWinnerStrategyTest {

    private final FlatWinnerStrategy strategy = new FlatWinnerStrategy();

    @Test
    void singleWinner_onlyTheTopVoteGetterEarnsTheBonus() {
        Map<String, PlayerAnswer> submissions = new HashMap<>();
        submissions.put("u1", answer("sub-1"));
        submissions.put("u2", answer("sub-2"));

        List<RoundVote> votes = List.of(vote("sub-1"), vote("sub-1"), vote("sub-2"));

        Map<String, Integer> awards = strategy.award(submissions, votes, 50);

        assertEquals(1, awards.size());
        assertEquals(50, awards.get("u1"));
        assertFalse(awards.containsKey("u2"));
    }

    @Test
    void tie_everyTiedPlayerGetsTheFullBonus() {
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
    void singleVoter_onlyTheChosenSubmissionWins() {
        Map<String, PlayerAnswer> submissions = new HashMap<>();
        submissions.put("u1", answer("sub-1"));
        submissions.put("u2", answer("sub-2"));

        List<RoundVote> votes = List.of(vote("sub-2"));

        Map<String, Integer> awards = strategy.award(submissions, votes, 50);

        assertEquals(1, awards.size());
        assertEquals(50, awards.get("u2"));
    }

    @Test
    void everyPlayerGetsOne_everyPlayerWinsBecauseEveryoneIsTied() {
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
