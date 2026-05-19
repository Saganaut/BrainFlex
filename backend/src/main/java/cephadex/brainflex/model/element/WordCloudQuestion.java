/**
 * Mentimeter-style Word Cloud survey. Players submit up to N short words/phrases
 * which are aggregated server-side into a frequency map and rendered as a cloud.
 *
 * Never scored (`scored=false`, `survey=true` always). Normalization for
 * aggregation happens in {@link cephadex.brainflex.service.WordCloudAggregator}:
 * trim, lower-case (unless {@code caseSensitive}), drop banned words, strip
 * punctuation. {@code maxSubmissionsPerPlayer} caps each player's contribution
 * over a single round; {@code maxWordLength} truncates excessive submissions
 * client-side and is also enforced server-side.
 */
package cephadex.brainflex.model.element;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.ElementKind;
import cephadex.brainflex.model.enums.MediaPosition;
import cephadex.brainflex.model.enums.ResponseMode;

public record WordCloudQuestion(
        String id,
        String publicKey,
        String privateKey,
        String title,
        Map<String, Object> styledTitle,
        String prompt,
        int maxSubmissionsPerPlayer,   // default 3
        int maxWordLength,             // default 30
        boolean caseSensitive,         // default false
        boolean profanityFilter,       // default true
        List<String> bannedWords,      // host-supplied additions
        // scoring (forced to non-scored / survey)
        int pointValue,
        Difficulty difficulty,
        boolean scored,                // always false for word cloud
        boolean survey,                // always true
        Integer multipleSelections,
        ResponseMode responseMode,
        // best-answer modifier (ignored — survey only)
        boolean bestAnswerMode,
        String bestAnswerTitle,
        int bestAnswerBonus,
        String explanation,
        // shared chrome
        int displaySeconds,
        String speakerNotes,
        Image background,
        Image image,
        String videoUrl,
        String audioUrl,
        MediaPosition mediaPosition,
        // shared metadata (chunk 10b)
        String createdByUserId,
        String lastEditedByUserId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<String> tagIds,
        String mediaCaption,
        String altText,
        boolean reactionsEnabled,
        Integer version
) implements DeckElement {

    @Override
    public ElementKind kind() {
        return ElementKind.WORD_CLOUD;
    }
}
