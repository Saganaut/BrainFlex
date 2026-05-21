/**
 * Embedded membership/subscription record for a user.
 *
 * Captures the billing state for both individual subscribers and users
 * occupying a seat on an organization plan. When tier is ORG_SEAT the seat is
 * granted by the organization referenced in sourceOrganizationId; otherwise
 * sourceOrganizationId is null and billing belongs to the user directly.
 *
 * Stripe identifiers (customer/subscription) are stored as opaque strings so
 * the integration can be added later without a schema change.
 */
package cephadex.brainflex.model;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import cephadex.brainflex.model.enums.MembershipStatus;
import cephadex.brainflex.model.enums.MembershipTier;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Membership {

    private MembershipTier tier = MembershipTier.FREE;
    private MembershipStatus status = MembershipStatus.NONE;

    /**
     * When the current paid period (or trial) began. Null while tier is FREE.
     */
    private LocalDateTime startedAt;

    /**
     * When the current paid period ends. Renewal extends this; cancellation
     * leaves it pointing at the access cutoff so we keep features enabled
     * through the end of the paid period.
     */
    private LocalDateTime currentPeriodEnd;

    /**
     * If true, the subscription will not auto-renew after currentPeriodEnd.
     * Set when the user cancels but the period has not yet lapsed.
     */
    private Boolean cancelAtPeriodEnd = false;

    /**
     * Organization that granted this user's seat. Only populated when tier is
     * ORG_SEAT — billing/plan state lives on the organization in that case.
     */
    private String sourceOrganizationId;

    /**
     * Opaque external billing references. Populated by the Stripe integration
     * once it lands; safe to leave null for now.
     */
    private String stripeCustomerId;
    private String stripeSubscriptionId;

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
    private LocalDateTime monthlyCountPeriodStart;

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
    private LocalDateTime quotaResetsAt;
}
