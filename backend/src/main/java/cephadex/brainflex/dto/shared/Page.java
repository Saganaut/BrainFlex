/**
 * Generic page envelope returned by paginated read endpoints. Carries the
 * requested page of items plus enough pagination signal for clients to render
 * a "load more" affordance without a separate count request.
 *
 * {@code hasMore} is computed server-side so the client doesn't have to derive
 * it from {@code page} / {@code size} / {@code totalElements}. Replaces the
 * per-endpoint {@code *Page} / {@code *Response} envelopes that all carried
 * exactly this shape. Endpoints that need extra summary fields (e.g. the
 * ratings histogram) keep their own record.
 */
package cephadex.brainflex.dto.shared;

import java.util.List;

public record Page<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        boolean hasMore) {
}
