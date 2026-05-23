/**
 * Embedded subscription plan for an organization.
 *
 * Tracks the org-level billing state and the seat accounting that determines
 * how many members can occupy ORG_SEAT memberships at once. Seat consumption
 * itself lives on each User.membership; this record is the authoritative
 * source for the plan limit and renewal dates.
 *
 * The seven Stripe-shaped fields (tier/status/period/cancel/customer/subscription)
 * live on the embedded {@link BillingState} so the same value type is reused by
 * {@link Membership} and the Stripe webhook can write either scope through a
 * single shape. The remaining fields are org-only: seat math and the per-seat
 * quota mirrored onto seat-holders' memberships.
 */
package cephadex.brainflex.model.org;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import cephadex.brainflex.model.user.BillingState;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrganizationPlan {

    /**
     * Tier/status/period/Stripe-identifiers, shared shape with {@link Membership}.
     * Never null on a hydrated organization — defaults to a FREE billing state.
     */
    private BillingState billing = new BillingState();

    private int seatLimit = 0;

    /**
     * Org-level feature gating, mirrored on each seat-holder's {@link Membership#getFeatureFlags()}
     * at seat-assignment time. Lets the owner unlock a feature for the whole org without
     * touching every member document. Never null.
     */
    private Set<String> featureFlags = new LinkedHashSet<>();

    /**
     * Per-seat monthly session cap applied to every member when they consume an
     * {@code ORG_SEAT} membership. {@code 0} = unlimited. Mirrored onto each seat-holder's
     * {@link Membership#getMonthlyInteractiveSessionLimit()} at seat-assignment time so the
     * quota check stays single-document.
     */
    private int monthlyInteractiveSessionLimit = 0;

    /**
     * When the org's quota window resets (start of next calendar month UTC). Null until
     * the first seat-holder triggers a quota check. Surfaces in the org admin UI.
     */
    private Instant quotaResetsAt;
}
