package cephadex.brainflex.dto.user;

import static cephadex.brainflex.dto.user.UserResponse.externalIdentityOf;
import static cephadex.brainflex.dto.user.UserResponse.largestUrl;

import java.time.Instant;
import java.util.List;

import cephadex.brainflex.model.image.Image;
import cephadex.brainflex.model.org.Membership;
import cephadex.brainflex.model.user.PlayerStats;
import cephadex.brainflex.model.user.User;
import io.swagger.v3.oas.annotations.media.Schema;

// IDE throughing warnings for the static imports TODO: Resolve it
@SuppressWarnings("unused")
@Schema(oneOf = { UserResponse.GuestUser.class, UserResponse.RegisteredUser.class })
public sealed interface UserResponse {
    // The shared contract
    interface View {
        String id();

        String userName();
    }

    record GuestLoginRequest(String username) {
    }

    /**
     * `pictureUrl` is kept on the wire for back-compat (small consumers like
     * the leaderboard tile that don't care about size selection). It mirrors
     * the largest URL in `picture.variants`, or the OAuth URL when the user
     * hasn't uploaded their own avatar. Prefer `picture.variants` on the
     * client when rendering at a specific size.
     */
    record GuestUser(
            String id,
            String userName,
            Boolean isGuest,
            String pictureUrl,
            Image picture,
            PlayerStats stats)
            implements UserResponse, View {

        public GuestUser(User user, Image picture) {
            this(
                    String.valueOf(user.getId()),
                    user.getUserName(),
                    user.isGuest(),
                    largestUrl(picture, user.getPictureUrl()),
                    picture,
                    user.getStats());
        }
    }

    /**
     * The external OAuth identity tied to this account. {@code provider} is one of
     * the
     * {@link cephadex.brainflex.service.OAuthProviderService} constants
     * ({@code google} /
     * {@code discord} / {@code microsoft}); {@code id} is the provider-specific
     * subject.
     * Replaces the legacy flat {@code googleId} field, which only covered one of
     * the
     * three providers. Null only if every provider id column on {@link User} is
     * empty,
     * which shouldn't happen for a registered (non-guest) account.
     */
    record ExternalIdentity(String provider, String id) {
    }

    record RegisteredUser(
            String id,
            String email,
            String name,
            String userName,
            Boolean isGuest,
            ExternalIdentity externalIdentity,
            String pictureUrl,
            Image picture,
            PlayerStats stats,
            Membership membership,
            Boolean newsletter,
            List<String> organizationIds,
            String activeThemeId,
            String timezone,
            Instant emailVerifiedAt,
            Instant lastLogin,
            Instant createdAt)
            implements UserResponse, View {

        public RegisteredUser(User user, Image picture) {
            this(
                    String.valueOf(user.getId()),
                    user.getEmail(),
                    user.getName(),
                    user.getUserName(),
                    user.isGuest(),
                    externalIdentityOf(user),
                    largestUrl(picture, user.getPictureUrl()),
                    picture,
                    user.getStats(),
                    user.getMembership(),
                    user.isNewsletter(),
                    user.getOrganizationIds() == null ? List.of() : List.copyOf(user.getOrganizationIds()),
                    user.getActiveThemeId(),
                    user.getTimezone(),
                    user.getEmailVerifiedAt(),
                    user.getLastLogin(),
                    user.getCreatedAt());
        }
    }

    static ExternalIdentity externalIdentityOf(User user) {
        if (user.getGoogleId() != null)
            return new ExternalIdentity("google", user.getGoogleId());
        if (user.getDiscordId() != null)
            return new ExternalIdentity("discord", user.getDiscordId());
        if (user.getMicrosoftId() != null)
            return new ExternalIdentity("microsoft", user.getMicrosoftId());
        return null;
    }

    /**
     * Largest variant URL, or the fallback (typically the OAuth URL) when
     * the image carries no variants. Null when both are blank.
     */
    static String largestUrl(Image picture, String fallback) {
        if (picture != null) {
            String largest = picture.largestUrl();
            if (largest != null && !largest.isBlank()) {
                return largest;
            }
        }
        return fallback;
    }
}
