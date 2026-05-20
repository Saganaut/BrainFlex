/**
 * One-shot migration that seeds the {@code game_history} collection from the
 * existing {@code interactive_session_results} + {@code interactive_sessions}
 * documents.
 *
 * Chunk 15 added per-user history rows that the live-session finish path
 * writes going forward. To make "every game I ever played" return all of a
 * user's pre-chunk sessions too, this runner walks the result documents and
 * pipes each through {@link GameHistoryService#recordFinish}. The service
 * already swallows duplicate-key errors, so re-runs are safe — the unique
 * {@code (userId, interactiveSessionId)} index gates each row.
 *
 * Gated on {@code --migrate.game-history=true} so it never runs during a
 * normal boot. Triggered by a short-lived Spring Boot process started by an
 * operator command (mirrors {@link UserRoleBackfillMigration} / scripts).
 */
package cephadex.brainflex.config;

import java.util.Optional;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cephadex.brainflex.model.InteractiveSession;
import cephadex.brainflex.model.InteractiveSessionResult;
import cephadex.brainflex.repository.InteractiveSessionRepository;
import cephadex.brainflex.repository.InteractiveSessionResultRepository;
import cephadex.brainflex.service.GameHistoryService;

@Configuration
@ConditionalOnProperty(name = "migrate.game-history", havingValue = "true")
public class GameHistoryBackfillMigration {

    @Bean
    @SuppressWarnings("unused")
    ApplicationRunner runGameHistoryBackfill(
            InteractiveSessionResultRepository resultRepository,
            InteractiveSessionRepository sessionRepository,
            GameHistoryService gameHistoryService,
            ApplicationContext applicationContext) {
        return (ApplicationArguments args) -> {
            try {
                System.out.println("=== Game-history backfill migration: starting ===");
                long visited = 0;
                long sessionsHydrated = 0;
                long sessionsMissing = 0;
                for (InteractiveSessionResult result : resultRepository.findAll()) {
                    visited++;
                    if (result.getInteractiveSessionId() == null || result.getPlacements() == null) {
                        continue;
                    }
                    Optional<InteractiveSession> session = sessionRepository.findById(result.getInteractiveSessionId());
                    if (session.isEmpty()) {
                        sessionsMissing++;
                        continue;
                    }
                    sessionsHydrated++;
                    InteractiveSession s = session.get();
                    // The session.endedAt is the better playedAt source than
                    // result.endedAt, but we fall back to it if the session row
                    // is missing the field for any reason (pre-chunk-13 data).
                    if (s.getEndedAt() == null) {
                        s.setEndedAt(result.getEndedAt());
                    }
                    gameHistoryService.recordFinish(s, result.getPlacements());
                }
                System.out.println("=== Game-history backfill migration: done ===");
                System.out.println("  results visited:     " + visited);
                System.out.println("  sessions hydrated:   " + sessionsHydrated);
                System.out.println("  sessions not found:  " + sessionsMissing);
            } finally {
                int exit = SpringApplication.exit(applicationContext, () -> 0);
                System.exit(exit);
            }
        };
    }
}
