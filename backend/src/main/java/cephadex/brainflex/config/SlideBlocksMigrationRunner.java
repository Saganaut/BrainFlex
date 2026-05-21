/**
 * One-time migration to backfill {@link Slide#blocks()} from the legacy
 * {@link Slide#body()} field (chunk 10c).
 *
 * Gated on `--migrate.slide-blocks=true` so it never runs during a normal
 * boot. Idempotent: a slide that already has a non-empty `blocks` list is
 * skipped; only slides with a non-blank legacy `body` and no `blocks` get
 * the canonical `[BodyBlock(body)]` wrap.
 *
 * Mirrors {@link SampleDataSeeder}'s ApplicationRunner pattern — boots Spring,
 * runs once, exits.
 */
package cephadex.brainflex.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cephadex.brainflex.model.Deck;
import cephadex.brainflex.model.element.BodyBlock;
import cephadex.brainflex.model.element.DeckElement;
import cephadex.brainflex.model.element.Slide;
import cephadex.brainflex.model.element.SlideBlock;
import cephadex.brainflex.repository.DeckRepository;

@Configuration
@ConditionalOnProperty(name = "migrate.slide-blocks", havingValue = "true")
public class SlideBlocksMigrationRunner {

    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    @SuppressWarnings("unused")
    ApplicationRunner runSlideBlocksMigration(DeckRepository deckRepository) {
        return (ApplicationArguments args) -> {
            try {
                System.out.println("=== Slide-blocks migration: starting ===");
                int decksScanned = 0;
                int decksUpdated = 0;
                int slidesMigrated = 0;
                int slidesSkipped = 0;

                for (Deck deck : deckRepository.findAll()) {
                    decksScanned++;
                    List<DeckElement> elements = deck.getElements();
                    if (elements == null || elements.isEmpty()) continue;

                    boolean deckChanged = false;
                    List<DeckElement> migrated = new ArrayList<>(elements.size());
                    for (DeckElement element : elements) {
                        if (element instanceof Slide slide) {
                            if (hasBlocks(slide)) {
                                slidesSkipped++;
                                migrated.add(slide);
                                continue;
                            }
                            if (slide.body() == null || slide.body().isBlank()) {
                                migrated.add(slide);
                                continue;
                            }
                            migrated.add(withBlocks(slide, List.of(
                                    new BodyBlock(slide.id() + "-block-1", slide.body()))));
                            slidesMigrated++;
                            deckChanged = true;
                        } else {
                            migrated.add(element);
                        }
                    }
                    if (deckChanged) {
                        deck.setElements(migrated);
                        deckRepository.save(deck);
                        decksUpdated++;
                    }
                }

                System.out.println("=== Slide-blocks migration: done ===");
                System.out.println("  decks scanned:    " + decksScanned);
                System.out.println("  decks updated:    " + decksUpdated);
                System.out.println("  slides migrated:  " + slidesMigrated);
                System.out.println("  slides skipped:   " + slidesSkipped + " (already had blocks)");
            } finally {
                int exit = SpringApplication.exit(applicationContext, () -> 0);
                System.exit(exit);
            }
        };
    }

    private static boolean hasBlocks(Slide slide) {
        return slide.blocks() != null && !slide.blocks().isEmpty();
    }

    /** Reconstruct a Slide with a substituted `blocks` list. Lives here rather
     *  than in {@link cephadex.brainflex.service.DeckElementCloner} because
     *  the cloner intentionally preserves blocks on every clone — this is the
     *  one path that wants to replace them. */
    private static Slide withBlocks(Slide s, List<SlideBlock> blocks) {
        return new Slide(
                s.id(), s.slideKind(), s.body(), blocks,
                s.resultsDisplayType(), s.multipleSelectionsEnabled(),
                s.selectionsPerParticipant(), s.showResultsAsPercentage(),
                s.joinType(), s.showJoinInformation(), s.showQrCode(), s.showResponses(),
                s.heading(), s.participantInformation(),
                s.autoAdvanceSeconds(),
                s.chrome());
    }
}
