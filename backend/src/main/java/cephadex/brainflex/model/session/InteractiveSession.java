/**
 * The central document for an active or completed interactive session.
 *
 * <p>Tracks runtime state — room code, players, current round, phase — and
 * carries a {@link PlayableContent} {@code content} that was deep-copied
 * from the source deck at session-create time. All gameplay reads from
 * {@code content} (never re-fetches the live Deck) so authoring the source
 * deck mid-session can't desync clients.
 *
 * <p>Per-session overrides are direct mutation of {@code content} — flip
 * {@code content.format}, swap {@code content.settings}, etc. No shadow
 * fields, no null-fallback resolvers.
 *
 * <p>{@code deckId + deckVersion} together form the version pin recorded
 * for audit/replay — they answer "which authored revision was played?"
 * without trusting the snapshot copy. {@code Deck.version} is bumped on
 * every save in {@code DeckService}.
 *
 * <p>Unique indexes on {@code roomCode} and {@code inviteToken} support
 * the two join flows (code + link).
 */
package cephadex.brainflex.model.session;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import cephadex.brainflex.model.enums.ResponseMode;
import cephadex.brainflex.model.enums.RoundPhase;
import cephadex.brainflex.model.enums.SessionLifecycle;
import cephadex.brainflex.model.org.Team;
import cephadex.brainflex.model.shared.Auditable;
import cephadex.brainflex.model.shared.PlayableContent;
import lombok.Data;

@Data
@Document(collection = "interactive_sessions")
public class InteractiveSession extends Auditable {
    @Id
    private String id;

    @Indexed(unique = true)
    private String roomCode;

    @Indexed(unique = true)
    private String inviteToken;

    private SessionLifecycle status = SessionLifecycle.LOBBY;

    // The active phase within the current round. Most question kinds live in
    // SUBMIT only; Best-Answer-mode questions cycle SUBMIT → VOTE → REVEAL.
    private RoundPhase phase = RoundPhase.SUBMIT;

    private String hostUserId;

    // Version pin. `deckId` is the source deck; `deckVersion` is the value
    // of `Deck.version` at the moment this session was created.
    private String deckId;
    private int deckVersion;

    // Frozen, mutable copy of the source deck's PlayableContent. All
    // gameplay (broadcasting, scoring, review) reads from here — never
    // re-fetches the live Deck — so mid-session deck edits don't desync.
    // Per-session overrides (host flips the format, settings tweaks, the
    // start-game shuffle) are direct mutation of this copy.
    private PlayableContent content = new PlayableContent();

    private List<InteractiveSessionPlayer> players = new ArrayList<>();

    // Chunk 12 — team mode. The teamMode / autoBalanceTeams toggles live on
    // content.settings; teams is the live roster, empty unless team mode is on.
    private List<Team> teams = new ArrayList<>();

    private int currentRound = 0; // index into content.elements

    private Instant startedAt;
    private Instant endedAt;
    private Instant roundStartedAt; // timestamp current round began; speed-bonus reference

    // Chunk 13 — lobby + host display additions. host* fields are denormalised
    // so the lobby header doesn't need a User lookup on every refresh.
    // customRoomCode is the host-typed override — active roomCode falls back to
    // the auto-generated value when blank.
    private String customRoomCode;
    private String hostName;
    private String hostAvatarUrl;
    private int spectatorCount = 0;
    private Instant lobbyOpenedAt = Instant.now();

    // when a CSV/PDF report is exported
    // off the finished session; null until then.
    private String exportedReportUrl;

    // revealedElementIds: elementIds for which the host has manually clicked
    // "Reveal results" during an ON_CLICK round. Subsequent reveal calls
    // are no-ops (idempotent). Survives advanceRound so the review can
    // tell which rounds were instant vs. manual.
    // elementResponseModeOverrides: elementId → ResponseMode, applied by
    // submitAnswer before the element's authored mode. Cleared on round
    // advance only via natural lifecycle (no explicit clear step).
    private Set<String> revealedElementIds = new HashSet<>();
    private Map<String, ResponseMode> elementResponseModeOverrides = new HashMap<>();

    // Chunk 25 — host timer pause. The round countdown is server-scheduled, so
    // pausing means cancelling the pending timeout and remembering how much
    // time was left. timerPaused gates whether a countdown is frozen;
    // timerRemainingMillis is the millis left at the moment of pause (null while
    // running); timerPausedAt is the wall-clock pause instant (audit/debug,
    // mirroring the other Instant fields).
    private boolean timerPaused = false;
    private Long timerRemainingMillis;
    private Instant timerPausedAt;

    // Bumped on every timer cancel (pause / restart / end-submit finalize) so a
    // scheduled timeout that has already been handed to the executor no-ops
    // when it eventually fires. This is the *persisted* authority — it survives
    // a cache miss / backend restart, unlike the in-memory ScheduledFuture the
    // service also tracks for eager cancellation. Pairs with the existing
    // `currentRound != timedRound` guard in handleRoundTimeout to cover
    // pause/resume/restart that happen within the same round index.
    private int timerGeneration = 0;
}
