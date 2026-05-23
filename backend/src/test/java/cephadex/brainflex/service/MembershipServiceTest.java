/**
 * Unit tests for {@link MembershipService#canStartInteractiveSession(User)}.
 *
 * Three buckets:
 *   - {@code monthlyInteractiveSessionLimit == 0} → always returns true (unlimited).
 *   - {@code count < limit} → true.
 *   - {@code count >= limit} → false, unless the stored {@code monthlyCountPeriodStart}
 *     sits in a previous month (in which case the counter is treated as zero, mirroring
 *     the {@link GameHistoryService} roll-over write path).
 */
package cephadex.brainflex.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import cephadex.brainflex.model.org.Membership;
import cephadex.brainflex.model.user.User;
class MembershipServiceTest {

    private final MembershipService service = new MembershipService();

    @Test
    void canStartInteractiveSession_NullUser_ReturnsFalse() {
        assertFalse(service.canStartInteractiveSession(null));
    }

    @Test
    void canStartInteractiveSession_NoMembership_ReturnsTrue() {
        User u = new User();
        u.setMembership(null);
        assertTrue(service.canStartInteractiveSession(u));
    }

    @Test
    void canStartInteractiveSession_ZeroLimit_ReturnsTrue() {
        User u = userWithLimit(0, 999);
        assertTrue(service.canStartInteractiveSession(u));
    }

    @Test
    void canStartInteractiveSession_CountBelowLimit_ReturnsTrue() {
        User u = userWithLimit(10, 3);
        assertTrue(service.canStartInteractiveSession(u));
    }

    @Test
    void canStartInteractiveSession_CountAtLimit_ReturnsFalse() {
        User u = userWithLimit(5, 5);
        assertFalse(service.canStartInteractiveSession(u));
    }

    @Test
    void canStartInteractiveSession_CountAboveLimit_ReturnsFalse() {
        User u = userWithLimit(5, 7);
        assertFalse(service.canStartInteractiveSession(u));
    }

    @Test
    void canStartInteractiveSession_StaleMonthlyStart_ReturnsTrue() {
        // Counter says 99/5 but periodStart is two months back; the read-side
        // gate must treat that as zero so the user isn't gated after the
        // window rolled over.
        User u = userWithLimit(5, 99);
        u.getMembership().setMonthlyCountPeriodStart(Instant.parse("2020-01-01T00:00:00Z"));
        assertTrue(service.canStartInteractiveSession(u));
    }

    @Test
    void monthlyCountFor_StaleStart_ReturnsZero() {
        Membership m = new Membership();
        m.setMonthlyInteractiveSessionCount(42);
        m.setMonthlyCountPeriodStart(Instant.parse("2020-01-01T00:00:00Z"));
        assertEquals(0, service.monthlyCountFor(m, Instant.parse("2026-05-21T12:00:00Z")));
    }

    @Test
    void monthlyCountFor_SameMonth_ReturnsStoredCount() {
        Membership m = new Membership();
        m.setMonthlyInteractiveSessionCount(7);
        m.setMonthlyCountPeriodStart(Instant.parse("2026-05-01T00:00:00Z"));
        assertEquals(7, service.monthlyCountFor(m, Instant.parse("2026-05-21T12:00:00Z")));
    }

    private static User userWithLimit(int limit, int currentCount) {
        User u = new User();
        Membership m = new Membership();
        m.setMonthlyInteractiveSessionLimit(limit);
        m.setMonthlyInteractiveSessionCount(currentCount);
        m.setMonthlyCountPeriodStart(Instant.now());
        u.setMembership(m);
        return u;
    }
}
