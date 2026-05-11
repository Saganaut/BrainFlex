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
}
