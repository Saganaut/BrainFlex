/**
 * One-shot migration that promotes the legacy free-form {@code Deck.tags}
 * strings into first-class {@link cephadex.brainflex.model.Tag} documents.
 *
 * Gated on {@code --migrate.legacy-tags=true} so it never runs during a
 * normal boot. Triggered by {@code scripts/migrate-legacy-tags.sh}, which
 * boots a short-lived Spring Boot process and exits when this runner
 * completes.
 *
 * For each deck with a non-empty {@code tags} list and an empty
 * {@code tagIds}: every legacy string is slugified, find-or-created as an
 * uncurated Tag, and the resulting slug is appended to the deck's
 * {@code tagIds}. The original {@code tags} list is left alone — the
 * read-time hydrator now overrides it from {@code tagIds}, so the legacy
 * value is harmless and useful as a paper trail until we strip the field.
 *
 * Safe to re-run: existing tagIds are not overwritten, and Tag rows are
 * upserted by slug.
 */
package cephadex.brainflex.config;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cephadex.brainflex.model.Deck;
import cephadex.brainflex.model.Tag;
import cephadex.brainflex.repository.DeckRepository;
import cephadex.brainflex.service.TagService;

@Configuration
@ConditionalOnProperty(name = "migrate.legacy-tags", havingValue = "true")
public class LegacyTagMigration {

    @Bean
    @SuppressWarnings("unused")
    ApplicationRunner runLegacyTagMigration(
            DeckRepository deckRepository,
            TagService tagService,
            ApplicationContext applicationContext) {
        return (ApplicationArguments args) -> {
            try {
                System.out.println("=== Legacy tag migration: starting ===");
                int decksTouched = 0;
                int tagsCreated = 0;

                List<Deck> decks = deckRepository.findAll();
                for (Deck deck : decks) {
                    if (deck.getTagIds() != null && !deck.getTagIds().isEmpty()) continue;
                    List<String> legacy = deck.getTags();
                    if (legacy == null || legacy.isEmpty()) continue;

                    Set<String> tagIds = new LinkedHashSet<>();
                    for (String legacyTag : legacy) {
                        if (legacyTag == null || legacyTag.isBlank()) continue;
                        Tag tag = tagService.findOrCreateFromLegacyTag(legacyTag);
                        if (tag == null) continue;
                        if (tag.getCreatedAt() != null && tag.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(1))) {
                            tagsCreated++;
                        }
                        tagIds.add(tag.getId());
                    }
                    if (tagIds.isEmpty()) continue;
                    deck.setTagIds(new ArrayList<>(tagIds));
                    deckRepository.save(deck);
                    decksTouched++;
                }

                int recounted = tagService.recomputeDeckCounts(deckRepository.findAll());

                System.out.println("=== Legacy tag migration: done ===");
                System.out.println("  decks updated:    " + decksTouched);
                System.out.println("  tags newly added: " + tagsCreated);
                System.out.println("  tag counts dirty: " + recounted);
            } finally {
                int exit = SpringApplication.exit(applicationContext, () -> 0);
                System.exit(exit);
            }
        };
    }
}
