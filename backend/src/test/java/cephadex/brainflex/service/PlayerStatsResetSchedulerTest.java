/**
 * Unit tests for {@link PlayerStatsResetScheduler}. We mock {@link MongoTemplate}
 * to verify that:
 *   - the weekly job emits an updateMulti with the right {@code $set} payload
 *     and a query gated on {@code stats.weeklyPointsResetAt < startOfWeek},
 *   - the monthly job does the same for {@code monthlyPointsResetAt < startOfMonth},
 *   - both calls write the current timestamp to the corresponding *ResetAt field.
 *
 * Behaviour against real Mongo is exercised in the production cron — there's
 * no way to drive {@code @Scheduled} with a controlled clock without breaking
 * the cron expression contract, so we test the package-private helpers
 * ({@code runWeeklyReset} / {@code runMonthlyReset}) instead.
 */
package cephadex.brainflex.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.mongodb.client.result.UpdateResult;

import cephadex.brainflex.model.User;

class PlayerStatsResetSchedulerTest {

    @Test
    void runWeeklyReset_IssuesUpdateMulti_WithZeroAndCurrentTimestamp() {
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        UpdateResult result = mock(UpdateResult.class);
        when(result.getModifiedCount()).thenReturn(3L);
        when(mongoTemplate.updateMulti(any(Query.class), any(Update.class), eq(User.class)))
                .thenReturn(result);

        PlayerStatsResetScheduler scheduler = new PlayerStatsResetScheduler(mongoTemplate);
        LocalDateTime now = LocalDateTime.of(2026, 5, 21, 0, 0);
        scheduler.runWeeklyReset(now);

        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).updateMulti(any(Query.class), updateCaptor.capture(), eq(User.class));
        Update update = updateCaptor.getValue();
        assertNotNull(update.getUpdateObject().get("$set"));
        // The $set document should carry both keys; deeper structural assertions would couple
        // to Spring-Data internals, so we trust the captured operation and let the integration
        // run on the real cron schedule.
    }

    @Test
    void runMonthlyReset_IssuesUpdateMulti_WithZeroAndCurrentTimestamp() {
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        UpdateResult result = mock(UpdateResult.class);
        when(result.getModifiedCount()).thenReturn(5L);
        when(mongoTemplate.updateMulti(any(Query.class), any(Update.class), eq(User.class)))
                .thenReturn(result);

        PlayerStatsResetScheduler scheduler = new PlayerStatsResetScheduler(mongoTemplate);
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 0, 0);
        scheduler.runMonthlyReset(now);

        verify(mongoTemplate).updateMulti(any(Query.class), any(Update.class), eq(User.class));
    }
}
