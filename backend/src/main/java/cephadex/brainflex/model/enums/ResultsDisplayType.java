/**
 * How aggregated responses for an element are visualised when the host reveals
 * results.
 *   DEFAULT        — kind-specific built-in (e.g. MCQ option bars, scales heatmap).
 *   BAR_HORIZONTAL — horizontal bar chart of the response distribution.
 *   BAR_VERTICAL   — vertical bar chart of the response distribution.
 *   WORD_CLOUD     — word-cloud rendering (text-heavy kinds).
 *   PIE_CHART      — pie chart of the response distribution.
 *
 * HISTOGRAM is a deprecated synonym for BAR_VERTICAL kept on the enum so older
 * Mongo documents continue to deserialize (Spring Data Mongo uses its own
 * codec, not Jackson, so @JsonAlias alone is not enough). New writes from the
 * editor always use BAR_VERTICAL; the renderer treats HISTOGRAM identically.
 */
package cephadex.brainflex.model.enums;

public enum ResultsDisplayType {
    DEFAULT,
    BAR_HORIZONTAL,
    BAR_VERTICAL,
    WORD_CLOUD,
    PIE_CHART,
    /** @deprecated use {@link #BAR_VERTICAL}. Read-only for legacy documents. */
    @Deprecated
    HISTOGRAM
}
