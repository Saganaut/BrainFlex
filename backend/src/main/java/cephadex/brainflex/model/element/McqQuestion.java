/**
 * Multiple-choice question with 2 to 6 options and one OR MORE correct answers.
 *
 * Scoring uses option ids rather than indexes so the editor can shuffle the
 * option list freely (and clients can render in any order) without recomputing
 * a "correct index". `correctOptionIds` is the set of option ids that count as
 * correct — an MCQ may have any number (zero excludes it from scored game
 * modes; one is the typical case; many means "any of these is acceptable").
 *
 * Multi-select is governed by `allowMultipleSelect`. When `false` (the default
 * Kahoot-style behavior) the scorer rejects payloads carrying more than one
 * option id. When `true`, the player may submit up to `maxSelections` ids;
 * `maxSelections=0` means unlimited (capped by the option count). The legacy
 * `multipleSelections` field is still emitted for the player-side display
 * limit but is no longer the source of truth for scoring eligibility.
 *
 * `shuffleOptions` is a per-player presentation flag honoured by
 * InteractiveSessionService.startRound — when set, each player sees a different (but
 * deterministic-on-reconnect) ordering of `options[]`.
 */
package cephadex.brainflex.model.element;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.ElementKind;
import cephadex.brainflex.model.enums.MediaPosition;
import cephadex.brainflex.model.enums.ResponseMode;

public record McqQuestion(
        String id,
        String publicKey,
        String privateKey,
        String title,
        Map<String, Object> styledTitle,
        String prompt,
        List<McqOption> options,
        List<String> correctOptionIds, // ids of every option that counts as correct
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
        // per-kind ergonomics (chunk 10)
        boolean shuffleOptions,
        boolean allowMultipleSelect,
        int maxSelections,
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
        return ElementKind.MCQ;
    }
}
