/**
 * Reuse / remix permissions an author grants on a published deck.
 *
 * Defaults to {@code ALL_RIGHTS_RESERVED} so nothing leaks into the wider
 * Creative Commons pool without an explicit opt-in. Surfaced on the deck
 * card so remixers can see at a glance whether forking is allowed.
 */
package cephadex.brainflex.model.enums;

public enum License {
    ALL_RIGHTS_RESERVED,
    CC_BY,
    CC_BY_SA,
    CC_BY_NC,
    CC0
}
