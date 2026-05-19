package cephadex.brainflex.dto;

import java.time.LocalDateTime;
import java.util.List;

import cephadex.brainflex.model.Membership;
import cephadex.brainflex.model.PlayerStats;
import cephadex.brainflex.model.User;
import cephadex.brainflex.model.element.Image;
import cephadex.brainflex.model.element.ImageVariant;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(oneOf = { UserDTO.GuestUser.class, UserDTO.RegisteredUser.class })
public sealed interface UserDTO {
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
            implements UserDTO, View {

        public GuestUser(User user, Image picture) {
            this(
                    String.valueOf(user.getId()),
                    user.getUserName(),
                    user.getIsGuest(),
                    largestUrl(picture, user.getPictureUrl()),
                    picture,
                    user.getStats());
        }
    }

    record RegisteredUser(
            String id,
            String email,
            String name,
            String userName,
            Boolean isGuest,
            String googleId,
            String pictureUrl,
            Image picture,
            PlayerStats stats,
            Membership membership,
            Boolean newsletter,
            List<String> organizationIds,
            String activeThemeId,
            LocalDateTime lastLogin,
            LocalDateTime createdAt)
            implements UserDTO, View {

        public RegisteredUser(User user, Image picture) {
            this(
                    String.valueOf(user.getId()),
                    user.getEmail(),
                    user.getName(),
                    user.getUserName(),
                    user.getIsGuest(),
                    user.getGoogleId(),
                    largestUrl(picture, user.getPictureUrl()),
                    picture,
                    user.getStats(),
                    user.getMembership(),
                    user.getNewsletter(),
                    user.getOrganizationIds() == null ? List.of() : List.copyOf(user.getOrganizationIds()),
                    user.getActiveThemeId(),
                    user.getLastLogin(),
                    user.getCreatedAt());
        }
    }

    /** Largest variant URL, or the fallback (typically the OAuth URL) when
     *  the image carries no variants. Null when both are blank. */
    static String largestUrl(Image picture, String fallback) {
        if (picture != null) {
            ImageVariant largest = picture.largestVariant();
            if (largest != null && largest.url() != null && !largest.url().isBlank()) {
                return largest.url();
            }
        }
        return fallback;
    }
}
