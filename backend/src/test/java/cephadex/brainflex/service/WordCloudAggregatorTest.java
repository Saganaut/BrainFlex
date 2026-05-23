/**
 * Unit tests for WordCloudAggregator covering case-insensitive folding,
 * trimming/punctuation, banned-word filtering, per-player submission caps,
 * and length truncation.
 */
package cephadex.brainflex.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import cephadex.brainflex.model.answer.WordCloudAnswer;
import cephadex.brainflex.model.element.WordCloudQuestion;
import cephadex.brainflex.model.enums.Difficulty;

class WordCloudAggregatorTest {

    private static WordCloudQuestion question(boolean caseSensitive, int maxPerPlayer,
            int maxWordLength, List<String> banned) {
        return new WordCloudQuestion(
                "wc-1",
                "Prompt", maxPerPlayer, maxWordLength, caseSensitive, true, banned,
                0, Difficulty.MEDIUM, null,
                TestElementChromes.survey("wc-1", "Prompt"));
    }

    @Test
    void foldsCaseInsensitive_andTallies() {
        var q = question(false, 3, 30, List.of());
        var counts = WordCloudAggregator.aggregate(q, List.of(
                new WordCloudAnswer(List.of("Monday")),
                new WordCloudAnswer(List.of("monday")),
                new WordCloudAnswer(List.of("MONDAY"))));
        assertEquals(Map.of("monday", 3), counts);
    }

    @Test
    void caseSensitive_keepsDistinct() {
        var q = question(true, 3, 30, List.of());
        var counts = WordCloudAggregator.aggregate(q, List.of(
                new WordCloudAnswer(List.of("Monday")),
                new WordCloudAnswer(List.of("monday"))));
        assertEquals(2, counts.size());
        assertEquals(1, counts.get("Monday"));
        assertEquals(1, counts.get("monday"));
    }

    @Test
    void stripsSurroundingPunctuation_andTrims() {
        var q = question(false, 3, 30, List.of());
        var counts = WordCloudAggregator.aggregate(q, List.of(
                new WordCloudAnswer(List.of("  hello!  ", "Hello?", "...hello..."))));
        assertEquals(Map.of("hello", 3), counts);
    }

    @Test
    void dropsBannedWords() {
        var q = question(false, 3, 30, List.of("spam", "Junk"));
        var counts = WordCloudAggregator.aggregate(q, List.of(
                new WordCloudAnswer(List.of("spam", "useful", "junk"))));
        assertEquals(Map.of("useful", 1), counts);
    }

    @Test
    void enforcesMaxSubmissionsPerPlayer() {
        var q = question(false, 2, 30, List.of());
        var counts = WordCloudAggregator.aggregate(q, List.of(
                new WordCloudAnswer(List.of("one", "two", "three", "four"))));
        assertEquals(2, counts.values().stream().mapToInt(Integer::intValue).sum());
        assertTrue(counts.containsKey("one"));
        assertTrue(counts.containsKey("two"));
        assertFalse(counts.containsKey("three"));
    }

    @Test
    void truncatesAtMaxWordLength() {
        var q = question(false, 3, 5, List.of());
        var counts = WordCloudAggregator.aggregate(q, List.of(
                new WordCloudAnswer(List.of("supercalifragilistic"))));
        assertEquals(Map.of("super", 1), counts);
    }

    @Test
    void ignoresEmptyAndNullEntries() {
        var q = question(false, 3, 30, List.of());
        var counts = WordCloudAggregator.aggregate(q, List.of(
                new WordCloudAnswer(java.util.Arrays.asList("hello", "", "   ", null, "world"))));
        assertEquals(Map.of("hello", 1, "world", 1), counts);
    }

    @Test
    void normalize_returnsCleanedList_inOrder() {
        var q = question(false, 3, 30, List.of("ban"));
        var out = WordCloudAggregator.normalize(q, List.of("  Apple ", "ban", "Apple!"));
        assertEquals(List.of("apple", "apple"), out);
    }
}
