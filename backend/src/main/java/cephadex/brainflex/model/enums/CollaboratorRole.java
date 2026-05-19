/**
 * Role a user holds on a deck via the {@code deck_collaborators} join collection.
 *
 * - OWNER: full edit + can manage collaborators + can transfer ownership. Exactly
 *   one OWNER row per deck — the service layer enforces uniqueness.
 * - EDITOR: full edit on the deck and its elements, but cannot manage collaborators.
 * - VIEWER: read-only access, only relevant for non-public decks (PRIVATE / ORG).
 */
package cephadex.brainflex.model.enums;

public enum CollaboratorRole {
    VIEWER,
    EDITOR,
    OWNER
}
