/**
 * Unit tests for the survey + matching branches in ElementScorer. Survey
 * kinds (Word Cloud, Allocation) are unscored by contract — every result
 * should be ZERO regardless of payload. Matching is scored, and exercises
 * both ALL_OR_NOTHING and PARTIAL paths.
 */
package cephadex.brainflex.service;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import cephadex.brainflex.model.answer.AllocationAnswer;
import cephadex.brainflex.model.answer.DrawingAnswer;
import cephadex.brainflex.model.answer.MatchingAnswer;
import cephadex.brainflex.model.answer.Stroke;
import cephadex.brainflex.model.answer.WordCloudAnswer;
import cephadex.brainflex.model.element.AllocationQuestion;
import cephadex.brainflex.model.element.DrawingQuestion;
import cephadex.brainflex.model.element.MatchingPair;
import cephadex.brainflex.model.element.MatchingQuestion;
import cephadex.brainflex.model.element.WordCloudQuestion;
import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.MatchingScoring;
import cephadex.brainflex.model.enums.MediaPosition;
import cephadex.brainflex.model.enums.ResponseMode;

class ElementScorerTest {

    private static WordCloudQuestion wordCloud() {
        return new WordCloudQuestion(
                "wc-1", "pub", "priv", "Title", null,
                "Describe Monday in a word", 3, 30, false, true, List.of(),
                0, Difficulty.MEDIUM,
                false, true, null, ResponseMode.ACCEPTING_RESPONSES,
                false, null, 0, null,
                30, null, null, null, null, null, MediaPosition.NONE);
    }

    private static AllocationQuestion allocation() {
        return new AllocationQuestion(
                "alloc-1", "pub", "priv", "Title", null,
                "Distribute 100 points across these features", List.of(),
                100, true, true,
                0, Difficulty.MEDIUM,
                false, true, null, ResponseMode.ACCEPTING_RESPONSES,
                false, null, 0, null,
                30, null, null, null, null, null, MediaPosition.NONE);
    }

    private static DrawingQuestion drawing() {
        return new DrawingQuestion(
                "draw-1", "pub", "priv", "Title", null,
                "Sketch the org chart", null,
                1920, 1080, 200, 500, List.of(),
                0, Difficulty.MEDIUM,
                false, true, null, ResponseMode.ACCEPTING_RESPONSES,
                false, null, 0, null,
                30, null, null, null, null, null, MediaPosition.NONE);
    }

    private static MatchingQuestion matching(MatchingScoring scoring) {
        return new MatchingQuestion(
                "m-1", "pub", "priv", "Title", null,
                "Match the rivers to their continents",
                List.of(
                        new MatchingPair("p1", "Nile", "Africa", null, null),
                        new MatchingPair("p2", "Amazon", "South America", null, null),
                        new MatchingPair("p3", "Yangtze", "Asia", null, null),
                        new MatchingPair("p4", "Danube", "Europe", null, null)),
                scoring,
                100, Difficulty.MEDIUM,
                true, false, null, ResponseMode.ACCEPTING_RESPONSES,
                false, null, 0, null,
                30, null, null, null, null, null, MediaPosition.NONE);
    }

    @Test
    void wordCloud_isAlwaysUnscored() {
        var result = ElementScorer.score(wordCloud(), new WordCloudAnswer(List.of("monday", "rainy")));
        assertFalse(result.correct());
        assertEquals(0, result.points());
    }

    @Test
    void allocation_isAlwaysUnscored() {
        var result = ElementScorer.score(
                allocation(), new AllocationAnswer(Map.of("opt-1", 60, "opt-2", 40)));
        assertFalse(result.correct());
        assertEquals(0, result.points());
    }

    @Test
    void drawing_isAlwaysUnscored() {
        var answer = new DrawingAnswer(List.of(
                new Stroke("#000", 4.0, List.of(0.0, 0.0, 1.0, 1.0))));
        var result = ElementScorer.score(drawing(), answer);
        assertFalse(result.correct());
        assertEquals(0, result.points());
    }

    @Test
    void matching_allOrNothing_awardsFullPointsOnPerfectMatch() {
        var answer = new MatchingAnswer(Map.of(
                "p1", "p1", "p2", "p2", "p3", "p3", "p4", "p4"));
        var result = ElementScorer.score(matching(MatchingScoring.ALL_OR_NOTHING), answer);
        assertTrue(result.correct());
        assertEquals(100, result.points());
    }

    @Test
    void matching_allOrNothing_awardsZeroOnAnyMismatch() {
        // Three correct, one wrong → zero under ALL_OR_NOTHING.
        var answer = new MatchingAnswer(Map.of(
                "p1", "p1", "p2", "p2", "p3", "p3", "p4", "p1"));
        var result = ElementScorer.score(matching(MatchingScoring.ALL_OR_NOTHING), answer);
        assertFalse(result.correct());
        assertEquals(0, result.points());
    }

    @Test
    void matching_partial_prorates() {
        // Two correct out of four → 50 of 100 points; not flagged as perfect.
        var answer = new MatchingAnswer(Map.of(
                "p1", "p1", "p2", "p2", "p3", "p1", "p4", "p2"));
        var result = ElementScorer.score(matching(MatchingScoring.PARTIAL), answer);
        assertFalse(result.correct());
        assertEquals(50, result.points());
    }

    @Test
    void matching_partial_perfectAwardsFullAndMarksCorrect() {
        var answer = new MatchingAnswer(Map.of(
                "p1", "p1", "p2", "p2", "p3", "p3", "p4", "p4"));
        var result = ElementScorer.score(matching(MatchingScoring.PARTIAL), answer);
        assertTrue(result.correct());
        assertEquals(100, result.points());
    }
}
