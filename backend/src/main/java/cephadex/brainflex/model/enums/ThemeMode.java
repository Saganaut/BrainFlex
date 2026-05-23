/**
 * Theme appearance preference selected by the owner of a {@link cephadex.brainflex.model.theme.Theme}.
 * Replaces the prior free-form {@code String mode} field ("light" | "dark" | "system") with a
 * typo-safe enum. Wire and persistence form is the enum {@code name()} (LIGHT / DARK / SYSTEM) —
 * see z-docs/to-do/migrations-needed.md for the one-shot lowercase → uppercase backfill.
 */
package cephadex.brainflex.model.enums;

public enum ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}
