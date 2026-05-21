/**
 * Embedded subscription plan for an organization.
 *
 * Tracks the org-level billing state (tier, status, period) and the seat
 * accounting that determines how many members can occupy ORG_SEAT memberships
 * at once. Seat consumption itself lives on each User.membership; this record
 * is the authoritative source for the plan limit and renewal dates.
 *
 * Like Membership, Stripe IDs are stored opaquely so the billing integration
 * can be slotted in later without a schema change.
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
public class OrganizationPlan {

    private MembershipTier tier = MembershipTier.FREE;
    private MembershipStatus status = MembershipStatus.NONE;

    private int seatLimit = 0;

    private LocalDateTime startedAt;
    private LocalDateTime currentPeriodEnd;

    private Boolean cancelAtPeriodEnd = false;

    private String stripeCustomerId;
    private String stripeSubscriptionId;

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
    private LocalDateTime quotaResetsAt;
}
