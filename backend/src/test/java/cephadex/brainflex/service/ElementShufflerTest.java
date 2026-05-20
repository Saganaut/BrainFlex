/**
 * Unit tests for {@link ElementShuffler}. Verifies the deterministic-on-
 * (room, element, player) contract spelled out in chunk 10's README so a
 * reconnecting player resumes with the same arrangement and so different
 * players see independent orderings.
 */
package cephadex.brainflex.service;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import cephadex.brainflex.model.element.DeckElement;
import cephadex.brainflex.model.element.McqOption;
import cephadex.brainflex.model.element.McqQuestion;
import cephadex.brainflex.model.element.NumberQuestion;
import cephadex.brainflex.model.element.RankingItem;
import cephadex.brainflex.model.element.RankingQuestion;
import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.MediaPosition;
import cephadex.brainflex.model.enums.RankingScoring;
import cephadex.brainflex.model.enums.ResponseMode;

class ElementShufflerTest {

    private static final String META_USER = null;
    private static final java.time.LocalDateTime META_TIME = null;
    private static final List<String> META_TAGS = List.of();
    private static final String META_CAPTION = null;
    private static final String META_ALT = null;
    private static final boolean META_REACTIONS = true;
    private static final Integer META_VERSION = 1;

    private static McqQuestion mcq(String id, boolean shuffleOptions, int optionCount) {
        var opts = new java.util.ArrayList<McqOption>();
        for (int i = 0; i < optionCount; i++) {
            opts.add(new McqOption("opt-" + i, "Option " + i, null, null));
        }
        return new McqQuestion(
                id, "pub", "priv", "Title", null,
                "Pick one", opts, List.of("opt-0"),
                100, Difficulty.EASY,
                true, false, null, ResponseMode.ACCEPTING_RESPONSES,
                false, null, 0, null,
                15, null, null, null, null, null, null, null, MediaPosition.NONE,
                shuffleOptions, false, 0,
                META_USER, META_USER, META_TIME, META_TIME, META_TAGS,
                META_CAPTION, META_ALT, META_REACTIONS, META_VERSION);
    }

    private static RankingQuestion ranking(String id, boolean shuffleItems, int itemCount) {
        var items = new java.util.ArrayList<RankingItem>();
        for (int i = 0; i < itemCount; i++) {
            items.add(new RankingItem("item-" + i, "Item " + i, null));
        }
        return new RankingQuestion(
                id, "pub", "priv", "Title", null,
                "Order these", items, List.of(),
                RankingScoring.EXACT,
                100, Difficulty.EASY,
                true, false, null, ResponseMode.ACCEPTING_RESPONSES,
                false, null, 0, null,
                15, null, null, null, null, null, null, null, MediaPosition.NONE,
                shuffleItems,
                META_USER, META_USER, META_TIME, META_TIME, META_TAGS,
                META_CAPTION, META_ALT, META_REACTIONS, META_VERSION);
    }

    private static NumberQuestion number() {
        return new NumberQuestion(
                "num-1", "pub", "priv", "Title", null,
                "How many?", 42.0, 0.0, "units", 0,
                100, Difficulty.EASY,
                true, false, null, ResponseMode.ACCEPTING_RESPONSES,
                false, null, 0, null,
                15, null, null, null, null, null, null, null, MediaPosition.NONE,
                null, null, true,
                META_USER, META_USER, META_TIME, META_TIME, META_TAGS,
                META_CAPTION, META_ALT, META_REACTIONS, META_VERSION);
    }

    private static List<String> optionIds(DeckElement element) {
        return ((McqQuestion) element).options().stream().map(McqOption::id).toList();
    }

    private static List<String> itemIds(DeckElement element) {
        return ((RankingQuestion) element).items().stream().map(RankingItem::id).toList();
    }

    // ---- shouldShuffle ----

    @Test
    void shouldShuffle_returnsFalseWhenMcqFlagOff() {
        assertFalse(ElementShuffler.shouldShuffle(mcq("q1", false, 4)));
    }

    @Test
    void shouldShuffle_returnsTrueWhenMcqFlagOnAndMultipleOptions() {
        assertTrue(ElementShuffler.shouldShuffle(mcq("q1", true, 4)));
    }

    @Test
    void shouldShuffle_returnsFalseWhenMcqHasOneOption() {
        // a one-option list is already in its only possible order
        assertFalse(ElementShuffler.shouldShuffle(mcq("q1", true, 1)));
    }

