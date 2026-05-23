/**
 * Wire-safe projection of {@link cephadex.brainflex.model.org.OrganizationPlan}.
 *
 * Strips the Stripe billing identifiers ({@code stripeCustomerId},
 * {@code stripeSubscriptionId}) for the same reason {@link MembershipResponse} does:
 * they are opaque server-only references used by the Stripe integration and
 * have no client-side consumer.
 *
 * Everything else (seat limit, period, feature flags, quota window) flows to
 * the org admin UI so it can render the plan status badge and "seats N of M"
 * meter without a second round-trip.
 */
package cephadex.brainflex.dto;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import cephadex.brainflex.model.enums.MembershipStatus;
import cephadex.brainflex.model.enums.MembershipTier;
import cephadex.brainflex.model.org.OrganizationPlan;
import cephadex.brainflex.model.user.BillingState;

public record OrganizationPlanResponse(
        MembershipTier tier,
        MembershipStatus status,
        int seatLimit,
        Instant startedAt,
        Instant currentPeriodEnd,
        Boolean cancelAtPeriodEnd,
        Set<String> featureFlags,
        int monthlyInteractiveSessionLimit,
        Instant quotaResetsAt) {

    public static OrganizationPlanResponse from(OrganizationPlan p) {
        if (p == null) {
            return null;
        }
        BillingState b = p.getBilling() != null ? p.getBilling() : new BillingState();
        return new OrganizationPlanResponse(
                b.getTier(),
                b.getStatus(),
                p.getSeatLimit(),
                b.getStartedAt(),
                b.getCurrentPeriodEnd(),
                b.getCancelAtPeriodEnd(),
                p.getFeatureFlags() == null ? new LinkedHashSet<>() : new LinkedHashSet<>(p.getFeatureFlags()),
                p.getMonthlyInteractiveSessionLimit(),
                p.getQuotaResetsAt());
    }
}
