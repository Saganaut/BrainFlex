/**
 * Embedded membership/subscription record for a user.
 *
 * Captures the billing state for both individual subscribers and users
 * occupying a seat on an organization plan. When {@code billing.tier} is
 * ORG_SEAT the seat is granted by the organization referenced in
 * {@link #sourceOrganizationId}; otherwise sourceOrganizationId is null and
 * billing belongs to the user directly.
 *
 * The seven Stripe-shaped fields (tier/status/period/cancel/customer/subscription)
 * live on the embedded {@link BillingState} so the same value type is reused by
 * {@link OrganizationPlan} and the Stripe webhook can write either scope through a
 * single shape. The rest of this record is user-side gameplay/usage state
 * (counters, quota, sourceOrganizationId, feature flags) that has no analog on
 * an org plan.
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
public class Membership {

    /**
     * Tier/status/period/Stripe-identifiers, shared shape with {@link OrganizationPlan}.
     * Never null on a hydrated user — defaults to a FREE billing state.
     */
    private BillingState billing = new BillingState();

    /**
     * Organization that granted this user's seat. Only populated when
     * {@code billing.tier} is ORG_SEAT — billing/plan state lives on the
     * organization in that case.
     */
    private String sourceOrganizationId;

    /**
     * Number of interactive sessions this user has hosted in the current
     * billing period. Bumped by {@link cephadex.brainflex.service.GameHistoryService}
     * when a session finishes; consulted by quota gates when tiered limits
     * are introduced. Rolls over to 1 on the first finish of a new calendar
     * month — see {@link #monthlyCountPeriodStart}.
     */
    private int monthlyInteractiveSessionCount = 0;

    /**
     * Start-of-month timestamp for the current counter window. Null until the
     * first session finish writes a value. The roll-over check compares
     * year+month against the incoming session's {@code playedAt}; we never
     * back-fill missed months.
     */
    private Instant monthlyCountPeriodStart;

    /**
     * Free-form capability flags resolved at request time. Drives feature gating like
     * {@code "reactions"}, {@code "team-mode"}, {@code "analytics-pro"},
     * {@code "ai-generation"}. Defaults empty — the membership tier still grants the
     * baseline feature set; this set is purely additive overrides. Never null.
     */
    private Set<String> featureFlags = new LinkedHashSet<>();

    /**
     * Soft monthly quota on hosted interactive sessions. {@code 0} means unlimited
     * (paid tiers); a positive value is the cap consulted by
     * {@code MembershipService.canStartInteractiveSession}. Stored on the user even when
     * the seat comes from an org plan so the gate can fast-path without re-resolving.
     */
    private int monthlyInteractiveSessionLimit = 0;

    /**
     * When the current month's quota window ends (start of next calendar month UTC).
     * Distinct from {@link #monthlyCountPeriodStart}: that field marks the boundary the
     * counter rolls forward from, while this one is the boundary the UI surfaces in
     * "resets in 7 days" copy. Null until the first quota check runs.
     */
    private Instant quotaResetsAt;
}
