/**
 * How to score a RankingQuestion answer.
 *   EXACT   — full points iff every position matches; zero otherwise
 *   PARTIAL — points scaled by the fraction of items in their correct position
 */
package cephadex.brainflex.model.enums;

public enum RankingScoring {
    EXACT,
    PARTIAL
}
