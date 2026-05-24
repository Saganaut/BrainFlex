/**
 * Pure utility for rolling up word-cloud submissions into a frequency map.
 *
 * Normalization (lower-case unless {@code caseSensitive}; trim; strip
 * surrounding punctuation; drop empty + banned words) is centralized here so
 * the live-reveal WebSocket push, the final reveal frame, and the
 * deck-analytics rollup all see identical counts.
 */
package cephadex.brainflex.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import cephadex.brainflex.model.answer.WordCloudAnswer;
import cephadex.brainflex.model.element.WordCloudQuestion;

public final class WordCloudAggregator {

    private WordCloudAggregator() {
    }

    /**
     * Roll up a collection of submissions into a {@code word -> count} map.
     * Insertion order is preserved so the first occurrence drives display
     * casing (relevant when {@code caseSensitive=false} — two submissions
     * differing only in case collapse onto the form first seen).
     */
    @SuppressWarnings("null")
    public static Map<String, Integer> aggregate(WordCloudQuestion question,
            Collection<WordCloudAnswer> answers) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (answers == null || answers.isEmpty())
            return counts;

        Set<String> banned = normalizedBannedWords(question);

        for (WordCloudAnswer answer : answers) {
            if (answer == null || answer.words() == null)
                continue;
            int taken = 0;
            int cap = question.maxSubmissionsPerPlayer() > 0
                    ? question.maxSubmissionsPerPlayer()
                    : Integer.MAX_VALUE;
            for (String raw : answer.words()) {
                if (taken >= cap)
                    break;
                String normalized = normalize(raw, question);
                if (normalized.isEmpty())
                    continue;
                if (banned.contains(toComparable(normalized, question)))
                    continue;
                counts.merge(normalized, 1, Integer::sum);
                taken++;
            }
        }
        return counts;
    }

    /**
     * Lighter convenience for one submission — same normalization, returns the
     * sanitized words to write back to PlayerAnswer.payload (so the historical
     * record matches what the aggregator counted).
     */
    public static List<String> normalize(WordCloudQuestion question, List<String> words) {
        if (words == null || words.isEmpty())
            return List.of();
        Set<String> banned = normalizedBannedWords(question);
        int cap = question.maxSubmissionsPerPlayer() > 0
                ? question.maxSubmissionsPerPlayer()
                : Integer.MAX_VALUE;
        List<String> out = new java.util.ArrayList<>();
        for (String raw : words) {
            if (out.size() >= cap)
                break;
            String normalized = normalize(raw, question);
            if (normalized.isEmpty())
                continue;
            if (banned.contains(toComparable(normalized, question)))
                continue;
            out.add(normalized);
        }
        return out;
    }

    private static String normalize(String raw, WordCloudQuestion q) {
        if (raw == null)
            return "";
        String trimmed = raw.trim();
        // Strip leading/trailing punctuation; keep internal characters so
        // "don't" and "co-op" survive intact.
        trimmed = trimmed.replaceAll("^[\\p{Punct}\\s]+|[\\p{Punct}\\s]+$", "");
        if (trimmed.isEmpty())
            return "";
        int max = q.maxWordLength() > 0 ? q.maxWordLength() : Integer.MAX_VALUE;
        if (trimmed.length() > max)
            trimmed = trimmed.substring(0, max);
        if (!q.caseSensitive())
            trimmed = trimmed.toLowerCase(Locale.ROOT);
        return trimmed;
    }

    private static String toComparable(String word, WordCloudQuestion q) {
        return q.caseSensitive() ? word : word.toLowerCase(Locale.ROOT);
    }

    private static Set<String> normalizedBannedWords(WordCloudQuestion q) {
        Set<String> banned = new HashSet<>();
        if (q.bannedWords() != null) {
            for (String w : q.bannedWords()) {
                if (w == null)
                    continue;
                String norm = w.trim();
                if (norm.isEmpty())
                    continue;
                banned.add(q.caseSensitive() ? norm : norm.toLowerCase(Locale.ROOT));
            }
        }
        return banned;
    }
}
