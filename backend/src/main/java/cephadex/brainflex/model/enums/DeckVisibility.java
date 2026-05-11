/**
 * Sharing scope for a Deck.
 *   PRIVATE  — only the creator
 *   UNLISTED — anyone with the link
 *   ORG      — visible to members of the creator's organization
 *   PUBLIC   — listed in the public template library
 */
package cephadex.brainflex.model.enums;

public enum DeckVisibility {
    PRIVATE,
    UNLISTED,
    ORG,
    PUBLIC
}
