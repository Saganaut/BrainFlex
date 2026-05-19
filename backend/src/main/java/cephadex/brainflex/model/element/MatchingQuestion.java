/**
 * Kahoot-style "puzzle pairs" / Mentimeter "Word Match". Players pair items
 * between two columns; the authoritative correct pairing lives on
 * {@code pairs} (each {@link MatchingPair} declares one left/right twin).
 *
 * The runtime shuffles the right column independently of {@code pairs}
 * ordering before broadcasting (see {@link cephadex.brainflex.service.ElementRedactor})
 * so the answer key isn't deducible from positional order; scoring still
 * matches by pair id, so reorders never affect correctness.
 *
 * Scoring follows {@code scoring}: ALL_OR_NOTHING is binary (every pair
 * correct → full points), PARTIAL pro-rates by the count of correct pairings.
 */
package cephadex.brainflex.model.element;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.ElementKind;
import cephadex.brainflex.model.enums.MatchingScoring;
import cephadex.brainflex.model.enums.MediaPosition;
import cephadex.brainflex.model.enums.ResponseMode;

public record MatchingQuestion(
        String id,
        String publicKey,
        String privateKey,
        String title,
        Map<String, Object> styledTitle,
        String prompt,
        List<MatchingPair> pairs,
        MatchingScoring scoring,
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
        return ElementKind.MATCHING;
    }
}
