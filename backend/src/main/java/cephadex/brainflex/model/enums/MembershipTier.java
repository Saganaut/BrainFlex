/**
 * Subscription tier a user (or organization) currently has.
 *
 * - FREE: default for new users; basic feature access only.
 * - INDIVIDUAL: paid individual subscription (single user).
 * - ORG_SEAT: this user occupies a seat on an organization plan; billing lives
 *   on the organization, not the user.
 *
 * For organizations the tier represents the plan SKU itself (e.g. ORG_TEAM).
 * Names are intentionally provider-agnostic — Stripe price/product IDs will be
 * mapped to these values at integration time.
 */
package cephadex.brainflex.model.enums;

public enum MembershipTier {
    FREE,
    INDIVIDUAL,
    ORG_SEAT,
    ORG_TEAM,
    ORG_BUSINESS
}
