/**
 * Unit tests for the survey + matching branches in ElementScorer, plus the
 * chunk-10 per-kind validation paths (MCQ multi-select rejection, Number
 * range rejection, Text fuzzy match). Survey kinds (Word Cloud, Allocation,
 * Drawing) are unscored by contract — every result should be ZERO regardless
 * of payload.
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
import cephadex.brainflex.model.answer.McqAnswer;
import cephadex.brainflex.model.answer.NumberAnswer;
import cephadex.brainflex.model.answer.Stroke;
import cephadex.brainflex.model.answer.TextAnswer;
import cephadex.brainflex.model.answer.WordCloudAnswer;
import cephadex.brainflex.model.element.AllocationQuestion;
import cephadex.brainflex.model.element.DrawingQuestion;
import cephadex.brainflex.model.element.MatchingPair;
import cephadex.brainflex.model.element.MatchingQuestion;
import cephadex.brainflex.model.element.McqOption;
import cephadex.brainflex.model.element.McqQuestion;
import cephadex.brainflex.model.element.NumberQuestion;
import cephadex.brainflex.model.element.TextQuestion;
import cephadex.brainflex.model.element.WordCloudQuestion;
import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.MatchingScoring;
import cephadex.brainflex.model.enums.MediaPosition;
import cephadex.brainflex.model.enums.ResponseMode;

class ElementScorerTest {

    // Metadata block (chunk 10b) — every test record gets the same v1 / reactions-on tail.
    private static final String META_USER = null;
    private static final java.time.LocalDateTime META_TIME = null;
    private static final List<String> META_TAGS = List.of();
    private static final String META_CAPTION = null;
    private static final String META_ALT = null;
    private static final boolean META_REACTIONS = true;
    private static final Integer META_VERSION = 1;

    private static WordCloudQuestion wordCloud() {
        return new WordCloudQuestion(
                "wc-1", "pub", "priv", "Title", null,
                "Describe Monday in a word", 3, 30, false, true, List.of(),
                0, Difficulty.MEDIUM,
                false, true, null, ResponseMode.ACCEPTING_RESPONSES,
                false, null, 0, null,
                30, null, null, null, null, null, MediaPosition.NONE,
                META_USER, META_USER, META_TIME, META_TIME, META_TAGS,
                META_CAPTION, META_ALT, META_REACTIONS, META_VERSION);
    }

    private static AllocationQuestion allocation() {
        return new AllocationQuestion(
                "alloc-1", "pub", "priv", "Title", null,
                "Distribute 100 points across these features", List.of(),
                100, true, true,
                0, Difficulty.MEDIUM,
                false, true, null, ResponseMode.ACCEPTING_RESPONSES,
                false, null, 0, null,
                30, null, null, null, null, null, MediaPosition.NONE,
                META_USER, META_USER, META_TIME, META_TIME, META_TAGS,
                META_CAPTION, META_ALT, META_REACTIONS, META_VERSION);
    }

    private static DrawingQuestion drawing() {
        return new DrawingQuestion(
                "draw-1", "pub", "priv", "Title", null,
                "Sketch the org chart", null,
                1920, 1080, 200, 500, List.of(),
                0, Difficulty.MEDIUM,
                false, true, null, ResponseMode.ACCEPTING_RESPONSES,
                false, null, 0, null,
                30, null, null, null, null, null, MediaPosition.NONE,
                META_USER, META_USER, META_TIME, META_TIME, META_TAGS,
                META_CAPTION, META_ALT, META_REACTIONS, META_VERSION);
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
                30, null, null, null, null, null, MediaPosition.NONE,
                META_USER, META_USER, META_TIME, META_TIME, META_TAGS,
                META_CAPTION, META_ALT, META_REACTIONS, META_VERSION);
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

    // ---- chunk 10: per-kind validation paths ----

    private static McqQuestion mcq(boolean allowMultiple, int maxSelections) {
        var a = new McqOption("a", "A", null, null);
        var b = new McqOption("b", "B", null, null);
        var c = new McqOption("c", "C", null, null);
        return new McqQuestion(
                "mcq-1", "pub", "priv", "Title", null,
                "Pick", List.of(a, b, c), List.of(a.id(), b.id()),
                100, Difficulty.EASY,
                true, false, null, ResponseMode.ACCEPTING_RESPONSES,
                false, null, 0, null,
                15, null, null, null, null, null, MediaPosition.NONE,
                true, allowMultiple, maxSelections,
                META_USER, META_USER, META_TIME, META_TIME, META_TAGS,
                META_CAPTION, META_ALT, META_REACTIONS, META_VERSION);
    }

    @Test
    void mcq_singleSelect_rejectsMultiPick() {
        var result = ElementScorer.score(mcq(false, 0), new McqAnswer(List.of("a", "b")));
        assertFalse(result.correct());
        assertEquals(0, result.points());
    }

    @Test
    void mcq_multiSelect_acceptsCorrectSubset() {
        var result = ElementScorer.score(mcq(true, 0), new McqAnswer(List.of("a", "b")));
        assertTrue(result.correct());
        assertEquals(100, result.points());
    }

    @Test
    void mcq_multiSelect_rejectsOverCap() {
        var result = ElementScorer.score(mcq(true, 1), new McqAnswer(List.of("a", "b")));
        assertFalse(result.correct());
        assertEquals(0, result.points());
    }

    @Test
    void mcq_multiSelect_maxSelectionsZeroIsUnlimited() {
        // chunk 21 — author may set Slide.selectionsPerParticipant = 0 ("unlimited")
        // which maps to McqQuestion.maxSelections = 0. The cap check must NOT
        // reject a submission just because it exceeds the correct-set size;
        // only a set-mismatch should zero the result.
        var result = ElementScorer.score(mcq(true, 0), new McqAnswer(List.of("a", "b", "c")));
        assertFalse(result.correct()); // c isn't correct, set mismatch
        assertEquals(0, result.points());
    }

    private static NumberQuestion number(Double min, Double max) {
        return new NumberQuestion(
                "num-1", "pub", "priv", "Title", null,
                "Guess", 10.0, 0.0, "", 0,
                100, Difficulty.EASY,
                true, false, null, ResponseMode.ACCEPTING_RESPONSES,
                false, null, 0, null,
                15, null, null, null, null, null, MediaPosition.NONE,
                min, max, true,
                META_USER, META_USER, META_TIME, META_TIME, META_TAGS,
                META_CAPTION, META_ALT, META_REACTIONS, META_VERSION);
    }

    @Test
    void number_rejectsBelowMin() {
        var result = ElementScorer.score(number(0.0, null), new NumberAnswer(-1));
        assertFalse(result.correct());
        assertEquals(0, result.points());
    }

    @Test
    void number_rejectsAboveMax() {
        var result = ElementScorer.score(number(null, 100.0), new NumberAnswer(101));
        assertFalse(result.correct());
        assertEquals(0, result.points());
    }

    @Test
    void number_acceptsInRange() {
        var result = ElementScorer.score(number(0.0, 100.0), new NumberAnswer(10));
        assertTrue(result.correct());
        assertEquals(100, result.points());
    }

    private static TextQuestion text(boolean fuzzy, int distance) {
        return new TextQuestion(
                "text-1", "pub", "priv", "Title", null,
                "Spell it", "Mississippi", List.of(), false,
                100, Difficulty.EASY,
                true, false, null, ResponseMode.ACCEPTING_RESPONSES,
                false, null, 0, null,
                20, null, null, null, null, null, MediaPosition.NONE,
                80, true, fuzzy, distance,
                META_USER, META_USER, META_TIME, META_TIME, META_TAGS,
                META_CAPTION, META_ALT, META_REACTIONS, META_VERSION);
    }

    @Test
    void text_fuzzyMatch_acceptsSingleTypo() {
        // "Missisippi" — one deletion away from "Mississippi".
        var result = ElementScorer.score(text(true, 1), new TextAnswer("Missisippi"));
        assertTrue(result.correct());
        assertEquals(100, result.points());
    }

    @Test
    void text_fuzzyMatch_rejectsWhenDistanceExceedsCap() {
        // "Mssisippi" — two edits away.
        var result = ElementScorer.score(text(true, 1), new TextAnswer("Mssisippi"));
        assertFalse(result.correct());
        assertEquals(0, result.points());
    }

    @Test
    void text_fuzzyDisabled_rejectsTypo() {
        var result = ElementScorer.score(text(false, 1), new TextAnswer("Missisippi"));
        assertFalse(result.correct());
        assertEquals(0, result.points());
    }
}
