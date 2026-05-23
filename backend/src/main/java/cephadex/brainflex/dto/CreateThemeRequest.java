// POST /api/themes body. organizationId widens visibility to an org the caller
// belongs to; null/blank keeps the theme personal.
package cephadex.brainflex.dto;

import cephadex.brainflex.model.enums.ThemeMode;

public record CreateThemeRequest(
        String name,
        int huePrimary,
        int hueAccent,
        ThemeMode mode,
        String organizationId) {
}
