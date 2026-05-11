/**
 * Utility for re-emitting a DeckElement with a different id. Records are
 * immutable so we need a polymorphic clone per kind; this is the single
 * place that knows the full constructor of every element type.
 */
package cephadex.brainflex.service;

import cephadex.brainflex.model.element.DeckElement;
import cephadex.brainflex.model.element.GridQuestion;
import cephadex.brainflex.model.element.ImageChoiceQuestion;
import cephadex.brainflex.model.element.McqQuestion;
import cephadex.brainflex.model.element.NumberQuestion;
import cephadex.brainflex.model.element.PlaceOnImageQuestion;
import cephadex.brainflex.model.element.QAndAQuestion;
import cephadex.brainflex.model.element.RankingQuestion;
import cephadex.brainflex.model.element.ScalesQuestion;
import cephadex.brainflex.model.element.Slide;
import cephadex.brainflex.model.element.TextQuestion;

public final class DeckElementCloner {

    private DeckElementCloner() {}

    public static DeckElement withId(DeckElement element, String id) {
        return switch (element) {
            case Slide s -> new Slide(
                    id, s.slideKind(), s.title(), s.body(),
                    s.displaySeconds(), s.hostNotes(), s.backgroundImageUrl(),
                    s.imageUrl(), s.videoUrl(), s.audioUrl(), s.mediaPosition());
            case McqQuestion q -> new McqQuestion(
                    id, q.prompt(), q.options(), q.correctOptionId(),
                    q.pointValue(), q.difficulty(),
                    q.bestAnswerMode(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.hostNotes(), q.backgroundImageUrl(),
                    q.imageUrl(), q.videoUrl(), q.audioUrl(), q.mediaPosition());
            case ImageChoiceQuestion q -> new ImageChoiceQuestion(
                    id, q.prompt(), q.options(), q.correctOptionId(),
                    q.pointValue(), q.difficulty(),
                    q.bestAnswerMode(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.hostNotes(), q.backgroundImageUrl(),
                    q.imageUrl(), q.videoUrl(), q.audioUrl(), q.mediaPosition());
            case TextQuestion q -> new TextQuestion(
                    id, q.prompt(), q.correctAnswer(), q.acceptedVariants(), q.caseSensitive(),
                    q.pointValue(), q.difficulty(),
                    q.bestAnswerMode(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.hostNotes(), q.backgroundImageUrl(),
                    q.imageUrl(), q.videoUrl(), q.audioUrl(), q.mediaPosition());
            case NumberQuestion q -> new NumberQuestion(
                    id, q.prompt(), q.correctValue(), q.tolerance(), q.unitLabel(), q.decimalPlaces(),
                    q.pointValue(), q.difficulty(),
                    q.bestAnswerMode(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.hostNotes(), q.backgroundImageUrl(),
                    q.imageUrl(), q.videoUrl(), q.audioUrl(), q.mediaPosition());
            case RankingQuestion q -> new RankingQuestion(
                    id, q.prompt(), q.items(), q.correctOrder(), q.scoring(),
                    q.pointValue(), q.difficulty(),
                    q.bestAnswerMode(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.hostNotes(), q.backgroundImageUrl(),
                    q.imageUrl(), q.videoUrl(), q.audioUrl(), q.mediaPosition());
            case ScalesQuestion q -> new ScalesQuestion(
                    id, q.prompt(), q.statements(), q.scaleMin(), q.scaleMax(),
                    q.minLabel(), q.maxLabel(), q.scored(), q.correctRatings(),
                    q.pointValue(), q.difficulty(),
                    q.bestAnswerMode(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.hostNotes(), q.backgroundImageUrl(),
                    q.imageUrl(), q.videoUrl(), q.audioUrl(), q.mediaPosition());
            case QAndAQuestion q -> new QAndAQuestion(
                    id, q.prompt(), q.maxSubmissionsPerPlayer(), q.allowVoting(), q.autoApprove(),
                    q.pointValue(), q.difficulty(),
                    q.bestAnswerMode(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.hostNotes(), q.backgroundImageUrl(),
                    q.imageUrl(), q.videoUrl(), q.audioUrl(), q.mediaPosition());
            case GridQuestion q -> new GridQuestion(
                    id, q.prompt(), q.rows(), q.cols(), q.cells(),
                    q.correctCellIndexes(), q.multipleCorrect(),
                    q.pointValue(), q.difficulty(),
                    q.bestAnswerMode(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.hostNotes(), q.backgroundImageUrl(),
                    q.imageUrl(), q.videoUrl(), q.audioUrl(), q.mediaPosition());
            case PlaceOnImageQuestion q -> new PlaceOnImageQuestion(
                    id, q.prompt(), q.targetImageUrl(),
                    q.correctX(), q.correctY(), q.tolerance(), q.scoring(),
                    q.pointValue(), q.difficulty(),
                    q.bestAnswerMode(), q.bestAnswerBonus(), q.explanation(),
                    q.displaySeconds(), q.hostNotes(), q.backgroundImageUrl(),
                    q.imageUrl(), q.videoUrl(), q.audioUrl(), q.mediaPosition());
        };
    }
}
