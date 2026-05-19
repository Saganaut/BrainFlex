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

import cephadex.brainflex.model.answer.AllocationAnswer;
import cephadex.brainflex.model.answer.AnswerPayload;
import cephadex.brainflex.model.answer.GridAnswer;
import cephadex.brainflex.model.answer.MatchingAnswer;
import cephadex.brainflex.model.answer.McqAnswer;
import cephadex.brainflex.model.answer.NumberAnswer;
import cephadex.brainflex.model.answer.PlaceOnImageAnswer;
import cephadex.brainflex.model.answer.RankingAnswer;
import cephadex.brainflex.model.answer.ScalesAnswer;
import cephadex.brainflex.model.answer.TextAnswer;
import cephadex.brainflex.model.answer.TimeoutAnswer;
import cephadex.brainflex.model.element.AllocationQuestion;
import cephadex.brainflex.model.element.DeckElement;
import cephadex.brainflex.model.element.DrawingQuestion;
import cephadex.brainflex.model.element.GridQuestion;
import cephadex.brainflex.model.element.MatchingPair;
import cephadex.brainflex.model.element.MatchingQuestion;
import cephadex.brainflex.model.element.McqQuestion;
import cephadex.brainflex.model.element.NumberQuestion;
import cephadex.brainflex.model.element.PlaceOnImageQuestion;
import cephadex.brainflex.model.element.QAndAQuestion;
import cephadex.brainflex.model.element.RankingQuestion;
import cephadex.brainflex.model.element.ScalesQuestion;
import cephadex.brainflex.model.element.Slide;
import cephadex.brainflex.model.element.TextQuestion;
import cephadex.brainflex.model.element.WordCloudQuestion;
import cephadex.brainflex.model.enums.MatchingScoring;
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
            case TextQuestion q -> scoreText(q, payload);
            case NumberQuestion q -> scoreNumber(q, payload);
            case RankingQuestion q -> scoreRanking(q, payload);
            case ScalesQuestion q -> scoreScales(q, payload);
            case @SuppressWarnings("unused") QAndAQuestion ignored -> Result.ZERO;
            case GridQuestion q -> scoreGrid(q, payload);
            case PlaceOnImageQuestion q -> scorePlace(q, payload);
            case @SuppressWarnings("unused") WordCloudQuestion ignored -> Result.ZERO;
            case @SuppressWarnings("unused") AllocationQuestion ignored -> Result.ZERO;
            case MatchingQuestion q -> scoreMatching(q, payload);
            case @SuppressWarnings("unused") DrawingQuestion ignored -> Result.ZERO;
        };
    }

    private static Result scoreMcq(McqQuestion q, AnswerPayload payload) {
        if (!(payload instanceof McqAnswer a))
            return Result.ZERO;
        List<String> submitted = a.optionIds();
        if (submitted == null || submitted.isEmpty())
            return Result.ZERO;
        // Reject malformed payloads: single-select MCQs can't accept multi picks,
        // and multi-select MCQs honour the per-question maxSelections cap (0 = unlimited).
        if (!q.allowMultipleSelect() && submitted.size() > 1)
            return Result.ZERO;
        if (q.allowMultipleSelect() && q.maxSelections() > 0 && submitted.size() > q.maxSelections())
            return Result.ZERO;
        return scoreOptionPicks(submitted, q.correctOptionIds(), q.pointValue());
    }

    /**
     * MCQ scoring: when multiple correct ids exist the submitted set must equal
     * the correct set; otherwise (single-correct) any submitted id matching
     * counts. Empty correct set = unscored. Caller has already rejected
     * malformed submissions (multi-pick on a single-select question, over-cap).
     */
    private static Result scoreOptionPicks(List<String> submitted, List<String> correctIds, int points) {
        if (correctIds == null || correctIds.isEmpty())
            return Result.ZERO;
        if (correctIds.size() == 1) {
            boolean correct = submitted.contains(correctIds.get(0));
            return new Result(correct, correct ? points : 0);
        }
        boolean correct = submitted.size() == correctIds.size()
                && submitted.containsAll(correctIds);
        return new Result(correct, correct ? points : 0);
    }

    private static Result scoreText(TextQuestion q, AnswerPayload payload) {
        if (!(payload instanceof TextAnswer a) || a.text() == null)
            return Result.ZERO;
        String submitted = q.trimWhitespace() ? a.text().trim() : a.text();
        if (submitted.isEmpty())
            return Result.ZERO;

        if (matchesText(submitted, q.correctAnswer(), q)) {
            return new Result(true, q.pointValue());
        }
        List<String> variants = q.acceptedVariants();
        if (variants != null) {
            for (String variant : variants) {
                if (matchesText(submitted, variant, q)) {
                    return new Result(true, q.pointValue());
                }
            }
        }
        return Result.ZERO;
    }

    private static boolean matchesText(String submitted, String target, TextQuestion q) {
        if (target == null)
            return false;
        String t = q.trimWhitespace() ? target.trim() : target;
        String left = q.caseSensitive() ? submitted : submitted.toLowerCase(Locale.ROOT);
        String right = q.caseSensitive() ? t : t.toLowerCase(Locale.ROOT);
        if (left.equals(right))
            return true;
        if (q.fuzzyMatch() && q.fuzzyDistance() > 0)
            return levenshtein(left, right, q.fuzzyDistance()) <= q.fuzzyDistance();
        return false;
    }

    /**
     * Capped Levenshtein distance — returns `cap + 1` as soon as the running
     * minimum exceeds `cap`, so the caller pays linear work only for plausible
     * fuzzy matches. The cap shrinks the inner-loop band, keeping this O(n·cap)
     * instead of O(n·m) when cap is small (the only case we care about for
     * typo-tolerant text answers).
     */
    static int levenshtein(String left, String right, int cap) {
        int n = left.length();
        int m = right.length();
        if (Math.abs(n - m) > cap)
            return cap + 1;
        if (n == 0)
            return m;
        if (m == 0)
            return n;
        int[] prev = new int[m + 1];
        int[] curr = new int[m + 1];
        for (int j = 0; j <= m; j++) prev[j] = j;
        for (int i = 1; i <= n; i++) {
            curr[0] = i;
            int rowMin = curr[0];
            int from = Math.max(1, i - cap);
            int to = Math.min(m, i + cap);
            if (from > 1) curr[from - 1] = cap + 1;
            for (int j = from; j <= to; j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                int del = prev[j] + 1;
                int ins = curr[j - 1] + 1;
                int sub = prev[j - 1] + cost;
                int best = Math.min(del, Math.min(ins, sub));
                curr[j] = best;
                if (best < rowMin) rowMin = best;
            }
            if (to < m) curr[to + 1] = cap + 1;
            if (rowMin > cap)
                return cap + 1;
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[m];
    }

    private static Result scoreNumber(NumberQuestion q, AnswerPayload payload) {
        if (!(payload instanceof NumberAnswer a))
            return Result.ZERO;
        if (q.minValue() != null && a.value() < q.minValue())
            return Result.ZERO;
        if (q.maxValue() != null && a.value() > q.maxValue())
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

    private static Result scoreMatching(MatchingQuestion q, AnswerPayload payload) {
        if (!(payload instanceof MatchingAnswer a) || a.leftIdToRightId() == null)
            return Result.ZERO;
        List<MatchingPair> pairs = q.pairs();
        if (pairs == null || pairs.isEmpty())
            return Result.ZERO;
        int matched = 0;
        for (MatchingPair pair : pairs) {
            String submittedRight = a.leftIdToRightId().get(pair.id());
            if (submittedRight != null && submittedRight.equals(pair.id())) {
                matched++;
            }
        }
        boolean perfect = matched == pairs.size();
        if (q.scoring() == MatchingScoring.ALL_OR_NOTHING) {
            return new Result(perfect, perfect ? q.pointValue() : 0);
        }
        int points = (int) Math.round(q.pointValue() * ((double) matched / pairs.size()));
        return new Result(perfect, points);
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
