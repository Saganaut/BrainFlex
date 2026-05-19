/**
 * How to score a MatchingQuestion answer.
 *   ALL_OR_NOTHING — full points iff every pair is matched correctly; zero otherwise
 *   PARTIAL        — points scaled by the fraction of pairs matched correctly
 */
package cephadex.brainflex.model.enums;

public enum MatchingScoring {
    ALL_OR_NOTHING,
    PARTIAL
}
