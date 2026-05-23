/**
 * Quota gate around the host-can-create-a-session check.
 *
 * Chunk 20 left the monthly counter ({@code Membership.monthlyInteractiveSessionCount})
 * and the per-tier soft cap ({@code Membership.monthlyInteractiveSessionLimit}) on the
 * model already populated, plus the counter roll-over logic on
 * {@link cephadex.brainflex.service.GameHistoryService#recordFinish}. This service is the
 * single read-side gate the controllers consult before letting a host start one more
 * session.
 *
 * The check is intentionally a pure read of the embedded counter: the GameHistoryService
 * write path is the only place that mutates the counter, so a stale value here only
 * matters when the user just finished a session in another tab and the
 * post-{@code endGame} response hasn't returned yet — close enough for a soft quota.
 */
package cephadex.brainflex.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import cephadex.brainflex.model.org.Membership;
import cephadex.brainflex.model.user.User;

@Service
public class MembershipService {

    /**
     * Returns {@code true} when {@code user} is allowed to host another interactive
     * session this month. Two short-circuits:
     * - {@code Membership.monthlyInteractiveSessionLimit == 0} means "unlimited"
     * (the paid tiers' default) and always returns true.
     * - If the counter's {@code monthlyCountPeriodStart} sits in an earlier
     * calendar month than {@code Instant.now()}, the counter is treated as
     * zero — the roll-over write happens in {@link GameHistoryService} on the
     * next finish, so reading a stale counter from a prior month would otherwise
     * keep the user gated even after their fresh quota window opened.
     */
    public boolean canStartInteractiveSession(User user) {
        if (user == null)
            return false;
        Membership membership = user.getMembership();
        if (membership == null)
            return true;

        int limit = membership.getMonthlyInteractiveSessionLimit();
        if (limit <= 0)
            return true;

        int count = monthlyCountFor(membership, Instant.now());
        return count < limit;
    }

    /**
     * The counter value to compare against the limit. Returns {@code 0} when the
     * stored {@code monthlyCountPeriodStart} is in a previous calendar month,
     * mirroring the roll-over logic in {@link GameHistoryService} so the read-side
     * gate and the write-side bump agree.
     */
    public int monthlyCountFor(Membership membership, Instant now) {
        if (membership == null)
            return 0;
        Instant periodStart = membership.getMonthlyCountPeriodStart();
        if (periodStart == null)
            return membership.getMonthlyInteractiveSessionCount();
        java.time.ZonedDateTime psZ = periodStart.atZone(java.time.ZoneOffset.UTC);
        java.time.ZonedDateTime nowZ = now.atZone(java.time.ZoneOffset.UTC);
        boolean sameMonth = psZ.getYear() == nowZ.getYear()
                && psZ.getMonthValue() == nowZ.getMonthValue();
        return sameMonth ? membership.getMonthlyInteractiveSessionCount() : 0;
    }
}
