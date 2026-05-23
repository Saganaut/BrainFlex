/**
 * Wire-safe projection of {@link cephadex.brainflex.model.org.Membership}.
 *
 * Strips fields that should never leave the backend:
 *  - {@code stripeCustomerId} / {@code stripeSubscriptionId} — opaque billing
 *    identifiers used by the (forthcoming) Stripe webhook handler. They have
 *    no client-side use and leaking them into any non-/me read surface (admin
 *    list, public profile, etc.) is a security regression.
 *  - {@code monthlyCountPeriodStart} — the internal rollover boundary that the
 *    counter uses to detect new calendar months. Distinct from
 *    {@code quotaResetsAt}, which is the UI-facing "resets in N days" boundary
 *    and is kept on the wire.
 *
 * Everything else round-trips to the client: tier/status/period for the
 * billing badge, the usage counter + quota for the "X / Y sessions this month"
 * meter, {@code sourceOrganizationId} so the UI can label a seat as "via Acme
 * Corp", and the feature flag set for client-side capability gating.
 */
package cephadex.brainflex.dto.org;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import cephadex.brainflex.model.user.BillingState;
import cephadex.brainflex.model.org.Membership;
import cephadex.brainflex.model.enums.MembershipStatus;
import cephadex.brainflex.model.enums.MembershipTier;

public record MembershipResponse(
        MembershipTier tier,
        MembershipStatus status,
        Instant startedAt,
        Instant currentPeriodEnd,
        Boolean cancelAtPeriodEnd,
        String sourceOrganizationId,
        int monthlyInteractiveSessionCount,
        Set<String> featureFlags,
        int monthlyInteractiveSessionLimit,
        Instant quotaResetsAt) {

    public static MembershipResponse from(Membership m) {
        if (m == null) {
            return null;
        }
        BillingState b = m.getBilling() != null ? m.getBilling() : new BillingState();
        return new MembershipResponse(
                b.getTier(),
                b.getStatus(),
                b.getStartedAt(),
                b.getCurrentPeriodEnd(),
                b.getCancelAtPeriodEnd(),
                m.getSourceOrganizationId(),
                m.getMonthlyInteractiveSessionCount(),
                m.getFeatureFlags() == null ? new LinkedHashSet<>() : new LinkedHashSet<>(m.getFeatureFlags()),
                m.getMonthlyInteractiveSessionLimit(),
                m.getQuotaResetsAt());
    }
}
