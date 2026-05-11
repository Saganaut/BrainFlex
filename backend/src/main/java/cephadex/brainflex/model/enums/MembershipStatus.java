/**
 * Lifecycle state of a paid membership.
 *
 * Mirrors the common subscription states a billing provider exposes so that we
 * can drive UI gating from a single field without re-deriving from dates. The
 * Stripe integration will write directly into this field when webhook events
 * arrive (subscription.created, .updated, .deleted, invoice.payment_failed).
 */
package cephadex.brainflex.model.enums;

public enum MembershipStatus {
    ACTIVE,
    TRIALING,
    PAST_DUE,
    CANCELED,
    EXPIRED,
    NONE
}
