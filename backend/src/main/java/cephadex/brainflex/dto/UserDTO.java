package cephadex.brainflex.dto;

import java.time.LocalDateTime;
import java.util.List;

import cephadex.brainflex.model.Membership;
import cephadex.brainflex.model.PlayerStats;
import cephadex.brainflex.model.User;
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

    record GuestUser(
            String id,
            String userName,
            Boolean isGuest,
            String pictureUrl,
            PlayerStats stats)
            implements UserDTO, View {
        public GuestUser(User user) {
            this(
                    String.valueOf(user.getId()),
                    user.getUserName(),
                    user.getIsGuest(),
                    user.getPictureUrl(),
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
            PlayerStats stats,
            Membership membership,
            Boolean newsletter,
            List<String> organizationIds,
            String activeThemeId,
            LocalDateTime lastLogin,
            LocalDateTime createdAt)
            implements UserDTO, View {
        public RegisteredUser(User user) {
            this(
                    String.valueOf(user.getId()),
                    user.getEmail(),
                    user.getName(),
                    user.getUserName(),
                    user.getIsGuest(),
                    user.getGoogleId(),
                    user.getPictureUrl(),
                    user.getStats(),
                    user.getMembership(),
                    user.getNewsletter(),
                    user.getOrganizationIds() == null ? List.of() : List.copyOf(user.getOrganizationIds()),
                    user.getActiveThemeId(),
                    user.getLastLogin(),
                    user.getCreatedAt());
        }
    }
}