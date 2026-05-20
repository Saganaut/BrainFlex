/**
 * Order an item list correctly. Covers both chronological (oldest to newest) and
 * ranking (lowest to highest) framings — they share the same data shape and
 * differ only in the prompt copy.
 *
 * `items` is the shuffled candidate set shown to players; `correctOrder` is the
 * authoritative sequence (each entry an item id). Scoring follows `scoring`:
 * EXACT awards full points only on a perfect match; PARTIAL awards pro-rated
 * credit by the count of items in their correct position.
 *
 * `shuffleItemsForPresentation` is a per-player presentation flag honoured by
 * InteractiveSessionService.startRound — when set, each player sees a different (but
 * deterministic-on-reconnect) ordering of `items[]`.
 */
package cephadex.brainflex.model.element;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.ElementKind;
import cephadex.brainflex.model.enums.MediaPosition;
import cephadex.brainflex.model.enums.RankingScoring;
import cephadex.brainflex.model.enums.ResponseMode;

public record RankingQuestion(
        String id,
        String publicKey,
        String privateKey,
        String title,
        Map<String, Object> styledTitle,
        String prompt,
        List<RankingItem> items,
        List<String> correctOrder,     // list of item ids in the correct sequence
        RankingScoring scoring,
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
        boolean shuffleItemsForPresentation,
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
        return ElementKind.RANKING;
    }
}
