/**
 * One-shot migration that backfills the discovery metadata added in chunk 02
 * (publishStatus, publishedAt, language, difficulty, license).
 *
 * Existing decks with a null publishStatus get:
 * - {@code PUBLISHED + publishedAt=createdAt} for system seeds, so the welcome
 *   tour stays visible on the Explore surface
 * - {@code DRAFT} for user-owned decks, so authors get a chance to review
 *   before their content is auto-shipped to the public feed
 *
 * Language defaults to "en", difficulty to MEDIUM, license to
 * ALL_RIGHTS_RESERVED — matches the new field defaults so the migration is
 * idempotent (re-running it doesn't overwrite anything that's already filled
 * in).
 *
 * Gated on {@code --migrate.discovery-metadata=true} so it never runs during
 * a normal boot. Triggered by {@code scripts/migrate-discovery-metadata.sh}.
 */
package cephadex.brainflex.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cephadex.brainflex.model.Deck;
import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.License;
import cephadex.brainflex.model.enums.PublishStatus;
import cephadex.brainflex.repository.DeckRepository;

@Configuration
@ConditionalOnProperty(name = "migrate.discovery-metadata", havingValue = "true")
public class DiscoveryMetadataMigration {

    @Bean
    @SuppressWarnings("unused")
    ApplicationRunner runDiscoveryMetadataMigration(
            DeckRepository deckRepository,
            ApplicationContext applicationContext) {
        return (ApplicationArguments args) -> {
            try {
                System.out.println("=== Discovery metadata migration: starting ===");
                int published = 0;
                int drafted = 0;
                int languageDefaulted = 0;
                int difficultyDefaulted = 0;
                int licenseDefaulted = 0;

                for (Deck deck : deckRepository.findAll()) {
                    boolean dirty = false;

                    if (deck.getPublishStatus() == null) {
                        if (deck.isSystem()) {
                            deck.setPublishStatus(PublishStatus.PUBLISHED);
                            deck.setPublishedAt(deck.getCreatedAt());
                            published++;
                        } else {
                            deck.setPublishStatus(PublishStatus.DRAFT);
                            drafted++;
                        }
                        dirty = true;
                    }
                    if (deck.getLanguage() == null || deck.getLanguage().isBlank()) {
                        deck.setLanguage("en");
                        languageDefaulted++;
                        dirty = true;
                    }
                    if (deck.getDifficulty() == null) {
                        deck.setDifficulty(Difficulty.MEDIUM);
                        difficultyDefaulted++;
                        dirty = true;
                    }
                    if (deck.getLicense() == null) {
                        deck.setLicense(License.ALL_RIGHTS_RESERVED);
                        licenseDefaulted++;
                        dirty = true;
                    }

                    if (dirty) deckRepository.save(deck);
                }

                System.out.println("=== Discovery metadata migration: done ===");
                System.out.println("  published (system): " + published);
                System.out.println("  drafted (user):     " + drafted);
                System.out.println("  language defaulted: " + languageDefaulted);
                System.out.println("  difficulty default: " + difficultyDefaulted);
                System.out.println("  license defaulted:  " + licenseDefaulted);
            } finally {
                int exit = SpringApplication.exit(applicationContext, () -> 0);
                System.exit(exit);
            }
        };
    }
}
