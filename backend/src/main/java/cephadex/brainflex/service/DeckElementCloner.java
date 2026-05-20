/**
 * Utility for re-emitting a DeckElement with a different id or with a
 * substituted child list. Records are immutable so we need a polymorphic
 * clone per kind; this is the single place that knows the full constructor
 * of every element type.
 */
package cephadex.brainflex.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cephadex.brainflex.model.element.AllocationQuestion;
import cephadex.brainflex.model.element.DeckElement;
import cephadex.brainflex.model.element.DrawingQuestion;
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
                q.id(), q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                q.prompt(), options, q.correctOptionIds(),
                q.pointValue(), q.difficulty(),
                q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(), q.explanation(),
                q.displaySeconds(), q.speakerNotes(), q.background(),
                q.image(), q.videoUrl(), q.audioUrl(), q.videoAssetId(), q.audioAssetId(), q.mediaPosition(),
                q.shuffleOptions(), q.allowMultipleSelect(), q.maxSelections(),
                q.createdByUserId(), q.lastEditedByUserId(), q.createdAt(), q.updatedAt(),
                q.tagIds(), q.mediaCaption(), q.altText(), q.reactionsEnabled(), q.version());
    }

    public static DeckElement withId(DeckElement element, String id) {
        return switch (element) {
            case Slide s -> new Slide(
                    id, s.slideKind(), s.publicKey(), s.privateKey(),
                    s.title(), s.styledTitle(), s.body(),
                    s.scored(), s.survey(), s.multipleSelections(), s.responseMode(),
                    s.displaySeconds(), s.speakerNotes(), s.background(),
                    s.image(), s.videoUrl(), s.audioUrl(), s.videoAssetId(), s.audioAssetId(), s.mediaPosition(),
                    s.resultsDisplayType(), s.multipleSelectionsEnabled(),
                    s.selectionsPerParticipant(), s.showResultsAsPercentage(),
                    s.joinType(), s.showJoinInformation(), s.showQrCode(), s.showResponses(),
                    s.heading(), s.participantInformation(),
                    s.autoAdvanceSeconds(),
                    s.createdByUserId(), s.lastEditedByUserId(), s.createdAt(), s.updatedAt(),
                    s.tagIds(), s.mediaCaption(), s.altText(), s.reactionsEnabled(), s.version());
            case McqQuestion q -> new McqQuestion(
                    id, q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), q.options(), q.correctOptionIds(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.speakerNotes(), q.background(),
                    q.image(), q.videoUrl(), q.audioUrl(), q.videoAssetId(), q.audioAssetId(), q.mediaPosition(),
                    q.shuffleOptions(), q.allowMultipleSelect(), q.maxSelections(),
                    q.createdByUserId(), q.lastEditedByUserId(), q.createdAt(), q.updatedAt(),
                    q.tagIds(), q.mediaCaption(), q.altText(), q.reactionsEnabled(), q.version());
            case TextQuestion q -> new TextQuestion(
                    id, q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), q.correctAnswer(), q.acceptedVariants(), q.caseSensitive(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.speakerNotes(), q.background(),
                    q.image(), q.videoUrl(), q.audioUrl(), q.videoAssetId(), q.audioAssetId(), q.mediaPosition(),
                    q.maxLength(), q.trimWhitespace(), q.fuzzyMatch(), q.fuzzyDistance(),
                    q.createdByUserId(), q.lastEditedByUserId(), q.createdAt(), q.updatedAt(),
                    q.tagIds(), q.mediaCaption(), q.altText(), q.reactionsEnabled(), q.version());
            case NumberQuestion q -> new NumberQuestion(
                    id, q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), q.correctValue(), q.tolerance(), q.unitLabel(), q.decimalPlaces(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.speakerNotes(), q.background(),
                    q.image(), q.videoUrl(), q.audioUrl(), q.videoAssetId(), q.audioAssetId(), q.mediaPosition(),
                    q.minValue(), q.maxValue(), q.allowNegative(),
                    q.createdByUserId(), q.lastEditedByUserId(), q.createdAt(), q.updatedAt(),
                    q.tagIds(), q.mediaCaption(), q.altText(), q.reactionsEnabled(), q.version());
            case RankingQuestion q -> new RankingQuestion(
                    id, q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), q.items(), q.correctOrder(), q.scoring(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.speakerNotes(), q.background(),
                    q.image(), q.videoUrl(), q.audioUrl(), q.videoAssetId(), q.audioAssetId(), q.mediaPosition(),
                    q.shuffleItemsForPresentation(),
                    q.createdByUserId(), q.lastEditedByUserId(), q.createdAt(), q.updatedAt(),
                    q.tagIds(), q.mediaCaption(), q.altText(), q.reactionsEnabled(), q.version());
            case ScalesQuestion q -> new ScalesQuestion(
                    id, q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), q.statements(), q.scaleMin(), q.scaleMax(),
                    q.minLabel(), q.maxLabel(), q.correctRatings(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.speakerNotes(), q.background(),
                    q.image(), q.videoUrl(), q.audioUrl(), q.videoAssetId(), q.audioAssetId(), q.mediaPosition(),
                    q.createdByUserId(), q.lastEditedByUserId(), q.createdAt(), q.updatedAt(),
                    q.tagIds(), q.mediaCaption(), q.altText(), q.reactionsEnabled(), q.version());
            case QAndAQuestion q -> new QAndAQuestion(
                    id, q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), q.maxSubmissionsPerPlayer(), q.allowVoting(), q.autoApprove(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.speakerNotes(), q.background(),
                    q.image(), q.videoUrl(), q.audioUrl(), q.videoAssetId(), q.audioAssetId(), q.mediaPosition(),
                    q.anonymousSubmissions(), q.minVotesToShow(),
                    q.createdByUserId(), q.lastEditedByUserId(), q.createdAt(), q.updatedAt(),
                    q.tagIds(), q.mediaCaption(), q.altText(), q.reactionsEnabled(), q.version());
            case GridQuestion q -> new GridQuestion(
                    id, q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), q.rows(), q.cols(), q.cells(),
                    q.correctCellIndexes(), q.multipleCorrect(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.speakerNotes(), q.background(),
                    q.image(), q.videoUrl(), q.audioUrl(), q.videoAssetId(), q.audioAssetId(), q.mediaPosition(),
                    q.createdByUserId(), q.lastEditedByUserId(), q.createdAt(), q.updatedAt(),
                    q.tagIds(), q.mediaCaption(), q.altText(), q.reactionsEnabled(), q.version());
            case PlaceOnImageQuestion q -> new PlaceOnImageQuestion(
                    id, q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), q.targetImage(),
                    q.correctX(), q.correctY(), q.tolerance(), q.scoring(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.speakerNotes(), q.background(),
                    q.image(), q.videoUrl(), q.audioUrl(), q.videoAssetId(), q.audioAssetId(), q.mediaPosition(),
                    q.createdByUserId(), q.lastEditedByUserId(), q.createdAt(), q.updatedAt(),
                    q.tagIds(), q.mediaCaption(), q.altText(), q.reactionsEnabled(), q.version());
            case WordCloudQuestion q -> new WordCloudQuestion(
                    id, q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), q.maxSubmissionsPerPlayer(), q.maxWordLength(),
                    q.caseSensitive(), q.profanityFilter(), q.bannedWords(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.speakerNotes(), q.background(),
                    q.image(), q.videoUrl(), q.audioUrl(), q.videoAssetId(), q.audioAssetId(), q.mediaPosition(),
                    q.createdByUserId(), q.lastEditedByUserId(), q.createdAt(), q.updatedAt(),
                    q.tagIds(), q.mediaCaption(), q.altText(), q.reactionsEnabled(), q.version());
            case AllocationQuestion q -> new AllocationQuestion(
                    id, q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), q.options(),
                    q.totalPointsToDistribute(), q.allowZeroOnItem(), q.enforceExactTotal(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.speakerNotes(), q.background(),
                    q.image(), q.videoUrl(), q.audioUrl(), q.videoAssetId(), q.audioAssetId(), q.mediaPosition(),
                    q.createdByUserId(), q.lastEditedByUserId(), q.createdAt(), q.updatedAt(),
                    q.tagIds(), q.mediaCaption(), q.altText(), q.reactionsEnabled(), q.version());
            case MatchingQuestion q -> new MatchingQuestion(
                    id, q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), regenPairIds(q.pairs()), q.scoring(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.speakerNotes(), q.background(),
                    q.image(), q.videoUrl(), q.audioUrl(), q.videoAssetId(), q.audioAssetId(), q.mediaPosition(),
                    q.createdByUserId(), q.lastEditedByUserId(), q.createdAt(), q.updatedAt(),
                    q.tagIds(), q.mediaCaption(), q.altText(), q.reactionsEnabled(), q.version());
            case DrawingQuestion q -> new DrawingQuestion(
                    id, q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), q.backingImage(),
                    q.canvasWidth(), q.canvasHeight(),
                    q.maxStrokesPerPlayer(), q.maxPointsPerStroke(), q.palette(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.speakerNotes(), q.background(),
                    q.image(), q.videoUrl(), q.audioUrl(), q.videoAssetId(), q.audioAssetId(), q.mediaPosition(),
                    q.createdByUserId(), q.lastEditedByUserId(), q.createdAt(), q.updatedAt(),
                    q.tagIds(), q.mediaCaption(), q.altText(), q.reactionsEnabled(), q.version());
        };
    }

    /**
     * Clone the element with the shared-metadata block (chunk 10b) replaced.
     * Everything else — id, kind-specific payload, chrome — passes through. Used
     * by {@link DeckService} to stamp provenance on add/update without each
     * controller knowing the per-kind canonical constructor.
     */
    public static DeckElement withMetadata(
            DeckElement element,
            String createdByUserId, String lastEditedByUserId,
            LocalDateTime createdAt, LocalDateTime updatedAt,
            List<String> tagIds, String mediaCaption, String altText,
            boolean reactionsEnabled, Integer version) {
        return switch (element) {
            case Slide s -> new Slide(
                    s.id(), s.slideKind(), s.publicKey(), s.privateKey(),
                    s.title(), s.styledTitle(), s.body(),
                    s.scored(), s.survey(), s.multipleSelections(), s.responseMode(),
                    s.displaySeconds(), s.speakerNotes(), s.background(),
                    s.image(), s.videoUrl(), s.audioUrl(), s.videoAssetId(), s.audioAssetId(), s.mediaPosition(),
                    s.resultsDisplayType(), s.multipleSelectionsEnabled(),
                    s.selectionsPerParticipant(), s.showResultsAsPercentage(),
                    s.joinType(), s.showJoinInformation(), s.showQrCode(), s.showResponses(),
                    s.heading(), s.participantInformation(),
                    s.autoAdvanceSeconds(),
                    createdByUserId, lastEditedByUserId, createdAt, updatedAt,
                    tagIds, mediaCaption, altText, reactionsEnabled, version);
            case McqQuestion q -> new McqQuestion(
                    q.id(), q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), q.options(), q.correctOptionIds(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.speakerNotes(), q.background(),
                    q.image(), q.videoUrl(), q.audioUrl(), q.videoAssetId(), q.audioAssetId(), q.mediaPosition(),
                    q.shuffleOptions(), q.allowMultipleSelect(), q.maxSelections(),
                    createdByUserId, lastEditedByUserId, createdAt, updatedAt,
                    tagIds, mediaCaption, altText, reactionsEnabled, version);
            case TextQuestion q -> new TextQuestion(
                    q.id(), q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), q.correctAnswer(), q.acceptedVariants(), q.caseSensitive(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.speakerNotes(), q.background(),
                    q.image(), q.videoUrl(), q.audioUrl(), q.videoAssetId(), q.audioAssetId(), q.mediaPosition(),
                    q.maxLength(), q.trimWhitespace(), q.fuzzyMatch(), q.fuzzyDistance(),
                    createdByUserId, lastEditedByUserId, createdAt, updatedAt,
                    tagIds, mediaCaption, altText, reactionsEnabled, version);
            case NumberQuestion q -> new NumberQuestion(
                    q.id(), q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), q.correctValue(), q.tolerance(), q.unitLabel(), q.decimalPlaces(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.speakerNotes(), q.background(),
                    q.image(), q.videoUrl(), q.audioUrl(), q.videoAssetId(), q.audioAssetId(), q.mediaPosition(),
                    q.minValue(), q.maxValue(), q.allowNegative(),
                    createdByUserId, lastEditedByUserId, createdAt, updatedAt,
                    tagIds, mediaCaption, altText, reactionsEnabled, version);
            case RankingQuestion q -> new RankingQuestion(
                    q.id(), q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), q.items(), q.correctOrder(), q.scoring(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.speakerNotes(), q.background(),
                    q.image(), q.videoUrl(), q.audioUrl(), q.videoAssetId(), q.audioAssetId(), q.mediaPosition(),
                    q.shuffleItemsForPresentation(),
                    createdByUserId, lastEditedByUserId, createdAt, updatedAt,
                    tagIds, mediaCaption, altText, reactionsEnabled, version);
            case ScalesQuestion q -> new ScalesQuestion(
                    q.id(), q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), q.statements(), q.scaleMin(), q.scaleMax(),
                    q.minLabel(), q.maxLabel(), q.correctRatings(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.speakerNotes(), q.background(),
                    q.image(), q.videoUrl(), q.audioUrl(), q.videoAssetId(), q.audioAssetId(), q.mediaPosition(),
                    createdByUserId, lastEditedByUserId, createdAt, updatedAt,
                    tagIds, mediaCaption, altText, reactionsEnabled, version);
            case QAndAQuestion q -> new QAndAQuestion(
                    q.id(), q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), q.maxSubmissionsPerPlayer(), q.allowVoting(), q.autoApprove(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.speakerNotes(), q.background(),
                    q.image(), q.videoUrl(), q.audioUrl(), q.videoAssetId(), q.audioAssetId(), q.mediaPosition(),
                    q.anonymousSubmissions(), q.minVotesToShow(),
                    createdByUserId, lastEditedByUserId, createdAt, updatedAt,
                    tagIds, mediaCaption, altText, reactionsEnabled, version);
            case GridQuestion q -> new GridQuestion(
                    q.id(), q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), q.rows(), q.cols(), q.cells(),
                    q.correctCellIndexes(), q.multipleCorrect(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.speakerNotes(), q.background(),
                    q.image(), q.videoUrl(), q.audioUrl(), q.videoAssetId(), q.audioAssetId(), q.mediaPosition(),
                    createdByUserId, lastEditedByUserId, createdAt, updatedAt,
                    tagIds, mediaCaption, altText, reactionsEnabled, version);
            case PlaceOnImageQuestion q -> new PlaceOnImageQuestion(
                    q.id(), q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), q.targetImage(),
                    q.correctX(), q.correctY(), q.tolerance(), q.scoring(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.speakerNotes(), q.background(),
                    q.image(), q.videoUrl(), q.audioUrl(), q.videoAssetId(), q.audioAssetId(), q.mediaPosition(),
                    createdByUserId, lastEditedByUserId, createdAt, updatedAt,
                    tagIds, mediaCaption, altText, reactionsEnabled, version);
            case WordCloudQuestion q -> new WordCloudQuestion(
                    q.id(), q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), q.maxSubmissionsPerPlayer(), q.maxWordLength(),
                    q.caseSensitive(), q.profanityFilter(), q.bannedWords(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.speakerNotes(), q.background(),
                    q.image(), q.videoUrl(), q.audioUrl(), q.videoAssetId(), q.audioAssetId(), q.mediaPosition(),
                    createdByUserId, lastEditedByUserId, createdAt, updatedAt,
                    tagIds, mediaCaption, altText, reactionsEnabled, version);
            case AllocationQuestion q -> new AllocationQuestion(
                    q.id(), q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), q.options(),
                    q.totalPointsToDistribute(), q.allowZeroOnItem(), q.enforceExactTotal(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.speakerNotes(), q.background(),
                    q.image(), q.videoUrl(), q.audioUrl(), q.videoAssetId(), q.audioAssetId(), q.mediaPosition(),
                    createdByUserId, lastEditedByUserId, createdAt, updatedAt,
                    tagIds, mediaCaption, altText, reactionsEnabled, version);
            case MatchingQuestion q -> new MatchingQuestion(
                    q.id(), q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), q.pairs(), q.scoring(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.speakerNotes(), q.background(),
                    q.image(), q.videoUrl(), q.audioUrl(), q.videoAssetId(), q.audioAssetId(), q.mediaPosition(),
                    createdByUserId, lastEditedByUserId, createdAt, updatedAt,
                    tagIds, mediaCaption, altText, reactionsEnabled, version);
            case DrawingQuestion q -> new DrawingQuestion(
                    q.id(), q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), q.backingImage(),
                    q.canvasWidth(), q.canvasHeight(),
                    q.maxStrokesPerPlayer(), q.maxPointsPerStroke(), q.palette(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.speakerNotes(), q.background(),
                    q.image(), q.videoUrl(), q.audioUrl(), q.videoAssetId(), q.audioAssetId(), q.mediaPosition(),
                    createdByUserId, lastEditedByUserId, createdAt, updatedAt,
                    tagIds, mediaCaption, altText, reactionsEnabled, version);
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
