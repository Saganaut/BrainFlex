/**
 * One item in a RankingQuestion. Stable per-item id means the editor / runtime
 * can shuffle items for presentation without rewriting `correctOrder`.
 */
package cephadex.brainflex.model.element;

public record RankingItem(
        String id,
        String label,
        String imageUrl
) {
}
