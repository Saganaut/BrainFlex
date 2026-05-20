/**
 * Produces a per-player view of a DeckElement with answer-bearing collections
 * permuted by a stable seed.
 *
 * Chunk 10 — common element additions — calls for {@code McqQuestion.shuffleOptions}
 * and {@code RankingQuestion.shuffleItemsForPresentation} to surface a
 * different (but reproducible) order to each player. Determinism is keyed on
 * {@code roomCode + "|" + elementId + "|" + playerId} so a player who
 * reconnects mid-round sees the same arrangement they were rendering before
 * the drop without the server having to store a permutation table.
 *
 * The shuffler operates on the already-redacted element returned by
 * {@link ElementRedactor}: it never reads correct-answer fields and never
 * exposes per-player ordering to anyone but the target player. The base
 * topic broadcast ({@code /topic/interactive-session/{roomCode}/round}) keeps the
 * authoring order so the host/projector view stays canonical.
 *
 * Kinds without an order-sensitive presentation list (Slide, TextQuestion,
 * NumberQuestion, etc.) are returned unchanged.
 */
package cephadex.brainflex.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import cephadex.brainflex.model.element.DeckElement;
import cephadex.brainflex.model.element.McqOption;
import cephadex.brainflex.model.element.McqQuestion;
import cephadex.brainflex.model.element.RankingItem;
import cephadex.brainflex.model.element.RankingQuestion;

public final class ElementShuffler {

    private ElementShuffler() {}

    /**
     * Returns the element to send to {@code playerId} for this round. When the
     * element opts into per-player shuffle, the returned record carries a
     * permuted copy of the options/items list seeded on the (roomCode,
     * elementId, playerId) tuple. Otherwise the input is returned unchanged.
     */
    public static DeckElement shuffleForPlayer(DeckElement element, String roomCode, String playerId) {
        return switch (element) {
            case McqQuestion q when q.shuffleOptions() && q.options() != null && q.options().size() > 1 ->
                    withShuffledOptions(q, permute(q.options(), seedFor(roomCode, q.id(), playerId)));
            case RankingQuestion q
                    when q.shuffleItemsForPresentation() && q.items() != null && q.items().size() > 1 ->
                    withShuffledItems(q, permute(q.items(), seedFor(roomCode, q.id(), playerId)));
            default -> element;
        };
    }

    /**
     * True when the element opts into per-player shuffle for at least one of
     * its presentation lists. The service uses this to skip the per-user fan
     * out when the work is a no-op for every player.
     */
    public static boolean shouldShuffle(DeckElement element) {
        return switch (element) {
            case McqQuestion q -> q.shuffleOptions() && q.options() != null && q.options().size() > 1;
            case RankingQuestion q ->
                    q.shuffleItemsForPresentation() && q.items() != null && q.items().size() > 1;
            default -> false;
        };
    }

    private static long seedFor(String roomCode, String elementId, String playerId) {
        return (roomCode + "|" + elementId + "|" + playerId).hashCode();
    }

    private static <T> List<T> permute(List<T> source, long seed) {
        List<T> copy = new ArrayList<>(source);
        Collections.shuffle(copy, new Random(seed));
        return copy;
    }

    private static McqQuestion withShuffledOptions(McqQuestion q, List<McqOption> options) {
        return new McqQuestion(
                q.id(), q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                q.prompt(), options,
                q.correctOptionIds(),
                q.pointValue(), q.difficulty(),
                q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(),
                q.explanation(),
                q.displaySeconds(), q.speakerNotes(), q.background(),
                q.image(), q.videoUrl(), q.audioUrl(), q.videoAssetId(), q.audioAssetId(), q.mediaPosition(),
                q.shuffleOptions(), q.allowMultipleSelect(), q.maxSelections(),
                q.createdByUserId(), q.lastEditedByUserId(), q.createdAt(), q.updatedAt(),
                q.tagIds(), q.mediaCaption(), q.altText(), q.reactionsEnabled(), q.version());
    }

    private static RankingQuestion withShuffledItems(RankingQuestion q, List<RankingItem> items) {
        return new RankingQuestion(
                q.id(), q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                q.prompt(), items,
                q.correctOrder(),
                q.scoring(),
                q.pointValue(), q.difficulty(),
                q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(),
                q.explanation(),
                q.displaySeconds(), q.speakerNotes(), q.background(),
                q.image(), q.videoUrl(), q.audioUrl(), q.videoAssetId(), q.audioAssetId(), q.mediaPosition(),
                q.shuffleItemsForPresentation(),
                q.createdByUserId(), q.lastEditedByUserId(), q.createdAt(), q.updatedAt(),
                q.tagIds(), q.mediaCaption(), q.altText(), q.reactionsEnabled(), q.version());
    }
}
