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
        String deckId,
        String deckCoverImageUrl,
        String deckBackgroundImageUrl,
        String themeId,
        List<DeckElement> deckSnapshot,
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
                session.getStatus(),
                session.getPhase(),
                session.getHostUserId(),
                session.getDeckId(),
                session.getDeckCoverImageUrl(),
                session.getDeckBackgroundImageUrl(),
                session.getThemeId(),
                session.getDeckSnapshot(),
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
