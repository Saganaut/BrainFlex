/**
 * Plain-POJO unit tests covering the field defaults added by chunk 20 — the
 * "User/Organization/Theme/Membership additions" pass. The intent is narrow:
 * if a future refactor strips the field initialiser from one of these models
 * (e.g. someone deletes {@code = new ArrayList<>()} thinking it's redundant),
 * this test fails loudly before a {@code null} silently propagates into a
 * service layer that assumes the collection is materialised.
 *
 * Deliberately not a {@code @SpringBootTest} — we're poking at default state,
 * not Mongo. Keeps the suite cheap (sub-second) and avoids bringing in the
 * full repository fan-out that {@code HealthControllerTest} needs.
 */
package cephadex.brainflex.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import cephadex.brainflex.model.enums.NotificationKind;
import cephadex.brainflex.model.enums.ThemeMode;
import cephadex.brainflex.model.enums.UserRole;
import cephadex.brainflex.model.media.GalleryImage;
import cephadex.brainflex.model.org.Membership;
import cephadex.brainflex.model.org.Organization;
import cephadex.brainflex.model.org.OrganizationPlan;
import cephadex.brainflex.model.session.AudienceSubmission;
import cephadex.brainflex.model.session.BestAnswerVote;
import cephadex.brainflex.model.theme.Theme;
import cephadex.brainflex.model.user.NotificationPrefs;
import cephadex.brainflex.model.user.PlayerStats;
import cephadex.brainflex.model.user.User;

class ModelDefaultsTest {

    @Test
    void user_hasSpecDefaults() {
        User user = new User();

        assertThat(user.getRoles()).containsExactly(UserRole.USER);
        assertThat(user.getOrganizationIds()).isEmpty();
        assertThat(user.getPictureVariants()).isEmpty();
        assertThat(user.getTagInterests()).isEmpty();
        assertThat(user.getLocale()).isEqualTo("en");
        assertThat(user.getDisplayName()).isNull();
        assertThat(user.getCustomAvatarUrl()).isNull();
        assertThat(user.getBio()).isNull();
        assertThat(user.getLocation()).isNull();
        assertThat(user.getWebsiteUrl()).isNull();
        assertThat(user.getNotificationPrefs()).isNull();
        assertThat(user.getStats()).isNotNull();
        assertThat(user.getMembership()).isNotNull();
    }

    @Test
    void playerStats_zeroesAllCountersAndCreatesEmptyMaps() {
        PlayerStats stats = new PlayerStats();

        assertThat(stats.getGamesPlayed()).isZero();
        assertThat(stats.getHighScore()).isZero();
        assertThat(stats.getTotalPoints()).isZero();
        assertThat(stats.getDailyLoginStreak()).isZero();
        assertThat(stats.getLongestStreak()).isZero();
        assertThat(stats.getPerfectGames()).isZero();
        assertThat(stats.getTotalReactionsSent()).isZero();
        assertThat(stats.getWeeklyPoints()).isZero();
        assertThat(stats.getMonthlyPoints()).isZero();
        assertThat(stats.getPresentedByKind()).isEmpty();
        assertThat(stats.getCorrectByKind()).isEmpty();
        assertThat(stats.getWeeklyPointsResetAt()).isNull();
        assertThat(stats.getMonthlyPointsResetAt()).isNull();
        assertThat(stats.getLastPlayedAt()).isNull();
    }

    @Test
    void organization_hasSpecDefaults() {
        Organization org = new Organization();

        assertThat(org.getPlan()).isNotNull();
        assertThat(org.getLogoVariants()).isEmpty();
        assertThat(org.isAllowPublicJoin()).isFalse();
        assertThat(org.getMemberCount()).isZero();
        // Organization now extends Auditable — createdAt / updatedAt are
        // populated by Spring Data's @CreatedDate / @LastModifiedDate hooks on
        // save, so a freshly constructed instance reads null on both.
        assertThat(org.getCreatedAt()).isNull();
        assertThat(org.getUpdatedAt()).isNull();
        assertThat(org.getEmailDomain()).isNull();
        assertThat(org.getInviteCode()).isNull();
        assertThat(org.getDefaultThemeId()).isNull();
    }

    @Test
    void theme_hasSpecDefaults() {
        Theme theme = new Theme();

        assertThat(theme.getHuePrimary()).isEqualTo(260);
        assertThat(theme.getHueAccent()).isEqualTo(25);
        assertThat(theme.getMode()).isEqualTo(ThemeMode.SYSTEM);
        assertThat(theme.getLogoVariants()).isEmpty();
        assertThat(theme.getBackgroundVariants()).isEmpty();
        assertThat(theme.getTokenOverrides()).isEmpty();
        assertThat(theme.getFontFamily()).isNull();
        assertThat(theme.getHeadingFontFamily()).isNull();
        assertThat(theme.getSoundThemeId()).isNull();
    }

