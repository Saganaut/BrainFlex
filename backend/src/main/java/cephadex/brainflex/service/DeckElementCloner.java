/**
 * Utility for re-emitting a DeckElement with a different id, a substituted
 * child list, or a fresh {@link ElementChrome}. Records are immutable so we
 * need a polymorphic clone per kind; this is the single place that knows the
 * full constructor of every element type.
 *
 * Chunk 25 collapsed the ~30 shared "chrome" components on each record into a
 * single {@link ElementChrome} value, so the switch arms here got an order of
 * magnitude shorter — most just hand the chrome through unchanged.
 */
package cephadex.brainflex.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cephadex.brainflex.model.element.AllocationQuestion;
import cephadex.brainflex.model.element.DeckElement;
import cephadex.brainflex.model.element.DrawingQuestion;
import cephadex.brainflex.model.element.ElementChrome;
import cephadex.brainflex.model.element.GridQuestion;
import cephadex.brainflex.model.element.MatchingPair;
import cephadex.brainflex.model.element.MatchingQuestion;
import cephadex.brainflex.model.element.McqOption;
import cephadex.brainflex.model.element.McqQuestion;
import cephadex.brainflex.model.element.NumberQuestion;
import cephadex.brainflex.model.element.PlaceOnImageQuestion;
import cephadex.brainflex.model.element.QAndAQuestion;
import cephadex.brainflex.model.element.RankingQuestion;
import cephadex.brainflex.model.element.ScalesQuestion;
import cephadex.brainflex.model.element.Slide;
import cephadex.brainflex.model.element.TextQuestion;
import cephadex.brainflex.model.element.WordCloudQuestion;

public final class DeckElementCloner {

    private DeckElementCloner() {}

    /** Clone an McqQuestion with a substituted options list. */
    public static McqQuestion withOptions(McqQuestion q, List<McqOption> options) {
        return new McqQuestion(
                q.id(), q.prompt(), options, q.correctOptionIds(),
                q.pointValue(), q.difficulty(), q.explanation(),
                q.shuffleOptions(), q.allowMultipleSelect(), q.maxSelections(),
                q.chrome());
    }

    /** Clone the element with the same chrome but a different id. */
    public static DeckElement withId(DeckElement element, String id) {
        return withIdAndChrome(element, id, element.chrome());
    }

    /** Clone the element with its existing id but a substituted chrome. */
    public static DeckElement withChrome(DeckElement element, ElementChrome chrome) {
        return withIdAndChrome(element, element.id(), chrome);
    }

    /**
     * Clone the element with the shared-metadata block (chunk 10b) replaced.
     * Everything else — id, kind-specific payload, the rest of chrome — passes
     * through. Used by {@link DeckService} to stamp provenance on add/update
     * without each controller knowing the per-kind canonical constructor.
     */
    public static DeckElement withMetadata(
            DeckElement element,
            String createdByUserId, String lastEditedByUserId,
            LocalDateTime createdAt, LocalDateTime updatedAt,
            List<String> tagIds, String mediaCaption, String altText,
            boolean reactionsEnabled, Integer version) {
        ElementChrome fresh = element.chrome().withMetadata(
                createdByUserId, lastEditedByUserId, createdAt, updatedAt,
                tagIds, mediaCaption, altText, reactionsEnabled, version);
        return withChrome(element, fresh);
    }

