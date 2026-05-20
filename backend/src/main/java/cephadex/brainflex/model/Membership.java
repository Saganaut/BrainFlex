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
}
