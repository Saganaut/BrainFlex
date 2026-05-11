/**
 * Public-facing representation of a Showcase.
 * Omits internal fields (raw answer data, question order) that clients
 * should not see, and flattens ShowcasePlayer into a safe ShowcasePlayerDTO.
 */
package cephadex.brainflex.dto;

import java.time.LocalDateTime;
import java.util.List;

import cephadex.brainflex.model.Showcase;
import cephadex.brainflex.model.ShowcaseSettings;
import cephadex.brainflex.model.ShowcasePlayer;
import cephadex.brainflex.model.enums.GameStatus;
import cephadex.brainflex.model.enums.GameType;

public record ShowcaseDTO(
        String id,
        String roomCode,
        String inviteToken,
        GameType type,
        GameStatus status,
        String hostUserId,
        String deckId,
        String deckCoverImageUrl,
        String deckBackgroundImageUrl,
        ShowcaseSettings settings,
        List<ShowcasePlayerDTO> players,
        int currentRound,
        LocalDateTime createdAt,
        LocalDateTime startedAt) {

    public ShowcaseDTO(Showcase session) {
        this(
                session.getId(),
                session.getRoomCode(),
                session.getInviteToken(),
                session.getType(),
                session.getStatus(),
                session.getHostUserId(),
                session.getDeckId(),
                session.getDeckCoverImageUrl(),
                session.getDeckBackgroundImageUrl(),
                session.getSettings(),
                session.getPlayers().stream().map(ShowcasePlayerDTO::new).toList(),
                session.getCurrentRound(),
                session.getCreatedAt(),
                session.getStartedAt());
    }

    /** Safe subset of ShowcasePlayer broadcast to all clients — no answer data. */
    public record ShowcasePlayerDTO(
            String userId,
            String userName,
            String pictureUrl,
            boolean isGuest,
            int score) {

        public ShowcasePlayerDTO(ShowcasePlayer player) {
            this(
                    player.getUserId(),
                    player.getUserName(),
                    player.getPictureUrl(),
                    player.isGuest(),
                    player.getScore());
        }
    }
}
