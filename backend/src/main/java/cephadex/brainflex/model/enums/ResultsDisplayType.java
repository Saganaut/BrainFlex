/**
 * How aggregated responses for an element are visualised when the host reveals
 * results.
 *   DEFAULT   — kind-specific built-in (e.g. MCQ option bars, scales heatmap).
 *   HISTOGRAM — bucketed bar chart of the response distribution.
 *   PIE_CHART — pie chart of the response distribution.
 */
package cephadex.brainflex.model.enums;

public enum ResultsDisplayType {
    DEFAULT,
    HISTOGRAM,
    PIE_CHART
}
