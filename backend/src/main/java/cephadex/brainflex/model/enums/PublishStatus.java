/**
 * Lifecycle state of a deck independent of its visibility scope.
 *
 * - DRAFT: author is still building it; never surfaced in Explore.
 * - PUBLISHED: author has shipped it. Combined with {@code DeckVisibility=PUBLIC}
 *   this is what shows up in /api/decks/explore.
 * - ARCHIVED: hidden from Explore and the owner's primary deck list but not
 *   destroyed — past InteractiveSessions, ratings, and play history still resolve.
 */
package cephadex.brainflex.model.enums;

public enum PublishStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED
}
