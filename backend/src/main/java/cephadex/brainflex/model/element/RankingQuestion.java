/**
 * Order an item list correctly. Covers both chronological (oldest → newest) and
 * ranking (lowest → highest) framings — they share the same data shape and
 * differ only in the prompt copy.
 *
 * `items` is the shuffled candidate set shown to players; `correctOrder` is the
 * authoritative sequence (each entry an item id). Scoring follows `scoring`:
 * EXACT awards full points only on a perfect match; PARTIAL awards pro-rated
 * credit by the count of items in their correct position.
 */
package cephadex.brainflex.model.element;

import java.util.List;

import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.ElementKind;
import cephadex.brainflex.model.enums.MediaPosition;
import cephadex.brainflex.model.enums.RankingScoring;

public record RankingQuestion(
        String id,
        String prompt,
        List<RankingItem> items,
        List<String> correctOrder,     // list of item ids in the correct sequence
        RankingScoring scoring,
        // scoring
        int pointValue,
        Difficulty difficulty,
        // best-answer modifier
        boolean bestAnswerMode,
        int bestAnswerBonus,
        String explanation,
        // shared chrome
        int displaySeconds,
        String hostNotes,
        String backgroundImageUrl,
        String imageUrl,
        String videoUrl,
        String audioUrl,
        MediaPosition mediaPosition
) implements DeckElement {

    @Override
    public ElementKind kind() {
        return ElementKind.RANKING;
    }
}
