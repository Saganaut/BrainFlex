/**
 * Mentimeter-style "100 Points" survey. Players distribute a fixed pool of
 * points across N options to express weighted preferences.
 *
 * Never scored by contract ({@code scored=false}, {@code survey=true}). Reuses
 * {@link McqOption} so authors get the same option-list editor (text + optional
 * thumbnail + color) used for MCQ.
 *
 * {@code totalPointsToDistribute} caps each player's distribution (default 100).
 * {@code allowZeroOnItem} controls whether a per-option score of zero is valid.
 * {@code enforceExactTotal=true} rejects submissions whose sum != the pool;
 * {@code =false} lets partial distributions through (server clamps overruns).
 */
package cephadex.brainflex.model.element;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.ElementKind;
import cephadex.brainflex.model.enums.MediaPosition;
import cephadex.brainflex.model.enums.ResponseMode;

public record AllocationQuestion(
        String id,
        String publicKey,
        String privateKey,
        String title,
        Map<String, Object> styledTitle,
        String prompt,
        List<McqOption> options,
        int totalPointsToDistribute,   // default 100
        boolean allowZeroOnItem,       // default true
        boolean enforceExactTotal,     // default true
        // scoring (forced to non-scored / survey)
        int pointValue,
        Difficulty difficulty,
        boolean scored,                // always false for allocation
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
        return ElementKind.ALLOCATION;
    }
}
