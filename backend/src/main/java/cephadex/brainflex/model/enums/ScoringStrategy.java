/**
 * Unified scoring-strategy enum that replaces the four per-kind enums removed
 * in the §4 sweep of {@code z-docs/to-do/dto-consolidation-quick-wins.md}:
 * {@code RankingScoring}, {@code MatchingScoring}, {@code PlaceScoring}, and
 * {@code BestAnswerScoring}. One enum keeps the strategies in a single place
 * and collapses the four parallel TS string-literal unions the frontend
 * codegen used to emit into one.
 *
 * Each question type — and the best-answer chrome — declares the subset of
 * values it accepts via the static {@code ALLOWED_FOR_*} EnumSets below. The
 * canonical constructor on each question record enforces the subset so an
 * invalid combination (e.g. RankingQuestion with FLAT_WINNER) fails fast at
 * construction rather than confusing the runtime scorer.
 *
 * Wire format: each value's name() is the same string that was previously
 * persisted by the per-kind enums (POINTS_PER_VOTE, FLAT_WINNER, EXACT,
 * PARTIAL, BINARY, LINEAR, ALL_OR_NOTHING), so existing documents deserialize
 * without a data migration — see z-docs/to-do/migrations-needed.md for the
 * verification checklist.
 */
package cephadex.brainflex.model.enums;

import java.util.EnumSet;
import java.util.Set;

public enum ScoringStrategy {
    /** Full points iff every position/pair matches exactly; zero otherwise. */
    EXACT,
    /** Pro-rated points by the fraction of positions/pairs that match. */
    PARTIAL,
    /** Full points within tolerance, zero outside. */
    BINARY,
    /** Points scale linearly from full (at zero distance) to zero (at tolerance edge). */
    LINEAR,
    /** Binary score for compound answers: every pair correct → full points, else zero. */
    ALL_OR_NOTHING,
    /** Best-answer phase: each player earns {@code votesReceived * bestAnswerPoints}. */
    POINTS_PER_VOTE,
    /** Best-answer phase: the top-voted submission(s) each earn {@code bestAnswerPoints}. */
    FLAT_WINNER;

    public static final Set<ScoringStrategy> ALLOWED_FOR_RANKING =
            EnumSet.of(EXACT, PARTIAL);

    public static final Set<ScoringStrategy> ALLOWED_FOR_MATCHING =
            EnumSet.of(ALL_OR_NOTHING, PARTIAL);

    public static final Set<ScoringStrategy> ALLOWED_FOR_PLACE =
            EnumSet.of(BINARY, LINEAR);

    public static final Set<ScoringStrategy> ALLOWED_FOR_BEST_ANSWER =
            EnumSet.of(POINTS_PER_VOTE, FLAT_WINNER);
}