    private static DeckElement withIdAndChrome(DeckElement element, String id, ElementChrome chrome) {
        return switch (element) {
            case Slide s -> new Slide(
                    id, s.slideKind(), s.body(), s.blocks(),
                    s.resultsDisplayType(), s.multipleSelectionsEnabled(),
                    s.selectionsPerParticipant(), s.showResultsAsPercentage(),
                    s.joinType(), s.showJoinInformation(), s.showQrCode(), s.showResponses(),
                    s.heading(), s.participantInformation(),
                    s.autoAdvanceSeconds(),
                    chrome);
            case McqQuestion q -> new McqQuestion(
                    id, q.prompt(), q.options(), q.correctOptionIds(),
                    q.pointValue(), q.difficulty(), q.explanation(),
                    q.shuffleOptions(), q.allowMultipleSelect(), q.maxSelections(),
                    chrome);
            case TextQuestion q -> new TextQuestion(
                    id, q.prompt(), q.correctAnswer(), q.acceptedVariants(), q.caseSensitive(),
                    q.pointValue(), q.difficulty(), q.explanation(),
                    q.maxLength(), q.trimWhitespace(), q.fuzzyMatch(), q.fuzzyDistance(),
                    chrome);
            case NumberQuestion q -> new NumberQuestion(
                    id, q.prompt(), q.correctValue(), q.tolerance(), q.unitLabel(), q.decimalPlaces(),
                    q.pointValue(), q.difficulty(), q.explanation(),
                    q.minValue(), q.maxValue(), q.allowNegative(),
                    chrome);
            case RankingQuestion q -> new RankingQuestion(
                    id, q.prompt(), q.items(), q.correctOrder(), q.scoring(),
                    q.pointValue(), q.difficulty(), q.explanation(),
                    q.shuffleItemsForPresentation(),
                    chrome);
            case ScalesQuestion q -> new ScalesQuestion(
                    id, q.prompt(), q.statements(),
                    q.scaleMin(), q.scaleMax(), q.minLabel(), q.maxLabel(), q.correctRatings(),
                    q.pointValue(), q.difficulty(), q.explanation(),
                    chrome);
            case QAndAQuestion q -> new QAndAQuestion(
                    id, q.prompt(), q.maxSubmissionsPerPlayer(), q.allowVoting(), q.autoApprove(),
                    q.pointValue(), q.difficulty(), q.explanation(),
                    q.anonymousSubmissions(), q.minVotesToShow(),
                    chrome);
            case GridQuestion q -> new GridQuestion(
                    id, q.prompt(), q.rows(), q.cols(), q.cells(),
                    q.correctCellIndexes(), q.multipleCorrect(),
                    q.pointValue(), q.difficulty(), q.explanation(),
                    chrome);
            case PlaceOnImageQuestion q -> new PlaceOnImageQuestion(
                    id, q.prompt(), q.targetImage(),
                    q.correctX(), q.correctY(), q.tolerance(), q.scoring(),
                    q.pointValue(), q.difficulty(), q.explanation(),
                    chrome);
            case WordCloudQuestion q -> new WordCloudQuestion(
                    id, q.prompt(), q.maxSubmissionsPerPlayer(), q.maxWordLength(),
                    q.caseSensitive(), q.profanityFilter(), q.bannedWords(),
                    q.pointValue(), q.difficulty(), q.explanation(),
                    chrome);
            case AllocationQuestion q -> new AllocationQuestion(
                    id, q.prompt(), q.options(),
                    q.totalPointsToDistribute(), q.allowZeroOnItem(), q.enforceExactTotal(),
                    q.pointValue(), q.difficulty(), q.explanation(),
                    chrome);
            // withId on Matching re-mints pair ids so the clone's pairs are independent
            // of the source's; an in-place chrome swap (id unchanged) keeps the original pairs.
            case MatchingQuestion q -> new MatchingQuestion(
                    id, q.prompt(),
                    id.equals(q.id()) ? q.pairs() : regenPairIds(q.pairs()),
                    q.scoring(),
                    q.pointValue(), q.difficulty(), q.explanation(),
                    chrome);
            case DrawingQuestion q -> new DrawingQuestion(
                    id, q.prompt(), q.backingImage(),
                    q.canvasWidth(), q.canvasHeight(),
                    q.maxStrokesPerPlayer(), q.maxPointsPerStroke(), q.palette(),
                    q.pointValue(), q.difficulty(), q.explanation(),
                    chrome);
        };
    }

    /** Re-mint each MatchingPair id when cloning so the clone's pairs are
     *  independent of the source's. Labels and images carry across unchanged. */
    private static List<MatchingPair> regenPairIds(List<MatchingPair> pairs) {
        if (pairs == null) return null;
        List<MatchingPair> renewed = new ArrayList<>(pairs.size());
        for (MatchingPair pair : pairs) {
            renewed.add(new MatchingPair(
                    UUID.randomUUID().toString(),
                    pair.leftLabel(), pair.rightLabel(),
                    pair.leftImage(), pair.rightImage()));
        }
        return renewed;
    }
}
