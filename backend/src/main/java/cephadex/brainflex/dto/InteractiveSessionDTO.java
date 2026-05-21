/**
 * Public-facing representation of a InteractiveSession. The full deck snapshot is
 * included so clients can render the deck independently of the source Deck
 * document (which may have been edited since the interactiveSession was created).
 *
 * Note: the snapshot here includes correct-answer fields. For pre-IN_PROGRESS
 * lobby reads that's fine; mid-round the gameplay broadcast uses the redacted
 * RoundStartMessage instead.
 */
package cephadex.brainflex.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import cephadex.brainflex.model.InteractiveSession;
import cephadex.brainflex.model.InteractiveSessionSettings;
import cephadex.brainflex.model.InteractiveSessionPlayer;
import cephadex.brainflex.model.Team;
import cephadex.brainflex.model.UserSnapshot;
import cephadex.brainflex.model.element.DeckElement;
import cephadex.brainflex.model.enums.InteractiveSessionStatus;
import cephadex.brainflex.model.enums.InteractiveSessionPhase;
import cephadex.brainflex.model.enums.ResponseMode;
import cephadex.brainflex.model.enums.SessionFormat;

public record InteractiveSessionDTO(
        String id,
        String roomCode,
        String inviteToken,
        InteractiveSessionStatus status,
        InteractiveSessionPhase phase,
        // Chunk 24 — chrome flavor frozen at create time. Drives which UI
        // shell (GameOver vs SessionSummary) the client mounts.
        SessionFormat format,
        String hostUserId,
        // Chunk 13 — denormalized host display fields for lobby header.
        String hostName,
        String hostAvatarUrl,
        String deckId,
        List<DeckElement> deckSnapshot,
        // Frozen deckCoverImageUrl / deckBackgroundImageUrl / themeId /
        // anonymousMode / allowReJoin / teamMode / autoBalanceTeams live on
        // `settings` — they're chrome/config knobs, not first-class session
        // state. `teams` is the live roster (empty when teamMode is off).
        InteractiveSessionSettings settings,
        List<InteractiveSessionPlayerDTO> players,
        List<Team> teams,
        String customRoomCode,
        int spectatorCount,
        LocalDateTime lobbyOpenedAt,
        int currentRound,
        // Chunk 24 — host-runtime overlays exposed so reconnecting hosts (and
        // post-game review) can rebuild reveal + freeze state without waiting
        // for a fresh broadcast. revealedElementIds lists every element the
        // host has manually surfaced via reveal-now; elementResponseModeOverrides
        // maps the current freeze-state map (only entries with a value of
        // NOT_ACCEPTING_RESPONSES need a UI cue, but the full map ships for
        // symmetry with the model field).
        List<String> revealedElementIds,
        Map<String, ResponseMode> elementResponseModeOverrides,
        LocalDateTime createdAt,
        LocalDateTime startedAt) {

    public InteractiveSessionDTO(InteractiveSession session) {
        this(
                session.getId(),
                session.getRoomCode(),
                session.getInviteToken(),
                session.getStatus(),
                session.getPhase(),
                session.getFormat(),
                session.getHostUserId(),
                session.getHostName(),
                session.getHostAvatarUrl(),
                session.getDeckId(),
                session.getDeckSnapshot(),
                session.getSettings(),
                session.getPlayers().stream().map(InteractiveSessionPlayerDTO::new).toList(),
                session.getTeams(),
                session.getCustomRoomCode(),
                session.getSpectatorCount(),
                session.getLobbyOpenedAt(),
                session.getCurrentRound(),
                session.getRevealedElementIds() == null
                        ? List.of()
                        : List.copyOf(session.getRevealedElementIds()),
                session.getElementResponseModeOverrides() == null
                        ? Map.of()
                        : Map.copyOf(session.getElementResponseModeOverrides()),
                session.getCreatedAt(),
                session.getStartedAt());
    }

    /** Safe subset of InteractiveSessionPlayer broadcast to all clients — no answer data. */
    public record InteractiveSessionPlayerDTO(
            UserSnapshot user,
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

        public InteractiveSessionPlayerDTO(InteractiveSessionPlayer player) {
            this(
                    player.getUser(),
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
