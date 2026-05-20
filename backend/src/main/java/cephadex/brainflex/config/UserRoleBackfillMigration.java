/**
 * One-shot migration that backfills the {@code roles} field on existing
 * {@code users} documents to {@code [USER]}.
 *
 * Until chunk 20 landed, {@code User.roles} did not exist; existing user
 * documents have no {@code roles} field, so Spring Data deserializes the
 * Java field to null. {@code AuthoritiesService} and {@code AdminProperties}
 * already tolerate null safely, but a normalized field is easier to reason
 * about in queries and reports.
 *
 * Gated on {@code --migrate.user-roles=true} so it never runs during a
 * normal boot. Triggered by a short-lived Spring Boot process started by
 * {@code scripts/migrate-user-roles.sh}.
 *
 * Safe to re-run: the query only matches documents missing the field, so
 * existing role assignments (USER, MODERATOR, ADMIN) are never overwritten.
 */
package cephadex.brainflex.config;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import cephadex.brainflex.model.User;
import cephadex.brainflex.model.enums.UserRole;

@Configuration
@ConditionalOnProperty(name = "migrate.user-roles", havingValue = "true")
public class UserRoleBackfillMigration {

    @Bean
    @SuppressWarnings("unused")
    ApplicationRunner runUserRoleBackfill(
            MongoTemplate mongoTemplate,
            ApplicationContext applicationContext) {
        return (ApplicationArguments args) -> {
            try {
                System.out.println("=== User-role backfill migration: starting ===");
                Query missing = new Query(Criteria.where("roles").exists(false));
                long candidates = mongoTemplate.count(missing, User.class);
                long updated = mongoTemplate.updateMulti(
                        missing,
                        new Update().set("roles", List.of(UserRole.USER.name())),
                        User.class).getModifiedCount();
                System.out.println("=== User-role backfill migration: done ===");
                System.out.println("  candidates: " + candidates);
                System.out.println("  updated:    " + updated);
            } finally {
                int exit = SpringApplication.exit(applicationContext, () -> 0);
                System.exit(exit);
            }
        };
    }
}
