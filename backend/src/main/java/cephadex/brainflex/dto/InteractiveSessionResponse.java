/**
 * Public-facing representation of a InteractiveSession. The full deck snapshot is
 * included so clients can render the deck independently of the source Deck
 * document (which may have been edited since the interactiveSession was created).
 *
 * Identity: every player crosses the wire as a session-scoped {@code playerId}
 * (see {@link cephadex.brainflex.model.session.InteractiveSessionPlayer#playerId}) —
 * the real account {@code userId} never ships. The host is identified the same
 * way via {@code hostPlayerId}. Display fields are projected through
 * {@link PublicUserSnapshot}, which drops {@code userId} from {@link UserSnapshot}.
 *
 * Self-identification: {@code viewerPlayerId} carries the caller's playerId on
 * REST responses (constructed through {@link #forViewer}); it is {@code null} on
 * broadcasts where the server has no per-viewer context. Frontends should
 * capture this once on initial fetch and keep it across subsequent broadcasts.
 *
 * Note: the snapshot here includes correct-answer fields. For pre-IN_PROGRESS
 * lobby reads that's fine; mid-round the gameplay broadcast uses the redacted
 * RoundStartMessage instead.
 */
package cephadex.brainflex.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import cephadex.brainflex.model.element.DeckElement;
import cephadex.brainflex.model.enums.RoundPhase;
import cephadex.brainflex.model.enums.SessionLifecycle;
import cephadex.brainflex.model.enums.ResponseMode;
import cephadex.brainflex.model.enums.SessionFormat;
import cephadex.brainflex.model.org.Team;
import cephadex.brainflex.model.session.InteractiveSession;
import cephadex.brainflex.model.session.InteractiveSessionPlayer;
import cephadex.brainflex.model.shared.PublicUserSnapshot;
import cephadex.brainflex.model.shared.UserSnapshot;
import cephadex.brainflex.model.session.InteractiveSessionSettings;

