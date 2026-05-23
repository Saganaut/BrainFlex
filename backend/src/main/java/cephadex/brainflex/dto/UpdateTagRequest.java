// Body of PUT /api/tags/{id}.
package cephadex.brainflex.dto;

import jakarta.validation.constraints.Size;

public record UpdateTagRequest(
                @Size(max = 80) String displayName,
                @Size(max = 64) String parentTagId,
                @Size(max = 500) String description,
                @Size(max = 500) String iconUrl,
                Boolean curated) {
}
