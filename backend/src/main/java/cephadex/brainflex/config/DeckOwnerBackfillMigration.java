/**
 * One-shot migration that backfills the {@code deck_collaborators} join
 * collection with an OWNER row per existing deck.
 *
 * For every deck with a non-null {@code creatorUserId} and no existing OWNER
 * row, inserts:
 *   userId   = creatorUserId
 *   role     = OWNER
 *   invitedAt = deck.createdAt
 *   acceptedAt = deck.createdAt
 *
 * System decks (creatorUserId may be null) are skipped — system decks are not
 * owned by anyone and their authorization runs through {@code Deck.isSystem}.
 *
 * Safe to re-run: existing collaborator rows are not overwritten and the
 * compound (deckId, userId) unique index prevents duplicates.
 *
 * Gated on {@code --migrate.deck-owners=true} so it never runs during a normal
 * boot. Triggered by {@code scripts/migrate-deck-owners.sh}.
 */
package cephadex.brainflex.config;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DuplicateKeyException;

import cephadex.brainflex.model.deck.Deck;
import cephadex.brainflex.model.deck.DeckCollaborator;
import cephadex.brainflex.model.enums.CollaboratorRole;
import cephadex.brainflex.repository.DeckCollaboratorRepository;
import cephadex.brainflex.repository.DeckRepository;

@Configuration
@ConditionalOnProperty(name = "migrate.deck-owners", havingValue = "true")
public class DeckOwnerBackfillMigration {

    @Bean
    @SuppressWarnings("unused")
    ApplicationRunner runDeckOwnerBackfill(
            DeckRepository deckRepository,
            DeckCollaboratorRepository collaboratorRepository,
            ApplicationContext applicationContext) {
        return (ApplicationArguments args) -> {
            try {
                System.out.println("=== Deck-owner backfill migration: starting ===");
                int inserted = 0;
                int skippedSystem = 0;
                int skippedExisting = 0;
                int skippedOrphan = 0;

                for (Deck deck : deckRepository.findAll()) {
                    if (deck.isSystem()) {
                        skippedSystem++;
                        continue;
                    }
                    String creatorId = deck.getCreatorUserId();
                    if (creatorId == null || creatorId.isBlank()) {
                        skippedOrphan++;
                        continue;
                    }
                    Optional<DeckCollaborator> existingOwner = collaboratorRepository
                            .findByDeckIdAndRole(deck.getId(), CollaboratorRole.OWNER);
                    if (existingOwner.isPresent()) {
                        skippedExisting++;
                        continue;
                    }
                    DeckCollaborator row = new DeckCollaborator();
                    row.setId(UUID.randomUUID().toString());
                    row.setDeckId(deck.getId());
                    row.setUserId(creatorId);
                    row.setRole(CollaboratorRole.OWNER);
                    row.setInvitedByUserId(creatorId);
                    Instant stamp = deck.getCreatedAt() == null
                            ? Instant.now()
                            : deck.getCreatedAt();
                    row.setInvitedAt(stamp);
                    row.setAcceptedAt(stamp);
                    try {
                        collaboratorRepository.insert(row);
                        inserted++;
                    } catch (DuplicateKeyException dup) {
                        // Lost a race with a parallel insert (or a partial
                        // earlier run); count it as already present and move on.
                        skippedExisting++;
                    }
                }

                System.out.println("=== Deck-owner backfill migration: done ===");
                System.out.println("  inserted:          " + inserted);
                System.out.println("  skipped (system):  " + skippedSystem);
                System.out.println("  skipped (existing):" + skippedExisting);
                System.out.println("  skipped (orphan):  " + skippedOrphan);
            } finally {
                int exit = SpringApplication.exit(applicationContext, () -> 0);
                System.exit(exit);
            }
        };
    }
}
