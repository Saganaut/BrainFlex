// Body of POST /api/tags. When `id` is null/blank the server slugifies
// `displayName`. Otherwise it must be kebab-case (validated server-side).
package cephadex.brainflex.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTagRequest(
        @Size(max = 64) String id,
        @NotBlank @Size(max = 80) String displayName,
        @Size(max = 64) String parentTagId,
        @Size(max = 500) String description,
        @Size(max = 500) String iconUrl,
        Boolean curated) {
}