    @Test
    void shouldShuffle_returnsTrueWhenRankingFlagOnAndMultipleItems() {
        assertTrue(ElementShuffler.shouldShuffle(ranking("r1", true, 4)));
    }

    @Test
    void shouldShuffle_returnsFalseForNonShuffleKind() {
        assertFalse(ElementShuffler.shouldShuffle(number()));
    }

    // ---- deterministic shuffle ----

    @Test
    void shuffleForPlayer_sameInputsProduceSameOrder() {
        McqQuestion question = mcq("q1", true, 6);
        var first = optionIds(ElementShuffler.shuffleForPlayer(question, "ROOM01", "playerA"));
        var second = optionIds(ElementShuffler.shuffleForPlayer(question, "ROOM01", "playerA"));
        assertEquals(first, second, "same seed must produce same order on reconnect");
    }

    @Test
    void shuffleForPlayer_differentPlayersGetIndependentOrders() {
        // With 6 options two distinct seeds nearly always produce different
        // orders. We try several player ids to keep the test robust against
        // an unlucky seed collision.
        McqQuestion question = mcq("q1", true, 6);
        var reference = optionIds(ElementShuffler.shuffleForPlayer(question, "ROOM01", "playerA"));
        boolean foundDifferent = false;
        for (String other : List.of("playerB", "playerC", "playerD", "playerE", "playerF")) {
            var candidate = optionIds(ElementShuffler.shuffleForPlayer(question, "ROOM01", other));
            if (!candidate.equals(reference)) {
                foundDifferent = true;
                break;
            }
        }
        assertTrue(foundDifferent, "at least one other player should get a different order");
    }

    @Test
    void shuffleForPlayer_preservesOptionSet() {
        McqQuestion question = mcq("q1", true, 6);
        var shuffled = optionIds(ElementShuffler.shuffleForPlayer(question, "ROOM01", "playerA"));
        var canonical = optionIds(question);
        assertEquals(canonical.size(), shuffled.size());
        assertTrue(shuffled.containsAll(canonical));
    }

    @Test
    void shuffleForPlayer_differentRoomsGetIndependentOrders() {
        McqQuestion question = mcq("q1", true, 6);
        var reference = optionIds(ElementShuffler.shuffleForPlayer(question, "ROOM01", "playerA"));
        boolean foundDifferent = false;
        for (String room : List.of("ROOM02", "ROOM03", "ROOM04", "ROOM05", "ROOM06")) {
            var candidate = optionIds(ElementShuffler.shuffleForPlayer(question, room, "playerA"));
            if (!candidate.equals(reference)) {
                foundDifferent = true;
                break;
            }
        }
        assertTrue(foundDifferent, "the same player in different rooms should see different orders");
    }

    @Test
    void shuffleForPlayer_returnsInputWhenShuffleDisabled() {
        McqQuestion question = mcq("q1", false, 4);
        DeckElement result = ElementShuffler.shuffleForPlayer(question, "ROOM01", "playerA");
        assertSame(question, result, "shuffle must be a no-op when the flag is off");
    }

    @Test
    void shuffleForPlayer_returnsInputForNonShuffleKind() {
        NumberQuestion question = number();
        DeckElement result = ElementShuffler.shuffleForPlayer(question, "ROOM01", "playerA");
        assertSame(question, result);
    }

    @Test
    void shuffleForPlayer_rankingItemsShuffleDeterministically() {
        RankingQuestion question = ranking("r1", true, 5);
        var first = itemIds(ElementShuffler.shuffleForPlayer(question, "ROOM01", "playerA"));
        var second = itemIds(ElementShuffler.shuffleForPlayer(question, "ROOM01", "playerA"));
        assertEquals(first, second);
    }

    @Test
    void shuffleForPlayer_rankingFlagOffReturnsInput() {
        RankingQuestion question = ranking("r1", false, 5);
        DeckElement result = ElementShuffler.shuffleForPlayer(question, "ROOM01", "playerA");
        assertSame(question, result);
    }

    @Test
    void shuffleForPlayer_differentElementsGetIndependentOrders() {
        // Two questions with identical option lists but different ids should
        // shuffle independently for the same player — otherwise round-to-round
        // the same player would see correlated orderings.
        McqQuestion q1 = mcq("q1", true, 6);
        McqQuestion q2 = mcq("q2", true, 6);
        var a = optionIds(ElementShuffler.shuffleForPlayer(q1, "ROOM01", "playerA"));
        var b = optionIds(ElementShuffler.shuffleForPlayer(q2, "ROOM01", "playerA"));
        assertNotEquals(a, b);
    }
}
