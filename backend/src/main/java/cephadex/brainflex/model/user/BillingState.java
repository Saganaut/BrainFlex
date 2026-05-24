/**
 * Shared billing/subscription state embedded in both { Membership} (user-scoped)
 * and { OrganizationPlan} (org-scoped).
 *
 * Both surfaces are driven by the same Stripe webhook handler (tier change, status
 * change, period renewal, cancel-at-period-end), so the seven fields covered here
 * carry identical semantics on either side. Keeping the value object lets the
 * webhook write to a {@code BillingState regardless of whether the affected scope
 * is a user or an organization, and gives MembershipResponse / OrganizationPlanResponse a
 * single projection path.
 *
 * Stripe identifiers are stored as opaque strings so the integration can be slotted
 * in later without a schema change. They never leave the backend — wire DTOs strip
 * them — see {@link cephadex.brainflex.dto.org.MembershipResponse}.
 *
 * Fields intentionally NOT included here:
 *  - {@code seatLimit} (OrganizationPlan-only — seat math doesn't apply to individual memberships).
 *  - {@code sourceOrganizationId}, {@code monthlyInteractiveSessionCount},
 *    {@code monthlyCountPeriodStart}, {@code featureFlags}, {@code monthlyInteractiveSessionLimit},
 *    {@code quotaResetsAt} (gameplay/usage state that doesn't belong on the billing record —
 *    cf. issue 2h in z-docs/to-do/java-model-issues.md, which calls out moving usage counters
 *    off "Membership" entirely as a follow-up).
 */
package cephadex.brainflex.model.user;

import java.time.Instant;

import cephadex.brainflex.model.enums.MembershipStatus;
import cephadex.brainflex.model.enums.MembershipTier;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BillingState {
    // TODO: Revisit when in scope
    private MembershipTier tier = MembershipTier.FREE;
    private MembershipStatus status = MembershipStatus.NONE;

    /**
     * When the current paid period (or trial) began. Null while tier is FREE.
     */
    private Instant startedAt;

    /**
     * When the current paid period ends. Renewal extends this; cancellation
     * leaves it pointing at the access cutoff so features stay enabled through
     * the end of the paid period.
     */
    private Instant currentPeriodEnd;

    /**
     * If true, the subscription will not auto-renew after currentPeriodEnd.
     * Set when the subscriber cancels but the period has not yet lapsed.
     */
    private Boolean cancelAtPeriodEnd = false;

    /**
     * Opaque external billing references. Populated by the Stripe integration
     * once it lands; safe to leave null for now. Never leaves the backend —
     * see MembershipResponse / OrganizationPlanResponse for the wire projections.
     */
    private String stripeCustomerId;
    private String stripeSubscriptionId;
}
