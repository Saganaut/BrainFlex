/**
 * Mentimeter-style Word Cloud survey. Players submit up to N short words/phrases
 * which are aggregated server-side into a frequency map and rendered as a cloud.
 *
 * Never scored (`scored=false`, `survey=true` always — both on {@link ElementChrome}).
 * Normalization for aggregation happens in
 * {@link cephadex.brainflex.service.WordCloudAggregator}: trim, lower-case
 * (unless {@code caseSensitive}), drop banned words, strip punctuation.
 * {@code maxSubmissionsPerPlayer} caps each player's contribution over a single
 * round; {@code maxWordLength} truncates excessive submissions client-side and
 * is also enforced server-side.
 */
package cephadex.brainflex.model.element;

import java.util.List;

import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.ElementKind;

public record WordCloudQuestion(
        String id,
        String prompt,
        int maxSubmissionsPerPlayer,   // default 3
        int maxWordLength,             // default 30
        boolean caseSensitive,         // default false
        boolean profanityFilter,       // default true
        List<String> bannedWords,      // host-supplied additions
        // scoring (question-only — not chrome; always 0 / unscored)
        int pointValue,
        Difficulty difficulty,
        String explanation,
        // shared chrome (chunk 25)
        ElementChrome chrome
) implements DeckElement {

    @Override
    public ElementKind kind() {
        return ElementKind.WORD_CLOUD;
    }
}