public record InteractiveSessionResponse(
                String id,
                String roomCode,
                String inviteToken,
                SessionLifecycle status,
                RoundPhase phase,
                // Chunk 24 — chrome flavor frozen at create time. Drives which UI
                // shell (GameOver vs SessionSummary) the client mounts.
                SessionFormat format,
                // Session-scoped playerId of the host. Derived from the host's
                // player record (the host is always seeded as a player at
                // create time). The host's real userId never crosses the wire.
                String hostPlayerId,
                // Chunk 13 — denormalized host display fields for lobby header.
                String hostName,
                String hostAvatarUrl,
                String deckId,
                // Version pin — value of Deck.version at create time. Together
                // with deckId it answers "which authored revision was played?"
                // independent of the frozen deckSnapshot copy below.
                int deckVersion,
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
                Instant lobbyOpenedAt,
                // Zero-based index into deckSnapshot of the element currently in
                // play. 0 == first element; human-facing round numbers are
                // currentRound + 1 ("Round 1 of N"). Advances as each round
                // completes and equals deckSnapshot.size() - 1 on the final
                // round. Raw 0-based value, mirroring RoundStartMessage.round.
                int currentRound,
                // Total rounds in the session == deckSnapshot.size() (one round
                // per DeckElement; rounds are positional, not grouped). Shipped
                // explicitly so clients can render "Round X of Y" without
                // reaching into deckSnapshot. Mirrors RoundStartMessage.totalRounds.
                int totalRounds,
                // Chunk 24 — host-runtime overlays exposed so reconnecting hosts (and
                // post-game review) can rebuild reveal + freeze state without waiting
                // for a fresh broadcast. revealedElementIds lists every element the
                // host has manually surfaced via reveal-now; elementResponseModeOverrides
                // maps the current freeze-state map (only entries with a value of
                // NOT_ACCEPTING_RESPONSES need a UI cue, but the full map ships for
                // symmetry with the model field).
                List<String> revealedElementIds,
                Map<String, ResponseMode> elementResponseModeOverrides,
                // Caller's session-scoped playerId — non-null only when the
                // response is built through {@link #forViewer(InteractiveSession, String)}.
                // Always null on broadcasts (no per-viewer context). Clients
                // should latch the first non-null value and ignore subsequent
                // nulls so identity survives lobby rebroadcasts.
                String viewerPlayerId,
                Instant createdAt,
                Instant startedAt) {

        /**
         * Broadcast constructor — no viewer context. {@code viewerPlayerId} is
         * always null. Use this for STOMP rebroadcasts where the message goes
         * to a topic with many subscribers.
         */
        public InteractiveSessionResponse(InteractiveSession session) {
                this(session, null);
        }

        /**
         * Factory for REST responses where the caller is known. Resolves
         * {@code viewerPlayerId} by matching {@code viewerUserId} against the
         * session's player roster. Pass {@code null} for anonymous callers
         * (spectator GET on a public lobby) — viewerPlayerId will be null.
         */
        public static InteractiveSessionResponse forViewer(InteractiveSession session, String viewerUserId) {
                String viewerPlayerId = viewerUserId == null
                                ? null
                                : session.getPlayers().stream()
                                                .filter(p -> viewerUserId.equals(p.getUserId()))
                                                .findFirst()
                                                .map(InteractiveSessionPlayer::getPlayerId)
                                                .orElse(null);
                return new InteractiveSessionResponse(session, viewerPlayerId);
        }

        private InteractiveSessionResponse(InteractiveSession session, String viewerPlayerId) {
                this(
                                session.getId(),
                                session.getRoomCode(),
                                session.getInviteToken(),
                                session.getStatus(),
                                session.getPhase(),
                                session.getContent().getFormat(),
                                resolveHostPlayerId(session),
                                session.getHostName(),
                                session.getHostAvatarUrl(),
                                session.getDeckId(),
                                session.getDeckVersion(),
                                session.getContent().getElements(),
                                session.getContent().getSettings(),
                                session.getPlayers().stream().map(InteractiveSessionPlayerDTO::new).toList(),
                                session.getTeams(),
                                session.getCustomRoomCode(),
                                session.getSpectatorCount(),
                                session.getLobbyOpenedAt(),
                                session.getCurrentRound(),
                                session.getContent().getElements().size(),
                                session.getRevealedElementIds() == null
                                                ? List.of()
                                                : List.copyOf(session.getRevealedElementIds()),
                                session.getElementResponseModeOverrides() == null
                                                ? Map.of()
                                                : Map.copyOf(session.getElementResponseModeOverrides()),
                                viewerPlayerId,
                                session.getCreatedAt(),
                                session.getStartedAt());
        }

        private static String resolveHostPlayerId(InteractiveSession session) {
                String hostUserId = session.getHostUserId();
                if (hostUserId == null) return null;
                return session.getPlayers().stream()
                                .filter(p -> hostUserId.equals(p.getUserId()))
                                .findFirst()
                                .map(InteractiveSessionPlayer::getPlayerId)
                                .orElse(null);
        }

        /**
         * Safe subset of InteractiveSessionPlayer broadcast to all clients —
         * no answer data, no real userId. {@code playerId} is the
         * session-scoped public handle; {@code user} is the display projection
         * via {@link PublicUserSnapshot}.
         */
        public record InteractiveSessionPlayerDTO(
                        String playerId,
                        PublicUserSnapshot user,
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
                        Instant lastSeenAt) {

                public InteractiveSessionPlayerDTO(InteractiveSessionPlayer player) {
                        this(
                                        player.getPlayerId(),
                                        PublicUserSnapshot.from(player.getUser()),
                                        player.getScore(),
                                        player.getTeamId(),
                                        player.getAvatarKey(),
                                        player.getColorTag(),
                                        player.getCurrentStreak(),
                                        player.getEndStats().longestStreak(),
                                        player.getEndStats().accuracy(),
                                        player.getEndStats().reactionsSent(),
                                        player.getSpeedBonusTotal(),
                                        player.isLateJoin(),
                                        player.isDisconnected(),
                                        player.getLastSeenAt());
                }
        }
}
