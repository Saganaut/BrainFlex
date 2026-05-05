/**
 * Public-facing representation of a GameSession.
 * Omits internal fields (raw answer data, question order) that clients
 * should not see, and flattens SessionPlayer into a safe SessionPlayerDTO.
 */
package cephadex.brainflex.dto;

import java.time.LocalDateTime;
import java.util.List;

import cephadex.brainflex.model.GameSession;
import cephadex.brainflex.model.GameSettings;
import cephadex.brainflex.model.SessionPlayer;
import cephadex.brainflex.model.enums.GameStatus;
import cephadex.brainflex.model.enums.GameType;

public record GameSessionDTO(
        String id,
        String roomCode,
        String inviteToken,
        GameType type,
        GameStatus status,
        String hostUserId,
        String contentPackId,
        GameSettings settings,
        List<SessionPlayerDTO> players,
        int currentRound,
        LocalDateTime createdAt,
        LocalDateTime startedAt) {

    public GameSessionDTO(GameSession session) {
        this(
                session.getId(),
                session.getRoomCode(),
                session.getInviteToken(),
                session.getType(),
                session.getStatus(),
                session.getHostUserId(),
                session.getContentPackId(),
                session.getSettings(),
                session.getPlayers().stream().map(SessionPlayerDTO::new).toList(),
                session.getCurrentRound(),
                session.getCreatedAt(),
                session.getStartedAt());
    }

    /** Safe subset of SessionPlayer broadcast to all clients — no answer data. */
    public record SessionPlayerDTO(
            String userId,
            String userName,
            String pictureUrl,
            boolean isGuest,
            int score) {

        public SessionPlayerDTO(SessionPlayer player) {
            this(
                    player.getUserId(),
                    player.getUserName(),
                    player.getPictureUrl(),
                    player.isGuest(),
                    player.getScore());
        }
    }
}
