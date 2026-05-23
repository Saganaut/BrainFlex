/**
 * Cron sweep that boots ScheduledInteractiveSession rows when their start
 * time arrives. Runs every 30s; the `(status, scheduledStartAt)` compound
 * index keeps the query cheap even with a large backlog.
 *
 * Two passes per tick:
 *   1. "Starting soon" reminder — every SCHEDULED row whose start is within
 *      the next 5 minutes and that has not been reminded yet fires
 *      {@link NotificationEvents.ScheduledInteractiveSessionBootingEvent}
 *      per resolved invitee. The {@code startingSoonNotifiedAt} stamp prevents
 *      re-notification on subsequent sweeps.
 *   2. Boot — every SCHEDULED row whose start is in the past is booted into a
 *      live InteractiveSession.
 *
 * Boot is best-effort per row: a single bad row (e.g. its deck was deleted)
 * won't stop the sweep from booting the others.
 */
package cephadex.brainflex.service;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import cephadex.brainflex.model.enums.ScheduleStatus;
import cephadex.brainflex.model.session.InteractiveSessionInvite;
import cephadex.brainflex.model.session.ScheduledInteractiveSession;
import cephadex.brainflex.model.user.User;
import cephadex.brainflex.repository.InteractiveSessionInviteRepository;
import cephadex.brainflex.repository.ScheduledInteractiveSessionRepository;
import cephadex.brainflex.repository.UserRepository;

@Component
public class ScheduledInteractiveSessionScheduler {

    private static final Logger log = LoggerFactory.getLogger(ScheduledInteractiveSessionScheduler.class);

    /** How early before boot the "starting soon" reminder fires. */
    private static final java.time.Duration REMINDER_LEAD = java.time.Duration.ofMinutes(5);

    private final ScheduledInteractiveSessionRepository scheduleRepository;
    private final InteractiveSessionInviteRepository inviteRepository;
    private final UserRepository userRepository;
    private final ScheduledInteractiveSessionService service;
    private final ApplicationEventPublisher events;

    public ScheduledInteractiveSessionScheduler(ScheduledInteractiveSessionRepository scheduleRepository,
            InteractiveSessionInviteRepository inviteRepository,
            UserRepository userRepository,
            ScheduledInteractiveSessionService service,
            ApplicationEventPublisher events) {
        this.scheduleRepository = scheduleRepository;
        this.inviteRepository = inviteRepository;
        this.userRepository = userRepository;
        this.service = service;
        this.events = events;
    }

    @Scheduled(fixedDelay = 30_000, initialDelay = 30_000)
    public void sweep() {
        sendStartingSoonReminders();
        bootDueSessions();
    }

    void sendStartingSoonReminders() {
        Instant now = Instant.now();
        Instant soonCutoff = now.plus(REMINDER_LEAD);
        List<ScheduledInteractiveSession> upcoming = scheduleRepository
                .findByStatusAndScheduledStartAtBefore(ScheduleStatus.SCHEDULED, soonCutoff);
        for (ScheduledInteractiveSession schedule : upcoming) {
            // Skip rows already in the boot window (handled by bootDueSessions
            // below) and rows that have already been reminded for this run.
            if (schedule.getScheduledStartAt() == null)
                continue;
            if (schedule.getScheduledStartAt().isBefore(now))
                continue;
            if (schedule.getStartingSoonNotifiedAt() != null)
                continue;
            try {
                List<InteractiveSessionInvite> invites = inviteRepository
                        .findByScheduledInteractiveSessionId(schedule.getId());
                for (InteractiveSessionInvite invite : invites) {
                    String userId = resolveInviteeUserId(invite);
                    if (userId == null)
                        continue;
                    events.publishEvent(new NotificationEvents.ScheduledInteractiveSessionBootingEvent(
                            schedule.getId(), userId));
                }
                schedule.setStartingSoonNotifiedAt(now);
                scheduleRepository.save(schedule);
            } catch (Exception e) {
                log.warn("Starting-soon reminder failed for {}: {}", schedule.getId(), e.getMessage());
            }
        }
    }

    void bootDueSessions() {
        Instant now = Instant.now();
        List<ScheduledInteractiveSession> due = scheduleRepository
                .findByStatusAndScheduledStartAtBefore(ScheduleStatus.SCHEDULED, now);
        if (due.isEmpty())
            return;

        for (ScheduledInteractiveSession schedule : due) {
            try {
                service.boot(schedule.getId());
            } catch (Exception e) {
                log.warn("Scheduled boot failed for {}: {}", schedule.getId(), e.getMessage());
            }
        }
    }

    /**
     * Resolve a registered userId from an invite — prefer the redeemed id,
     * fall back to email match. Pending / unknown emails return null.
     */
    private String resolveInviteeUserId(InteractiveSessionInvite invite) {
        if (invite.getResolvedUserId() != null)
            return invite.getResolvedUserId();
        if (invite.getEmail() == null)
            return null;
        return userRepository.findByEmail(invite.getEmail())
                .map(User::getId)
                .orElse(null);
    }
}
