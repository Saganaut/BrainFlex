/**
 * One-shot migration that backfills the chunk 20 profile additions on existing
 * {@code users} and {@code organizations} documents.
 *
 * Three independent backfills, executed in one pass:
 *   - {@code user.displayName} is set to the first non-blank of
 *     {@code userName} / {@code name} where the field is currently blank.
 *     Read paths still chain the fallback, but a materialised field saves a
 *     null-check on every render and keeps Mongo queries (e.g. for the
 *     leaderboard "name like" filter) honest.
 *   - {@code user.notificationPrefs} is materialised to the spec defaults via
 *     {@link NotificationPrefs#withDefaults()} when null. The {@code
 *     marketingEmail} field is mirrored from the legacy {@code user.newsletter}
 *     flag so a user who opted in once doesn't silently lose that grant on
 *     first read.
 *   - {@code organization.emailDomain} values are lowercased so the OAuth
 *     auto-join hook can do a case-insensitive lookup without normalising at
 *     read time.
 *
 * Gated on {@code --migrate.user-profile=true} so it never runs during a normal
 * boot. Triggered by {@code scripts/migrate-user-profile.sh}.
 *
 * Safe to re-run: the per-user / per-org checks short-circuit when the field
 * is already in the post-migration shape.
 */
package cephadex.brainflex.config;

import java.util.Locale;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cephadex.brainflex.model.user.User;
import cephadex.brainflex.repository.OrganizationRepository;
import cephadex.brainflex.repository.UserRepository;
import cephadex.brainflex.model.user.NotificationPrefs;
import cephadex.brainflex.model.org.Organization;

@Configuration
@ConditionalOnProperty(name = "migrate.user-profile", havingValue = "true")
public class UserProfileBackfillMigration {

    @Bean
    @SuppressWarnings("unused")
    ApplicationRunner runUserProfileBackfill(
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            ApplicationContext applicationContext) {
        return (ApplicationArguments args) -> {
            try {
                System.out.println("=== User-profile backfill migration: starting ===");

                int displayNameUpdated = 0;
                int notificationPrefsUpdated = 0;
                int marketingEmailMirrored = 0;
                int usersScanned = 0;

                for (User user : userRepository.findAll()) {
                    usersScanned++;
                    boolean changed = false;

                    if (isBlank(user.getDisplayName())) {
                        String fallback = firstNonBlank(user.getUserName(), user.getName());
                        if (!isBlank(fallback)) {
                            user.setDisplayName(fallback);
                            displayNameUpdated++;
                            changed = true;
                        }
                    }

                    if (user.getNotificationPrefs() == null) {
                        NotificationPrefs prefs = NotificationPrefs.withDefaults();
                        prefs.setMarketingEmail(user.isNewsletter());
                        marketingEmailMirrored++;
                        user.setNotificationPrefs(prefs);
                        notificationPrefsUpdated++;
                        changed = true;
                    }

                    if (changed) {
                        userRepository.save(user);
                    }
                }

                int emailDomainLowercased = 0;
                int orgsScanned = 0;
                for (Organization org : organizationRepository.findAll()) {
                    orgsScanned++;
                    String domain = org.getEmailDomain();
                    if (domain == null || domain.isBlank())
                        continue;
                    String normalised = domain.toLowerCase(Locale.ROOT);
                    if (!normalised.equals(domain)) {
                        org.setEmailDomain(normalised);
                        organizationRepository.save(org);
                        emailDomainLowercased++;
                    }
                }

                System.out.println("=== User-profile backfill migration: done ===");
                System.out.println("  users scanned:              " + usersScanned);
                System.out.println("  displayName backfilled:     " + displayNameUpdated);
                System.out.println("  notificationPrefs seeded:   " + notificationPrefsUpdated);
                System.out.println("  marketingEmail mirrored:    " + marketingEmailMirrored);
                System.out.println("  orgs scanned:               " + orgsScanned);
                System.out.println("  emailDomain lowercased:     " + emailDomainLowercased);
            } finally {
                int exit = SpringApplication.exit(applicationContext, () -> 0);
                System.exit(exit);
            }
        };
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String firstNonBlank(String a, String b) {
        if (!isBlank(a))
            return a;
        if (!isBlank(b))
            return b;
        return null;
    }
}
