/**
 * Scores a player's submission against a deck element. Dispatches on the pair
 * (element kind, payload kind); a mismatch (e.g. NumberAnswer for an MCQ) is
 * treated as a no-credit submission rather than an exception so the round can
 * still complete cleanly.
 *
 * Returns a `Result(correct, points)` per element + payload combo. Speed
 * bonuses are layered on top by ShowcaseService — this class only computes
 * the base score implied by the question's `pointValue`.
 */
package cephadex.brainflex.service;

import java.util.List;
import java.util.Locale;

import cephadex.brainflex.model.answer.AnswerPayload;
import cephadex.brainflex.model.answer.GridAnswer;
import cephadex.brainflex.model.answer.ImageChoiceAnswer;
import cephadex.brainflex.model.answer.McqAnswer;
import cephadex.brainflex.model.answer.NumberAnswer;
import cephadex.brainflex.model.answer.PlaceOnImageAnswer;
import cephadex.brainflex.model.answer.RankingAnswer;
import cephadex.brainflex.model.answer.ScalesAnswer;
import cephadex.brainflex.model.answer.TextAnswer;
import cephadex.brainflex.model.answer.TimeoutAnswer;
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
import cephadex.brainflex.model.enums.PlaceScoring;
import cephadex.brainflex.model.enums.RankingScoring;

public final class ElementScorer {

    public record Result(boolean correct, int points) {
        public static final Result ZERO = new Result(false, 0);
    }

    private ElementScorer() {
    }

    /**
     * Compute the base score for a submission. Returns ZERO for slides
     * (non-interactive), timeouts, mismatched payload kinds, and unscored
     * question types (Q&A, Scales when scored=false).
     */
    public static Result score(DeckElement element, AnswerPayload payload) {
        if (payload instanceof TimeoutAnswer)
            return Result.ZERO;

        return switch (element) {
            case @SuppressWarnings("unused") Slide ignored -> Result.ZERO;
            case McqQuestion q -> scoreMcq(q, payload);
            case ImageChoiceQuestion q -> scoreImageChoice(q, payload);
            case TextQuestion q -> scoreText(q, payload);
            case NumberQuestion q -> scoreNumber(q, payload);
            case RankingQuestion q -> scoreRanking(q, payload);
            case ScalesQuestion q -> scoreScales(q, payload);
            case @SuppressWarnings("unused") QAndAQuestion ignored -> Result.ZERO;
            case GridQuestion q -> scoreGrid(q, payload);
            case PlaceOnImageQuestion q -> scorePlace(q, payload);
        };
    }

    private static Result scoreMcq(McqQuestion q, AnswerPayload payload) {
        if (!(payload instanceof McqAnswer a))
            return Result.ZERO;
        List<String> correctIds = q.correctOptionIds();
        boolean correct = a.optionId() != null
                && correctIds != null
                && correctIds.contains(a.optionId());
        return new Result(correct, correct ? q.pointValue() : 0);
    }

    private static Result scoreImageChoice(ImageChoiceQuestion q, AnswerPayload payload) {
        if (!(payload instanceof ImageChoiceAnswer a))
            return Result.ZERO;
        boolean correct = a.optionId() != null && a.optionId().equals(q.correctOptionId());
        return new Result(correct, correct ? q.pointValue() : 0);
    }

    private static Result scoreText(TextQuestion q, AnswerPayload payload) {
        if (!(payload instanceof TextAnswer a) || a.text() == null)
            return Result.ZERO;
        String submitted = a.text().trim();
        if (submitted.isEmpty())
            return Result.ZERO;

        if (matchesText(submitted, q.correctAnswer(), q.caseSensitive())) {
            return new Result(true, q.pointValue());
        }
        List<String> variants = q.acceptedVariants();
        if (variants != null) {
            for (String variant : variants) {
                if (matchesText(submitted, variant, q.caseSensitive())) {
                    return new Result(true, q.pointValue());
                }
            }
        }
        return Result.ZERO;
    }

    private static boolean matchesText(String submitted, String target, boolean caseSensitive) {
        if (target == null)
            return false;
        String t = target.trim();
        return caseSensitive ? submitted.equals(t)
                : submitted.toLowerCase(Locale.ROOT).equals(t.toLowerCase(Locale.ROOT));
    }

    private static Result scoreNumber(NumberQuestion q, AnswerPayload payload) {
        if (!(payload instanceof NumberAnswer a))
            return Result.ZERO;
        boolean correct = Math.abs(a.value() - q.correctValue()) <= q.tolerance();
        return new Result(correct, correct ? q.pointValue() : 0);
    }

    private static Result scoreRanking(RankingQuestion q, AnswerPayload payload) {
        if (!(payload instanceof RankingAnswer a) || a.orderedItemIds() == null)
            return Result.ZERO;
        List<String> submitted = a.orderedItemIds();
        List<String> correct = q.correctOrder();
        if (correct == null || correct.isEmpty())
            return Result.ZERO;

        int matches = 0;
        int compareLen = Math.min(submitted.size(), correct.size());
        for (int i = 0; i < compareLen; i++) {
            if (submitted.get(i).equals(correct.get(i)))
                matches++;
        }
        boolean perfect = matches == correct.size() && submitted.size() == correct.size();
        if (q.scoring() == RankingScoring.EXACT) {
            return new Result(perfect, perfect ? q.pointValue() : 0);
        }
        // PARTIAL: pro-rated points; "correct" iff at least half right.
        int points = (int) Math.round(q.pointValue() * ((double) matches / correct.size()));
        return new Result(matches * 2 >= correct.size(), points);
    }

    private static Result scoreScales(ScalesQuestion q, AnswerPayload payload) {
        if (!q.scored())
            return Result.ZERO;
        if (!(payload instanceof ScalesAnswer a) || a.ratings() == null)
            return Result.ZERO;
        List<Integer> correct = q.correctRatings();
        if (correct == null || correct.size() != q.statements().size())
            return Result.ZERO;

        int matches = 0;
        for (int i = 0; i < q.statements().size(); i++) {
            String sid = q.statements().get(i).id();
            Integer submitted = a.ratings().get(sid);
            if (submitted != null && submitted.equals(correct.get(i)))
                matches++;
        }
        int points = (int) Math.round(q.pointValue() * ((double) matches / q.statements().size()));
        return new Result(matches == q.statements().size(), points);
    }

    private static Result scoreGrid(GridQuestion q, AnswerPayload payload) {
        if (!(payload instanceof GridAnswer a) || a.selectedCellIndexes() == null)
            return Result.ZERO;
        if (q.multipleCorrect()) {
            boolean correct = a.selectedCellIndexes().equals(q.correctCellIndexes());
            return new Result(correct, correct ? q.pointValue() : 0);
        }
        // single-pick: any one of the selected cells must be in the correct set
        boolean correct = a.selectedCellIndexes().stream()
                .anyMatch(q.correctCellIndexes()::contains);
        return new Result(correct, correct ? q.pointValue() : 0);
    }

    private static Result scorePlace(PlaceOnImageQuestion q, AnswerPayload payload) {
        if (!(payload instanceof PlaceOnImageAnswer a))
            return Result.ZERO;
        double dx = a.x() - q.correctX();
        double dy = a.y() - q.correctY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance > q.tolerance())
            return Result.ZERO;
        if (q.scoring() == PlaceScoring.BINARY) {
            return new Result(true, q.pointValue());
        }
        // LINEAR: full at zero distance, zero at the tolerance edge.
        double share = 1.0 - (distance / q.tolerance());
        return new Result(true, (int) Math.round(q.pointValue() * share));
    }
}
