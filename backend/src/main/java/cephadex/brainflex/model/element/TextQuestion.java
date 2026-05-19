/**
 * Free-text question. The submitted text is matched case-insensitively (unless
 * caseSensitive=true) against the canonical answer or any acceptedVariants.
 * Trimming is governed by `trimWhitespace` (default true). When `fuzzyMatch`
 * is enabled the scorer also accepts answers within `fuzzyDistance` Levenshtein
 * edits of the canonical or any variant — useful for typos and minor spelling
 * slips. `maxLength` is a client-side cap on the input (default 80).
 */
package cephadex.brainflex.model.element;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.ElementKind;
import cephadex.brainflex.model.enums.MediaPosition;
import cephadex.brainflex.model.enums.ResponseMode;

public record TextQuestion(
        String id,
        String publicKey,
        String privateKey,
        String title,
        Map<String, Object> styledTitle,
        String prompt,
        String correctAnswer,
        List<String> acceptedVariants,   // additional strings that also count as correct
        boolean caseSensitive,
        // scoring
        int pointValue,
        Difficulty difficulty,
        boolean scored,
        boolean survey,
        Integer multipleSelections,
        ResponseMode responseMode,
        // best-answer modifier
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
        // per-kind ergonomics (chunk 10)
        int maxLength,
        boolean trimWhitespace,
        boolean fuzzyMatch,
        int fuzzyDistance,
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
        return ElementKind.TEXT;
    }
}
