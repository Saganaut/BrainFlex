package cephadex.brainflex.dto;

import java.time.LocalDateTime;

import cephadex.brainflex.model.PlayerStats;

public sealed interface UserDTO {
    // The shared contract
    interface View {
        String id();

        String userName();
    }

    record Public(
            String id,
            String userName,
            String pictureUrl,
            PlayerStats stats)
            implements UserDTO, View {
    }

    record Private(
            String id,
            String email,
            String userName,
            String googleId,
            String pictureUrl,
            PlayerStats stats,
            LocalDateTime lastLogin,
            LocalDateTime createdAt)
            implements UserDTO, View {
    }
}