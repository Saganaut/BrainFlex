// PUT /api/themes/{id} body. Null fields are left unchanged; empty
// organizationId clears the org-sharing scope.
package cephadex.brainflex.dto;

import cephadex.brainflex.model.enums.ThemeMode;

public record UpdateThemeRequest(
                String name,
                Integer huePrimary,
                Integer hueAccent,
                ThemeMode mode,
                String organizationId) {
}
