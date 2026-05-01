package cephadex.brainflex.dto;

import java.time.LocalDateTime;

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

    record GuestLoginRequest(String username) {}

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
                    user.getLastLogin(),
                    user.getCreatedAt());
        }
    }
}