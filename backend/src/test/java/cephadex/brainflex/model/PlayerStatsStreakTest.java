/**
 * Unit tests for {@link PlayerStats#nextDailyLoginStreak} — the day-boundary
 * helper that advances {@code PlayerStats.dailyLoginStreak} after a finished
 * game. Day boundaries are timezone-sensitive (a user in LA can finish a game
 * at 23:30 local Tuesday and another at 00:30 local Wednesday and that is two
 * different days, but UTC would call both Wednesday) so the matrix below
 * pokes at the zone parameter explicitly rather than relying on the system
 * default.
 */
package cephadex.brainflex.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import cephadex.brainflex.model.user.PlayerStats;

class PlayerStatsStreakTest {

    private static final ZoneId LA = ZoneId.of("America/Los_Angeles");

    private static Instant at(ZoneId zone, int y, int m, int d, int h, int min) {
        return LocalDateTime.of(y, m, d, h, min).atZone(zone).toInstant();
    }

    @Test
    void firstEverPlay_startsStreakAtOne() {
        Instant now = at(ZoneOffset.UTC, 2026, 5, 22, 10, 0);
        assertThat(PlayerStats.nextDailyLoginStreak(0, null, now, ZoneOffset.UTC)).isEqualTo(1);
    }

    @Test
    void firstEverPlay_ignoresAnyStaleStreakValue() {
        // dailyLoginStreak is normally 0 when lastPlayedAt is null, but if some
        // corrupt doc carries (streak=7, lastPlayedAt=null) we still reset to
        // 1 — the null timestamp is the source of truth.
        Instant now = at(ZoneOffset.UTC, 2026, 5, 22, 10, 0);
        assertThat(PlayerStats.nextDailyLoginStreak(7, null, now, ZoneOffset.UTC)).isEqualTo(1);
    }

    @Test
    void sameLocalDay_keepsStreakUnchanged() {
        Instant earlier = at(LA, 2026, 5, 22, 9, 0);
        Instant later = at(LA, 2026, 5, 22, 23, 30);
        assertThat(PlayerStats.nextDailyLoginStreak(4, earlier, later, LA)).isEqualTo(4);
    }

    @Test
    void sameLocalDay_promotesZeroStreakToOne() {
        // Guards a backfill scenario: a user finishes their first game today
        // and a bad migration left dailyLoginStreak=0 with lastPlayedAt set to
        // earlier the same day. We refuse to land back on 0.
        Instant earlier = at(LA, 2026, 5, 22, 9, 0);
        Instant later = at(LA, 2026, 5, 22, 23, 30);
        assertThat(PlayerStats.nextDailyLoginStreak(0, earlier, later, LA)).isEqualTo(1);
    }

    @Test
    void exactlyOneLocalDayLater_incrementsByOne() {
        Instant yesterday = at(LA, 2026, 5, 22, 22, 0);
        Instant today = at(LA, 2026, 5, 23, 1, 0);
        assertThat(PlayerStats.nextDailyLoginStreak(3, yesterday, today, LA)).isEqualTo(4);
    }

    @Test
    void twoDayGap_resetsToOne() {
        Instant twoDaysAgo = at(LA, 2026, 5, 20, 18, 0);
        Instant today = at(LA, 2026, 5, 22, 10, 0);
        assertThat(PlayerStats.nextDailyLoginStreak(9, twoDaysAgo, today, LA)).isEqualTo(1);
    }

    @Test
    void zoneMattersForDayBoundary() {
        // Straddle LA midnight: previous = LA May 22 23:30 (UTC May 23
        // 06:30 in PDT), now = LA May 23 00:30 (UTC May 23 07:30). In LA
        // those are different days (May 22 → May 23) so the streak bumps;
        // in UTC they're the same day (both May 23) so the streak holds.
        // Confirms the helper honours the passed-in zone instead of the
        // JVM default.
        Instant previous = at(LA, 2026, 5, 22, 23, 30);
        Instant now = at(LA, 2026, 5, 23, 0, 30);
        assertThat(PlayerStats.nextDailyLoginStreak(2, previous, now, LA)).isEqualTo(3);
        assertThat(PlayerStats.nextDailyLoginStreak(2, previous, now, ZoneOffset.UTC)).isEqualTo(2);
    }

    @Test
    void clockSkewBackwards_treatedAsSameDayNoAdvance() {
        // If lastPlayedAt is somehow later than now (server clock drift,
        // restored backup), we don't want to reward the player with a
        // streak bump or drop them to 1. Hold steady.
        Instant later = at(LA, 2026, 5, 22, 12, 0);
        Instant earlier = at(LA, 2026, 5, 22, 9, 0);
        assertThat(PlayerStats.nextDailyLoginStreak(5, later, earlier, LA)).isEqualTo(5);
    }
}
