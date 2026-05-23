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

import java.util.List;

import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.ElementKind;
import cephadex.brainflex.model.enums.RankingScoring;
import cephadex.brainflex.model.element.parts.RankingItem;

public record RankingQuestion(
        String id,
        String prompt,
        List<RankingItem> items,
        List<String> correctOrder,     // list of item ids in the correct sequence
        RankingScoring scoring,
        // scoring (question-only — not chrome)
        int pointValue,
        Difficulty difficulty,
        String explanation,
        // per-kind ergonomics (chunk 10)
        boolean shuffleItemsForPresentation,
        // shared chrome (chunk 25)
        ElementChrome chrome
) implements DeckElement {

    @Override
    public ElementKind kind() {
        return ElementKind.RANKING;
    }
}
