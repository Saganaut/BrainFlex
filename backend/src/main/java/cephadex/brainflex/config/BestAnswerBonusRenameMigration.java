/**
 * One-shot migration that renames the legacy {@code bestAnswerBonus} field to
 * its current name {@code bestAnswerPoints} inside every element of every
 * deck's {@code elements[]} array.
 *
 * The Java rename happened in chunk 24 but the Mongo key was left untouched
 * via {@code @Field("bestAnswerBonus")} back-compat annotations on each of the
 * twelve scoreable question records. This migration moves the persisted key
 * forward so the annotations can be dropped.
 *
 * Works directly on raw documents via {@link MongoTemplate#getCollection} —
 * loading through {@link cephadex.brainflex.repository.DeckRepository} would
 * route the legacy field through the (now-removed) annotation and lose the
 * source value. The aggregation pipeline copies {@code bestAnswerBonus} into
 * {@code bestAnswerPoints} (preferring an already-renamed value if both
 * happen to be present) then unsets the legacy field.
 *
 * Gated on {@code --migrate.best-answer-bonus=true} so a normal boot is a
 * no-op; triggered by {@code scripts/migrate-best-answer-bonus.sh}. Safe to
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
@ConditionalOnProperty(name = "migrate.best-answer-bonus", havingValue = "true")
public class BestAnswerBonusRenameMigration {

        @Bean
        @SuppressWarnings("unused")
        ApplicationRunner runBestAnswerBonusRename(
                        MongoTemplate mongoTemplate,
                        ApplicationContext applicationContext) {
                return (ApplicationArguments args) -> {
                        try {
                                System.out.println("=== bestAnswerBonus → bestAnswerPoints rename: starting ===");

                                Document filter = new Document(
                                                "elements.bestAnswerBonus", new Document("$exists", true));

                                List<Document> pipeline = List.of(
                                                new Document("$set", new Document("elements",
                                                                new Document("$map", new Document()
                                                                                .append("input", "$elements")
                                                                                .append("as", "el")
                                                                                .append("in", new Document(
                                                                                                "$mergeObjects",
                                                                                                List.of(
                                                                                                                "$$el",
                                                                                                                new Document("bestAnswerPoints",
                                                                                                                                new Document("$ifNull",
                                                                                                                                                List.of(
                                                                                                                                                                "$$el.bestAnswerPoints",
                                                                                                                                                                "$$el.bestAnswerBonus"))))))))),
                                                new Document("$set", new Document("elements",
                                                                new Document("$map", new Document()
                                                                                .append("input", "$elements")
                                                                                .append("as", "el")
                                                                                .append("in", new Document(
                                                                                                "$unsetField",
                                                                                                new Document()
                                                                                                                .append("field", "bestAnswerBonus")
                                                                                                                .append("input", "$$el")))))));

                                UpdateResult result = mongoTemplate.getCollection("decks")
                                                .updateMany(filter, pipeline);

                                System.out.println("=== bestAnswerBonus → bestAnswerPoints rename: done ===");
                                System.out.println("  decks matched:    " + result.getMatchedCount());
                                System.out.println("  decks modified:   " + result.getModifiedCount());
                        } finally {
                                int exit = SpringApplication.exit(applicationContext, () -> 0);
                                System.exit(exit);
                        }
                };
        }
}
