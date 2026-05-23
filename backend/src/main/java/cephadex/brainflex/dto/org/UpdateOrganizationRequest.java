// PUT /api/organizations/{id} body. Owner-only partial update. Every field is
// nullable: null = "client did not send the field" → no-op. Empty string is
// interpreted as "clear this field" to give the settings UI a way to remove
// an existing value. allowPublicJoin is a primitive on the model but Boolean
// here so the null/false distinction round-trips.
package cephadex.brainflex.dto.org;

import jakarta.validation.constraints.Size;

public record UpdateOrganizationRequest(
        @Size(max = 200) String name,
        @Size(max = 1000) String description,
        @Size(max = 500) String websiteUrl,
        @Size(max = 200) String location,
        @Size(max = 253) String emailDomain,
        Boolean allowPublicJoin,
        String defaultThemeId) {
}
