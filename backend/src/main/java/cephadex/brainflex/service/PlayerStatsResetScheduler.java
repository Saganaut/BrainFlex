/**
 * Spring-scheduled crons that zero out the {@link cephadex.brainflex.model.user.PlayerStats}
 * rolling counters at the start of each ISO week / calendar month.
 *
 * Two independent jobs:
 *   - {@link #resetWeeklyPoints()} fires at 00:00 UTC every Monday and zeros
 *     {@code stats.weeklyPoints} on every user that hasn't already been reset
 *     this week. The "already this week" guard reads {@code weeklyPointsResetAt}
 *     so a manual re-run on the same day doesn't double-zero a user who already
 *     played after the cron fired.
 *   - {@link #resetMonthlyPoints()} fires at 00:00 UTC on the 1st of the month
 *     and zeros {@code stats.monthlyPoints} with the same guard against
 *     same-window double-reset.
 *
 * Both queries skip users whose reset timestamp is already inside the current
 * window, so a re-run is idempotent. The {@code lastPlayedAt} field is not
 * touched — it's an absolute high-water mark, not a rolling tally.
 */
package cephadex.brainflex.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import cephadex.brainflex.model.user.User;

@Component
public class PlayerStatsResetScheduler {

    private static final Logger log = LoggerFactory.getLogger(PlayerStatsResetScheduler.class);

    private final MongoTemplate mongoTemplate;

    public PlayerStatsResetScheduler(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Monday 00:00 UTC — zero {@code stats.weeklyPoints} for any user not yet reset
     * this week.
     */
    @Scheduled(cron = "0 0 0 ? * MON", zone = "UTC")
    public void resetWeeklyPoints() {
        runWeeklyReset(Instant.now(Clock.systemUTC()));
    }

    /**
     * First of each month 00:00 UTC — zero {@code stats.monthlyPoints} for any user
     * not yet reset this month.
     */
    @Scheduled(cron = "0 0 0 1 * *", zone = "UTC")
    public void resetMonthlyPoints() {
        runMonthlyReset(Instant.now(Clock.systemUTC()));
    }

    /**
     * Package-private hook so the unit test can drive both windows with a
     * controlled clock.
     */
    long runWeeklyReset(Instant now) {
        Instant windowStart = startOfIsoWeek(now);
        Query q = new Query(new Criteria().orOperator(
                Criteria.where("stats.weeklyPointsResetAt").exists(false),
                Criteria.where("stats.weeklyPointsResetAt").is(null),
                Criteria.where("stats.weeklyPointsResetAt").lt(windowStart)));
        long modified = mongoTemplate.updateMulti(
                q,
                new Update()
                        .set("stats.weeklyPoints", 0)
                        .set("stats.weeklyPointsResetAt", now),
                User.class).getModifiedCount();
        log.info("PlayerStatsResetScheduler.weekly: reset {} users (windowStart={})", modified, windowStart);
        return modified;
    }

    /**
     * Package-private hook so the unit test can drive both windows with a
     * controlled clock.
     */
    long runMonthlyReset(Instant now) {
        Instant windowStart = startOfMonth(now);
        Query q = new Query(new Criteria().orOperator(
                Criteria.where("stats.monthlyPointsResetAt").exists(false),
                Criteria.where("stats.monthlyPointsResetAt").is(null),
                Criteria.where("stats.monthlyPointsResetAt").lt(windowStart)));
        long modified = mongoTemplate.updateMulti(
                q,
                new Update()
                        .set("stats.monthlyPoints", 0)
                        .set("stats.monthlyPointsResetAt", now),
                User.class).getModifiedCount();
        log.info("PlayerStatsResetScheduler.monthly: reset {} users (windowStart={})", modified, windowStart);
        return modified;
    }

    private static Instant startOfIsoWeek(Instant now) {
        LocalDate date = now.atZone(ZoneOffset.UTC).toLocalDate()
                .with(ChronoField.DAY_OF_WEEK, 1);
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private static Instant startOfMonth(Instant now) {
        return now.atZone(ZoneOffset.UTC).toLocalDate().withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
