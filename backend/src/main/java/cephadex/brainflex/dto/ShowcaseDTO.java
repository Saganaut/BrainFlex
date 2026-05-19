/**
 * Public-facing representation of a Showcase. The full deck snapshot is
 * included so clients can render the deck independently of the source Deck
 * document (which may have been edited since the showcase was created).
 *
 * Note: the snapshot here includes correct-answer fields. For pre-IN_PROGRESS
 * lobby reads that's fine; mid-round the gameplay broadcast uses the redacted
 * RoundStartMessage instead.
 */
package cephadex.brainflex.dto;

import java.time.LocalDateTime;
import java.util.List;

import cephadex.brainflex.model.Showcase;
import cephadex.brainflex.model.ShowcaseSettings;
import cephadex.brainflex.model.ShowcasePlayer;
import cephadex.brainflex.model.Team;
import cephadex.brainflex.model.element.DeckElement;
import cephadex.brainflex.model.enums.GameStatus;
import cephadex.brainflex.model.enums.ShowcasePhase;

public record ShowcaseDTO(
        String id,
        String roomCode,
        String inviteToken,
        GameStatus status,
        ShowcasePhase phase,
        String hostUserId,
        // Chunk 13 — denormalized host display fields for lobby header.
        String hostName,
        String hostAvatarUrl,
        String deckId,
        String deckCoverImageUrl,
        String deckBackgroundImageUrl,
        String themeId,
        List<DeckElement> deckSnapshot,
        ShowcaseSettings settings,
        List<ShowcasePlayerDTO> players,
        // Chunk 12 — team mode. `teams` is empty when teamMode=false.
        boolean teamMode,
        boolean autoBalanceTeams,
        List<Team> teams,
        // Chunk 13 — lobby polish fields.
        boolean anonymousMode,
        String customRoomCode,
        boolean allowReJoin,
        int spectatorCount,
        LocalDateTime lobbyOpenedAt,
        int currentRound,
        LocalDateTime createdAt,
        LocalDateTime startedAt) {

    public ShowcaseDTO(Showcase session) {
        this(
                session.getId(),
                session.getRoomCode(),
                session.getInviteToken(),
                session.getStatus(),
                session.getPhase(),
                session.getHostUserId(),
                session.getHostName(),
                session.getHostAvatarUrl(),
                session.getDeckId(),
                session.getDeckCoverImageUrl(),
                session.getDeckBackgroundImageUrl(),
                session.getThemeId(),
                session.getDeckSnapshot(),
                session.getSettings(),
                session.getPlayers().stream().map(ShowcasePlayerDTO::new).toList(),
                session.isTeamMode(),
                session.isAutoBalanceTeams(),
                session.getTeams(),
                session.isAnonymousMode(),
                session.getCustomRoomCode(),
                session.isAllowReJoin(),
                session.getSpectatorCount(),
                session.getLobbyOpenedAt(),
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
            int score,
            String teamId,
            // Chunk 13 — lobby avatar + presence + per-player chrome stats.
            String avatarKey,
            String colorTag,
            int currentStreak,
            int longestStreak,
            double accuracy,
            int reactionsSent,
            int speedBonusTotal,
            boolean lateJoin,
            boolean disconnected,
            LocalDateTime lastSeenAt) {

        public ShowcasePlayerDTO(ShowcasePlayer player) {
            this(
                    player.getUserId(),
                    player.getUserName(),
                    player.getPictureUrl(),
                    player.isGuest(),
                    player.getScore(),
                    player.getTeamId(),
                    player.getAvatarKey(),
                    player.getColorTag(),
                    player.getCurrentStreak(),
                    player.getLongestStreak(),
                    player.getAccuracy(),
                    player.getReactionsSent(),
                    player.getSpeedBonusTotal(),
                    player.isLateJoin(),
                    player.isDisconnected(),
                    player.getLastSeenAt());
        }
    }
}
