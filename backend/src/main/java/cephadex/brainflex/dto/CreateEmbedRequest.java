// POST /api/media/embed body. URL is normalised to an iframe-ready form on the
// backend.
package cephadex.brainflex.dto;

import java.util.List;

public record CreateEmbedRequest(
        String url,
        String name,
        List<String> tags,
        String organizationId) {
}
