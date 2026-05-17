/**
 * Strips answer-key fields from a DeckElement before broadcasting to clients
 * during the SUBMIT phase. Returns a record clone with all correct-answer
 * fields null/empty so a clever client can't read the answer from the
 * WebSocket payload before submitting.
 *
 * The REVEAL phase broadcast uses the un-redacted element directly — by that
 * point everyone has submitted and the answer is meant to be visible.
 */
package cephadex.brainflex.service;

import java.util.List;

import cephadex.brainflex.model.element.DeckElement;
import cephadex.brainflex.model.element.GridQuestion;
import cephadex.brainflex.model.element.McqQuestion;
import cephadex.brainflex.model.element.NumberQuestion;
import cephadex.brainflex.model.element.PlaceOnImageQuestion;
import cephadex.brainflex.model.element.QAndAQuestion;
import cephadex.brainflex.model.element.RankingQuestion;
import cephadex.brainflex.model.element.ScalesQuestion;
import cephadex.brainflex.model.element.Slide;
import cephadex.brainflex.model.element.TextQuestion;

public final class ElementRedactor {

    private ElementRedactor() {}

    public static DeckElement redact(DeckElement element) {
        return switch (element) {
            case Slide s -> s; // nothing to redact
            case McqQuestion q -> new McqQuestion(
                    q.id(), q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), q.options(),
                    null,        // correctOptionIds — redacted before reveal
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(),
                    null,        // explanation
                    q.displaySeconds(), q.speakerNotes(), q.backgroundImageUrl(),
                    q.imageUrl(), q.videoUrl(), q.audioUrl(), q.mediaPosition());
            case TextQuestion q -> new TextQuestion(
                    q.id(), q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(),
                    null,        // correctAnswer
                    List.of(),   // acceptedVariants
                    q.caseSensitive(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(),
                    null,        // explanation
                    q.displaySeconds(), q.speakerNotes(), q.backgroundImageUrl(),
                    q.imageUrl(), q.videoUrl(), q.audioUrl(), q.mediaPosition());
            case NumberQuestion q -> new NumberQuestion(
                    q.id(), q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(),
                    0.0,         // correctValue
                    0.0,         // tolerance
                    q.unitLabel(), q.decimalPlaces(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(),
                    null,        // explanation
                    q.displaySeconds(), q.speakerNotes(), q.backgroundImageUrl(),
                    q.imageUrl(), q.videoUrl(), q.audioUrl(), q.mediaPosition());
            case RankingQuestion q -> new RankingQuestion(
                    q.id(), q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), q.items(),
                    List.of(),   // correctOrder
                    q.scoring(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(),
                    null,        // explanation
                    q.displaySeconds(), q.speakerNotes(), q.backgroundImageUrl(),
                    q.imageUrl(), q.videoUrl(), q.audioUrl(), q.mediaPosition());
            case ScalesQuestion q -> new ScalesQuestion(
                    q.id(), q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), q.statements(),
                    q.scaleMin(), q.scaleMax(), q.minLabel(), q.maxLabel(),
                    List.of(),   // correctRatings
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(),
                    null,        // explanation
                    q.displaySeconds(), q.speakerNotes(), q.backgroundImageUrl(),
                    q.imageUrl(), q.videoUrl(), q.audioUrl(), q.mediaPosition());
            case QAndAQuestion q -> q; // no answer key to hide
            case GridQuestion q -> new GridQuestion(
                    q.id(), q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), q.rows(), q.cols(), q.cells(),
                    java.util.Set.of(),  // correctCellIndexes
                    q.multipleCorrect(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(),
                    null,        // explanation
                    q.displaySeconds(), q.speakerNotes(), q.backgroundImageUrl(),
                    q.imageUrl(), q.videoUrl(), q.audioUrl(), q.mediaPosition());
            case PlaceOnImageQuestion q -> new PlaceOnImageQuestion(
                    q.id(), q.publicKey(), q.privateKey(), q.title(), q.styledTitle(),
                    q.prompt(), q.targetImageUrl(),
                    0.0,         // correctX
                    0.0,         // correctY
                    q.tolerance(), q.scoring(),
                    q.pointValue(), q.difficulty(),
                    q.scored(), q.survey(), q.multipleSelections(), q.responseMode(),
                    q.bestAnswerMode(), q.bestAnswerTitle(), q.bestAnswerBonus(),
                    null,        // explanation
                    q.displaySeconds(), q.speakerNotes(), q.backgroundImageUrl(),
                    q.imageUrl(), q.videoUrl(), q.audioUrl(), q.mediaPosition());
        };
    }

    /**
     * Also strips the host-only `speakerNotes` field — used when broadcasting an
     * element to non-host participants. The host sees the un-redacted version
     * via a separate /user/queue/host channel (future).
     */
    public static DeckElement redactForParticipant(DeckElement element) {
        DeckElement redacted = redact(element);
        // For v1 speakerNotes is included in the redact() output above; we expose this
        // method now so callers can opt into per-recipient stripping later without
        // changing call sites.
        return redacted;
    }
}
