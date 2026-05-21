/**
 * One-shot migration that renames the legacy top-level {@code recommendedPreset}
 * field on every deck document to its current name {@code defaultSessionFormat}.
 *
 * The Java rename happened earlier but the Mongo key was left untouched via the
 * {@code @Field("recommendedPreset")} / {@code @JsonProperty("defaultSessionFormat")}
 * / {@code @JsonAlias({"recommendedPreset"})} back-compat annotations on
 * {@link cephadex.brainflex.model.Deck}. This migration moves the persisted key
 * forward so the annotations can be dropped.
 *
 * Works directly on raw documents via {@link MongoTemplate#getCollection} — once
 * the annotations are removed, loading through {@link cephadex.brainflex.repository.DeckRepository}
 * would silently lose the legacy value. The aggregation pipeline copies
 * {@code recommendedPreset} into {@code defaultSessionFormat} (preferring an
 * already-renamed value if both happen to be present) then unsets the legacy
 * field.
 *
 * Gated on {@code --migrate.recommended-preset=true} so a normal boot is a
 * no-op; triggered by {@code scripts/migrate-recommended-preset.sh}. Safe to
 * re-run — once the legacy key is gone the filter matches nothing.
 */
package cephadex.brainflex.config;

import java.util.List;

import org.bson.Document;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.client.result.UpdateResult;

@Configuration
@ConditionalOnProperty(name = "migrate.recommended-preset", havingValue = "true")
public class RecommendedPresetRenameMigration {

    @Bean
    @SuppressWarnings("unused")
    ApplicationRunner runRecommendedPresetRename(
            MongoTemplate mongoTemplate,
            ApplicationContext applicationContext) {
        return (ApplicationArguments args) -> {
            try {
                System.out.println("=== recommendedPreset → defaultSessionFormat rename: starting ===");

                Document filter = new Document(
                        "recommendedPreset", new Document("$exists", true));

                List<Document> pipeline = List.of(
                        new Document("$set", new Document("defaultSessionFormat",
                                new Document("$ifNull", List.of(
                                        "$defaultSessionFormat",
                                        "$recommendedPreset")))),
                        new Document("$unset", "recommendedPreset"));

                UpdateResult result = mongoTemplate.getCollection("decks")
                        .updateMany(filter, pipeline);

                System.out.println("=== recommendedPreset → defaultSessionFormat rename: done ===");
                System.out.println("  decks matched:    " + result.getMatchedCount());
                System.out.println("  decks modified:   " + result.getModifiedCount());
            } finally {
                int exit = SpringApplication.exit(applicationContext, () -> 0);
                System.exit(exit);
            }
        };
    }
}
