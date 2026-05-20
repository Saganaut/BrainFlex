/**
 * Cron sweep that boots ScheduledInteractiveSession rows when their start
 * time arrives. Runs every 30s; the `(status, scheduledStartAt)` compound
 * index keeps the query cheap even with a large backlog.
 *
 * Boot is best-effort per row: a single bad row (e.g. its deck was deleted)
 * won't stop the sweep from booting the others.
 */
package cephadex.brainflex.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import cephadex.brainflex.model.ScheduledInteractiveSession;
import cephadex.brainflex.model.enums.ScheduleStatus;
import cephadex.brainflex.repository.ScheduledInteractiveSessionRepository;

@Component
public class ScheduledInteractiveSessionScheduler {

    private static final Logger log = LoggerFactory.getLogger(ScheduledInteractiveSessionScheduler.class);

    private final ScheduledInteractiveSessionRepository scheduleRepository;
    private final ScheduledInteractiveSessionService service;

    public ScheduledInteractiveSessionScheduler(ScheduledInteractiveSessionRepository scheduleRepository,
                                                ScheduledInteractiveSessionService service) {
        this.scheduleRepository = scheduleRepository;
        this.service = service;
    }

    @Scheduled(fixedDelay = 30_000, initialDelay = 30_000)
    public void bootDueSessions() {
        List<ScheduledInteractiveSession> due = scheduleRepository
                .findByStatusAndScheduledStartAtBefore(ScheduleStatus.SCHEDULED, LocalDateTime.now());
        if (due.isEmpty()) return;

        for (ScheduledInteractiveSession schedule : due) {
            try {
                service.boot(schedule.getId());
            } catch (Exception e) {
                log.warn("Scheduled boot failed for {}: {}", schedule.getId(), e.getMessage());
            }
        }
    }
}
