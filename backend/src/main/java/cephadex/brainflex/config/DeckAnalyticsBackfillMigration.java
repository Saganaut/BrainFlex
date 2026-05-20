/**
 * One-shot backfill that wipes {@code deck_analytics} and replays every
 * finished {@link cephadex.brainflex.model.InteractiveSession} through
 * {@link cephadex.brainflex.service.DeckAnalyticsService#recordGame} in
 * chronological order, producing a deterministic per-deck rollup.
 *
 * Gated on {@code --migrate.deck-analytics=true} so a normal boot is a no-op;
 * mirrors the chunk-15 {@code GameHistoryBackfillMigration} pattern. The
 * runner exits the JVM after completion so an operator can re-run the migration
 * without leaving a stray Spring context behind.
 *
 * Unlike the game-history backfill, this one <b>resets</b> the target collection
 * first — {@code recordGame} is not idempotent (it increments running averages
 * + counters), so the only safe replay strategy is "delete and replay from
 * zero."
 */
package cephadex.brainflex.config;

import java.util.Comparator;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cephadex.brainflex.model.InteractiveSession;
import cephadex.brainflex.model.enums.InteractiveSessionStatus;
import cephadex.brainflex.repository.InteractiveSessionRepository;
import cephadex.brainflex.service.DeckAnalyticsService;

@Configuration
@ConditionalOnProperty(name = "migrate.deck-analytics", havingValue = "true")
public class DeckAnalyticsBackfillMigration {

    @Bean
    @SuppressWarnings("unused")
    ApplicationRunner runDeckAnalyticsBackfill(
            InteractiveSessionRepository sessionRepository,
            DeckAnalyticsService deckAnalyticsService,
            ApplicationContext applicationContext) {
        return (ApplicationArguments args) -> {
            try {
                System.out.println("=== Deck-analytics backfill migration: starting ===");
                System.out.println("  wiping deck_analytics collection...");
                deckAnalyticsService.deleteAll();

                long visited = 0;
                long finishedReplayed = 0;
                long skippedNoDeck = 0;

                List<InteractiveSession> sessions = sessionRepository.findAll();
                sessions.sort(Comparator.comparing(
                        s -> s.getEndedAt() != null ? s.getEndedAt() : s.getCreatedAt(),
                        Comparator.nullsLast(Comparator.naturalOrder())));

                for (InteractiveSession session : sessions) {
                    visited++;
                    if (session.getStatus() != InteractiveSessionStatus.FINISHED) continue;
                    if (session.getDeckId() == null) {
                        skippedNoDeck++;
                        continue;
                    }
                    deckAnalyticsService.recordGame(session);
                    finishedReplayed++;
                }

                System.out.println("=== Deck-analytics backfill migration: done ===");
                System.out.println("  sessions visited:     " + visited);
                System.out.println("  finished replayed:    " + finishedReplayed);
                System.out.println("  skipped (no deck):    " + skippedNoDeck);
            } finally {
                int exit = SpringApplication.exit(applicationContext, () -> 0);
                System.exit(exit);
            }
        };
    }
}
