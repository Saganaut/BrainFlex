/**
 * MongoDB repository for ScheduledInteractiveSession. The cron sweep uses
 * `findByStatusAndScheduledStartAtBefore` to pick up rows ready to boot;
 * `findByHostUserId` powers the "/api/scheduled-interactive-sessions/mine" list.
 */
package cephadex.brainflex.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.ScheduledInteractiveSession;
import cephadex.brainflex.model.enums.ScheduleStatus;

public interface ScheduledInteractiveSessionRepository
        extends MongoRepository<ScheduledInteractiveSession, String> {

    List<ScheduledInteractiveSession> findByHostUserIdOrderByScheduledStartAtAsc(String hostUserId);

    List<ScheduledInteractiveSession> findByStatusAndScheduledStartAtBefore(
            ScheduleStatus status, LocalDateTime cutoff);

    Optional<ScheduledInteractiveSession> findByCreatedInteractiveSessionId(String interactiveSessionId);
}
