/**
 * Utility for re-emitting a DeckElement with a different id or with a
 * substituted child list. Records are immutable so we need a polymorphic
 * clone per kind; this is the single place that knows the full constructor
 * of every element type.
 */
package cephadex.brainflex.service;

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
                q.image(), q.videoUrl(), q.audioUrl(), q.mediaPosition(),
                q.shuffleOptions(), q.allowMultipleSelect(), q.maxSelections());
    }

    public static DeckElement withId(DeckElement element, String id) {
        return switch (element) {
            case Slide s -> new Slide(
                    id, s.slideKind(), s.publicKey(), s.privateKey(),
                    s.title(), s.styledTitle(), s.body(),
                    s.scored(), s.survey(), s.multipleSelections(), s.responseMode(),
                    s.displaySeconds(), s.speakerNotes(), s.background(),
                    s.image(), s.videoUrl(), s.audioUrl(), s.mediaPosition(),
                    s.resultsDisplayType(), s.multipleSelectionsEnabled(),
                    s.selectionsPerParticipant(), s.showResultsAsPercentage(),
                    s.joinType(), s.showJoinInformation(), s.showResponses(),
                    s.heading(), s.participantInformation(),
                    s.autoAdvanceSeconds());
            case McqQuestion q -> new McqQuestion(
                    id, q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), q.options(), q.correctOptionIds(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.speakerNotes(), q.background(),
                    q.image(), q.videoUrl(), q.audioUrl(), q.mediaPosition(),
                    q.shuffleOptions(), q.allowMultipleSelect(), q.maxSelections());
            case TextQuestion q -> new TextQuestion(
                    id, q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), q.correctAnswer(), q.acceptedVariants(), q.caseSensitive(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.speakerNotes(), q.background(),
                    q.image(), q.videoUrl(), q.audioUrl(), q.mediaPosition(),
                    q.maxLength(), q.trimWhitespace(), q.fuzzyMatch(), q.fuzzyDistance());
            case NumberQuestion q -> new NumberQuestion(
                    id, q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), q.correctValue(), q.tolerance(), q.unitLabel(), q.decimalPlaces(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.speakerNotes(), q.background(),
                    q.image(), q.videoUrl(), q.audioUrl(), q.mediaPosition(),
                    q.minValue(), q.maxValue(), q.allowNegative());
            case RankingQuestion q -> new RankingQuestion(
                    id, q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), q.items(), q.correctOrder(), q.scoring(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.speakerNotes(), q.background(),
                    q.image(), q.videoUrl(), q.audioUrl(), q.mediaPosition(),
                    q.shuffleItemsForPresentation());
            case ScalesQuestion q -> new ScalesQuestion(
                    id, q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), q.statements(), q.scaleMin(), q.scaleMax(),
                    q.minLabel(), q.maxLabel(), q.correctRatings(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.speakerNotes(), q.background(),
                    q.image(), q.videoUrl(), q.audioUrl(), q.mediaPosition());
            case QAndAQuestion q -> new QAndAQuestion(
                    id, q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), q.maxSubmissionsPerPlayer(), q.allowVoting(), q.autoApprove(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.speakerNotes(), q.background(),
                    q.image(), q.videoUrl(), q.audioUrl(), q.mediaPosition(),
                    q.anonymousSubmissions(), q.minVotesToShow());
            case GridQuestion q -> new GridQuestion(
                    id, q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), q.rows(), q.cols(), q.cells(),
                    q.correctCellIndexes(), q.multipleCorrect(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.speakerNotes(), q.background(),
                    q.image(), q.videoUrl(), q.audioUrl(), q.mediaPosition());
            case PlaceOnImageQuestion q -> new PlaceOnImageQuestion(
                    id, q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), q.targetImage(),
                    q.correctX(), q.correctY(), q.tolerance(), q.scoring(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.speakerNotes(), q.background(),
                    q.image(), q.videoUrl(), q.audioUrl(), q.mediaPosition());
            case WordCloudQuestion q -> new WordCloudQuestion(
                    id, q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), q.maxSubmissionsPerPlayer(), q.maxWordLength(),
                    q.caseSensitive(), q.profanityFilter(), q.bannedWords(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.speakerNotes(), q.background(),
                    q.image(), q.videoUrl(), q.audioUrl(), q.mediaPosition());
            case AllocationQuestion q -> new AllocationQuestion(
                    id, q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), q.options(),
                    q.totalPointsToDistribute(), q.allowZeroOnItem(), q.enforceExactTotal(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.speakerNotes(), q.background(),
                    q.image(), q.videoUrl(), q.audioUrl(), q.mediaPosition());
            case MatchingQuestion q -> new MatchingQuestion(
                    id, q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), regenPairIds(q.pairs()), q.scoring(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.speakerNotes(), q.background(),
                    q.image(), q.videoUrl(), q.audioUrl(), q.mediaPosition());
            case DrawingQuestion q -> new DrawingQuestion(
                    id, q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), q.backingImage(),
                    q.canvasWidth(), q.canvasHeight(),
                    q.maxStrokesPerPlayer(), q.maxPointsPerStroke(), q.palette(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.speakerNotes(), q.background(),
                    q.image(), q.videoUrl(), q.audioUrl(), q.mediaPosition());
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
