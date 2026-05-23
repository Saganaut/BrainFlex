// PUT /api/media/{id} body. Null fields are left unchanged; empty
// organizationId clears the org-sharing scope.
package cephadex.brainflex.dto;

import java.util.List;

public record UpdateMediaAssetRequest(
        String name,
        List<String> tags,
        String organizationId,
        String altText,
        String attribution,
        String sourceUrl) {
}
