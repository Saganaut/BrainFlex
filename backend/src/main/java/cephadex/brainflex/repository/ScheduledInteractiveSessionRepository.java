/**
 * MongoDB repository for ScheduledInteractiveSession. The cron sweep uses
 * `findByStatusAndScheduledStartAtBefore` to pick up rows ready to boot;
 * `findByHostUserId` powers the "/api/scheduled-interactive-sessions/mine" list.
 */
package cephadex.brainflex.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.enums.ScheduleStatus;
import cephadex.brainflex.model.session.ScheduledInteractiveSession;

public interface ScheduledInteractiveSessionRepository
                extends MongoRepository<ScheduledInteractiveSession, String> {

        List<ScheduledInteractiveSession> findByHostUserIdOrderByScheduledStartAtAsc(String hostUserId);

        List<ScheduledInteractiveSession> findByStatusAndScheduledStartAtBefore(
                        ScheduleStatus status, Instant cutoff);

        Optional<ScheduledInteractiveSession> findByCreatedInteractiveSessionId(String interactiveSessionId);
}
