// PUT /api/gallery/{id} body. Null fields are left unchanged; empty
// organizationId clears the org-sharing scope.
package cephadex.brainflex.dto.media;

import java.util.List;

public record UpdateGalleryImageRequest(
        String name,
        List<String> tags,
        String organizationId) {
}