    @Test
    void membership_hasSpecDefaults() {
        Membership membership = new Membership();

        assertThat(membership.getFeatureFlags()).isEmpty();
        assertThat(membership.getMonthlyInteractiveSessionCount()).isZero();
        assertThat(membership.getMonthlyInteractiveSessionLimit()).isZero();
        assertThat(membership.getQuotaResetsAt()).isNull();
        assertThat(membership.getMonthlyCountPeriodStart()).isNull();
    }

    @Test
    void organizationPlan_hasSpecDefaults() {
        OrganizationPlan plan = new OrganizationPlan();

        assertThat(plan.getFeatureFlags()).isEmpty();
        assertThat(plan.getMonthlyInteractiveSessionLimit()).isZero();
        assertThat(plan.getQuotaResetsAt()).isNull();
        assertThat(plan.getSeatLimit()).isZero();
    }

    @Test
    void galleryImage_hasSpecDefaults() {
        GalleryImage img = new GalleryImage();

        assertThat(img.getTags()).isEmpty();
        assertThat(img.getVariants()).isEmpty();
        assertThat(img.getWidth()).isNull();
        assertThat(img.getHeight()).isNull();
        assertThat(img.getAltText()).isNull();
        assertThat(img.getAttribution()).isNull();
        assertThat(img.getSourceUrl()).isNull();
        assertThat(img.getMimeType()).isNull();
        assertThat(img.getOriginalFileName()).isNull();
        assertThat(img.getSizeBytes()).isZero();
    }

    @Test
    void bestAnswerVote_defaultsWeightToOne() {
        assertThat(new BestAnswerVote().getWeight()).isEqualTo(1);
    }

    @Test
    void audienceSubmission_hasSpecDefaults() {
        AudienceSubmission sub = new AudienceSubmission();

        assertThat(sub.getUpvotes()).isZero();
        assertThat(sub.getDownvotes()).isZero();
        assertThat(sub.getModeratedByUserId()).isNull();
        assertThat(sub.getModeratedAt()).isNull();
        assertThat(sub.getModerationReason()).isNull();
    }

    @Test
    void notificationPrefs_bareConstructorIsEmpty() {
        NotificationPrefs prefs = new NotificationPrefs();

        assertThat(prefs.getInApp()).isEmpty();
        assertThat(prefs.getEmail()).isEmpty();
        assertThat(prefs.isWeeklyDigestEmail()).isTrue();
        assertThat(prefs.getMarketingEmail()).isNull();
    }

    @Test
    void notificationPrefs_withDefaultsPopulatesEveryKind() {
        NotificationPrefs prefs = NotificationPrefs.withDefaults();

        for (NotificationKind kind : NotificationKind.values()) {
            assertThat(prefs.getInApp()).containsEntry(kind, Boolean.TRUE);
            assertThat(prefs.getEmail()).containsKey(kind);
        }
        assertThat(prefs.getEmail()).containsEntry(NotificationKind.INTERACTIVE_SESSION_INVITE, Boolean.TRUE);
        assertThat(prefs.getEmail()).containsEntry(NotificationKind.COLLAB_INVITE, Boolean.TRUE);
        assertThat(prefs.getEmail()).containsEntry(NotificationKind.ORG_INVITE, Boolean.TRUE);
        assertThat(prefs.getEmail()).containsEntry(NotificationKind.DECK_COMMENT, Boolean.FALSE);
        assertThat(prefs.getEmail()).containsEntry(NotificationKind.SYSTEM, Boolean.FALSE);
    }

    @Test
    void notificationPrefs_helperFallsBackToSpecDefaultsForUnsetKinds() {
        NotificationPrefs prefs = new NotificationPrefs();

        // Empty maps -> helpers return spec defaults.
        assertThat(prefs.inAppEnabled(NotificationKind.DECK_COMMENT)).isTrue();
        assertThat(prefs.emailEnabled(NotificationKind.DECK_COMMENT)).isFalse();
        assertThat(prefs.emailEnabled(NotificationKind.INTERACTIVE_SESSION_INVITE)).isTrue();

        // Explicit overrides win over the spec default.
        prefs.getEmail().put(NotificationKind.INTERACTIVE_SESSION_INVITE, Boolean.FALSE);
        prefs.getInApp().put(NotificationKind.DECK_COMMENT, Boolean.FALSE);
        assertThat(prefs.emailEnabled(NotificationKind.INTERACTIVE_SESSION_INVITE)).isFalse();
        assertThat(prefs.inAppEnabled(NotificationKind.DECK_COMMENT)).isFalse();
    }
}
