/**
 * Pick cells in an N×M grid. Two scoring modes via `multipleCorrect`:
 *   false — player picks one cell, correct iff that cell is in correctCellIndexes
 *   true  — player picks any subset, correct iff the subset equals correctCellIndexes
 *
 * Cell indexes are row-major (top-left = 0, top-right = cols-1, next row = cols).
 */
package cephadex.brainflex.model.element;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.ElementKind;
import cephadex.brainflex.model.enums.MediaPosition;
import cephadex.brainflex.model.enums.ResponseMode;

public record GridQuestion(
        String id,
        String publicKey,
        String privateKey,
        String title,
        Map<String, Object> styledTitle,
        String prompt,
        int rows,
        int cols,
        GridCellsConfig cells,
        Set<Integer> correctCellIndexes,
        boolean multipleCorrect,
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
        String videoAssetId,
        String audioAssetId,
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
        return ElementKind.GRID;
    }
}
