/**
 * Redux slice for active InteractiveSession state.
 *
 * All WebSocket session events (ROUND_START, ROUND_RESULT, SESSION_ENDED, lobby
 * updates, presence, answer-progress) are dispatched here so any component can
 * read the current state without prop drilling.
 *
 * Element types are polymorphic — see `types/elements.ts` for the discriminated
 * unions. Per-element answer payloads are stored locally as `myAnswer` until
 * the round completes; the server confirms scoring via roundResultReceived.
 */
import { createSlice, type PayloadAction } from "@reduxjs/toolkit";
import type {
  InteractiveSessionDto,
  InteractiveSessionChatMessageDto,
  PlayerPlacement,
  InteractiveSessionPlayerDto,
  Team,
} from "./BrainFlexApi";
import type { AnswerPayload, DeckElement } from "../types/elements";
import type {
  AnonymizedSubmission,
  BestAnswerOutcome,
  VotePhaseStartPayload,
  VoteProgressPayload,
} from "../types/bestAnswer";

export interface RoundStartPayload {
  round: number;
  totalRounds: number;
  element: DeckElement;
  startedAt: string;
}

export interface PlayerRoundResult {
  userId: string;
  userName: string;
  payload?: AnswerPayload | null;
  wasCorrect: boolean;
  pointsAwarded: number;
  totalScore: number;
}

export interface RoundResultPayload {
  round: number;
  element: DeckElement;       // un-redacted; reveals correct answer
  playerResults: PlayerRoundResult[];
  // Populated when the round was a Best Answer round (SUBMIT → VOTE → REVEAL).
  // Carries the de-anonymized vote tallies + winner ids + bonus awarded.
  bestAnswer?: BestAnswerOutcome | null;
  // Chunk 24 — chrome the renderer needs without an extra session lookup.
  // GAME → RoundResult overlay (leaderboard chrome).
  // PRESENTATION → RoundDataView (aggregated chart, no rankings).
  format?: "GAME" | "PRESENTATION";
}

export interface SessionEndedPayload {
  placements: PlayerPlacement[];
}

// ─── Chunk 24 — PRESENTATION end-of-session + host reveal/freeze ─────────────

export interface SessionSummaryRound {
  roundIndex: number;
  element: DeckElement;
  aggregatedPayloads: AnswerPayload[];
}

export interface SessionSummaryPayload {
  roundsPlayed: number;
  anyScoringEnabled: boolean;
  rounds: SessionSummaryRound[];
}

export interface ResponsesRevealedPayload {
  round: number;
  elementId: string;
}

export interface WsErrorPayload {
  operation: string;
  roomCode: string;
  status: number;
  message: string;
}

export interface AnswerProgressPayload {
  round: number;
  answeredUserIds: string[];
  totalPlayers: number;
}

export interface WordCloudUpdatePayload {
  round: number;
  elementId: string;
  counts: Record<string, number>;
}

export interface PresencePayload {
  userId: string;
  online: boolean;
}

/**
 * One live reaction burst the host animates in ReactionRain. The slice
 * keeps a short rolling window — components consume from the tail and the
 * window is trimmed in {@link reactionReceived} so memory doesn't grow
 * unbounded over a long game.
 */
export interface LiveReaction {
  id: string;
  emoji: string;
  userName?: string;
  /** Monotonic local timestamp (Date.now()) when the reaction was queued. */
  queuedAt: number;
}

export interface ReactionPayload {
  id: string;
  elementId?: string;
  userId?: string;
  userName?: string;
  guest?: boolean;
  emoji: string;
  offsetMs?: number;
  sentAt?: string;
}

export interface TeamUpdatePayload {
  teams: Team[];
  memberships: { userId: string; teamId: string }[];
}

/** Max in-flight live reactions kept in the slice. Older bursts drop off. */
const LIVE_REACTION_WINDOW = 40;
/** Max chat history retained client-side. Older messages drop off. */
const CHAT_HISTORY_WINDOW = 200;

interface InteractiveSessionState {
  roomCode: string | null;
  status: InteractiveSessionDto["status"] | null;
  players: InteractiveSessionPlayerDto[];
  currentElement: DeckElement | null;
  round: number;
  totalRounds: number;
  // The local player's submitted payload for this round; null until they answer.
  myAnswer: AnswerPayload | null;
  roundResult: RoundResultPayload | null;
  finalPlacements: PlayerPlacement[];
  roundStartedAt: string | null;
  wsError: WsErrorPayload | null;
  answeredThisRound: string[];
  offlineUserIds: string[];

  // ---- Best Answer phase ----
  // "SUBMIT" while players are submitting normally; "VOTE" once the server
  // broadcasts the anonymized submissions. REVEAL is implicit — when
  // roundResult arrives with bestAnswer set, render the tally on top of the
  // existing RoundResult overlay and reset phase to SUBMIT for the next round.
  phase: "SUBMIT" | "VOTE";
  voteSubmissions: AnonymizedSubmission[];
  votePhaseStartedAt: string | null;
  votePhaseSeconds: number;     // 0 = unlimited
  // The local player's voted-for submissionId during VOTE phase; null until they vote.
  myVote: string | null;
  // userIds who have already voted this round (for the "n of m voted" indicator).
  votedThisRound: string[];

  // Live word -> count map for the active Word Cloud round. Empty {} between
  // rounds and on every non-WordCloud round. Updated by `wordCloudUpdated`,
  // which the server emits on every submission and once on round complete.
  wordCloudCounts: Record<string, number>;

  // ---- Audience engagement (chunk 11) ----
  // Trimmed history of chat messages for this session. Initial load comes from
  // useListChatQuery; STOMP /chat broadcasts append (or patch in place when
  // the message is already present and the server is rebroadcasting a
  // moderation flip).
  chat: InteractiveSessionChatMessageDto[];
  // Rolling window of recent reaction bursts. ReactionRain reads this and
  // animates each new entry; the window is trimmed so a long game doesn't
  // pile up megabytes of payloads in the store.
  liveReactions: LiveReaction[];

  // ---- Teams (chunk 12) ----
  // Mirrors InteractiveSessionDto.teams; TeamUpdateMessage broadcasts patch
  // both this and the per-player teamId in place so the lobby + scoreboard
  // re-render without refetching the whole session.
  teams: Team[];
  teamMode: boolean;
  autoBalanceTeams: boolean;

  // ---- Chunk 24 — session chrome + host overlays ----
  // `format` is frozen on the InteractiveSession at create time. Mirrored
  // here so PlayPage / ResultsPage can switch shells without waiting for the
  // RTK Query `getInteractiveSession` cache to repopulate.
  format: "GAME" | "PRESENTATION";
  // elementIds the host has explicitly revealed (ON_CLICK reveal-now). The
  // backend ResponsesRevealedMessage tracks one-shot per element per session;
  // we keep a Set so the player + host UIs can flip from "waiting" to "shown"
  // without an extra fetch.
  revealedElementIds: string[];
  // elementId → "FROZEN" overlay. Host-only state: when the host clicks
  // Freeze, we mark the element here so the toggle reflects current state.
  // Player rejections come back as WsErrors from submitAnswer, so they don't
  // need to consult this map.
  frozenElementIds: string[];
  // PRESENTATION end-of-session aggregated payload. Populated from
  // SessionSummaryMessage on /summary; null until the host ends the session.
  sessionSummary: SessionSummaryPayload | null;
}

const initialState: InteractiveSessionState = {
  roomCode: null,
  status: null,
  players: [],
  currentElement: null,
  round: 0,
  totalRounds: 0,
  myAnswer: null,
  roundResult: null,
  finalPlacements: [],
  roundStartedAt: null,
  wsError: null,
  answeredThisRound: [],
  offlineUserIds: [],
  phase: "SUBMIT",
  voteSubmissions: [],
  votePhaseStartedAt: null,
  votePhaseSeconds: 0,
  myVote: null,
  votedThisRound: [],
  wordCloudCounts: {},
  chat: [],
  liveReactions: [],
  teams: [],
  teamMode: false,
  autoBalanceTeams: false,
  format: "GAME",
  revealedElementIds: [],
  frozenElementIds: [],
  sessionSummary: null,
};

export const interactiveSessionSlice = createSlice({
  name: "interactiveSession",
  initialState,
  reducers: {
    setSession(state, action: PayloadAction<InteractiveSessionDto>) {
      const s = action.payload;
      state.roomCode = s.roomCode ?? null;
      state.status = s.status ?? null;
      state.players = s.players ?? [];
      state.totalRounds = s.settings?.totalRounds ?? 0;
      state.round = s.currentRound ?? 0;
      state.teams = s.teams ?? [];
      state.teamMode = s.settings?.teamMode ?? false;
      state.autoBalanceTeams = s.settings?.autoBalanceTeams ?? false;
      // Chunk 24 — format is frozen on the session at create time. Default
      // GAME so legacy sessions that pre-date the column still render the
      // existing chrome instead of falling through to a blank PRESENTATION.
      state.format = s.format ?? "GAME";
      // Chunk 24 — host overlays are persisted on the session document so a
      // host reconnect/refresh rebuilds reveal + freeze state from the DTO
      // instead of waiting for the next broadcast. STOMP messages still keep
      // the slice in sync once we're live; this just gives us a correct
      // starting point.
      state.revealedElementIds = s.revealedElementIds ?? [];
      const overrides = s.elementResponseModeOverrides ?? {};
      state.frozenElementIds = Object.entries(overrides)
        .filter(([, mode]) => mode === "NOT_ACCEPTING_RESPONSES")
        .map(([elementId]) => elementId);
    },

    roundStarted(state, action: PayloadAction<RoundStartPayload>) {
      state.status = "IN_PROGRESS";
      state.round = action.payload.round;
      state.totalRounds = action.payload.totalRounds;
      state.currentElement = action.payload.element;
      state.roundStartedAt = action.payload.startedAt;
      state.myAnswer = null;
      state.roundResult = null;
      state.answeredThisRound = [];
      // Reset vote-phase state at the top of every round; the server will tell
      // us to enter VOTE phase if this round is a Best Answer round.
      state.phase = "SUBMIT";
      state.voteSubmissions = [];
      state.votePhaseStartedAt = null;
      state.votePhaseSeconds = 0;
      state.myVote = null;
      state.votedThisRound = [];
      state.wordCloudCounts = {};
    },

    votePhaseStarted(state, action: PayloadAction<VotePhaseStartPayload>) {
      // Stale broadcasts (e.g. server retried after round advance) are dropped.
      if (action.payload.round !== state.round) return;
      state.phase = "VOTE";
      state.voteSubmissions = action.payload.submissions;
      state.votePhaseStartedAt = action.payload.phaseStartedAt;
      state.votePhaseSeconds = action.payload.timePerVote;
      state.myVote = null;
      state.votedThisRound = [];
    },

    /** Local-only: record what the player voted for so we can disable input. */
    voteSubmittedLocally(state, action: PayloadAction<string>) {
      state.myVote = action.payload;
    },

    voteProgressReceived(state, action: PayloadAction<VoteProgressPayload>) {
      if (action.payload.round !== state.round) return;
      state.votedThisRound = action.payload.votedUserIds;
    },

    /** Local-only: record what the player submitted so we can disable inputs etc. */
    answerSubmittedLocally(state, action: PayloadAction<AnswerPayload>) {
      state.myAnswer = action.payload;
    },

    answerProgressReceived(state, action: PayloadAction<AnswerProgressPayload>) {
      if (action.payload.round !== state.round) return;
      state.answeredThisRound = action.payload.answeredUserIds;
    },

    wordCloudUpdated(state, action: PayloadAction<WordCloudUpdatePayload>) {
      // Stale broadcasts from a previous round are dropped.
      if (action.payload.round !== state.round) return;
      // Element id guards against an out-of-order broadcast landing after the
      // round advanced to a new element with the same round number.
      if (
        state.currentElement &&
        state.currentElement.id !== action.payload.elementId
      ) {
        return;
      }
      state.wordCloudCounts = action.payload.counts;
    },

    presenceUpdated(state, action: PayloadAction<PresencePayload>) {
      const { userId, online } = action.payload;
      if (online) {
        state.offlineUserIds = state.offlineUserIds.filter((id) => id !== userId);
      } else if (!state.offlineUserIds.includes(userId)) {
        state.offlineUserIds.push(userId);
      }
    },

    roundResultReceived(state, action: PayloadAction<RoundResultPayload>) {
      state.roundResult = action.payload;
      for (const pr of action.payload.playerResults) {
        const player = state.players.find((p) => p.userId === pr.userId);
        if (player) player.score = pr.totalScore;
      }
      // Best Answer round just revealed → drop the VOTE-phase scaffolding so
      // the picker UI unmounts. The reveal lives on roundResult.bestAnswer.
      state.phase = "SUBMIT";
      state.voteSubmissions = [];
      state.votePhaseStartedAt = null;
    },

    sessionEnded(state, action: PayloadAction<SessionEndedPayload>) {
      state.status = "FINISHED";
      state.finalPlacements = action.payload.placements;
      state.currentElement = null;
    },

    /**
     * PRESENTATION end-of-session payload. Mutually exclusive with
     * sessionEnded on the wire: a GAME session emits placements,
     * PRESENTATION emits aggregated rounds. Clients subscribe to both topics
     * and only one fires per session.
     */
    sessionSummaryReceived(
      state,
      action: PayloadAction<SessionSummaryPayload>,
    ) {
      state.status = "FINISHED";
      state.sessionSummary = action.payload;
      state.currentElement = null;
    },

    /**
     * Host clicked Reveal on an ON_CLICK round. Track the elementId so the
     * player + host UIs can flip from "waiting for host" to "showing
     * responses." Server is idempotent — duplicates are no-ops here too.
     */
    responsesRevealed(state, action: PayloadAction<ResponsesRevealedPayload>) {
      const { elementId } = action.payload;
      if (!state.revealedElementIds.includes(elementId)) {
        state.revealedElementIds.push(elementId);
      }
    },

    /**
     * Host-local mirror of freeze state. The server stores the override map
     * on the session but doesn't expose it via the DTO, so the host tracks
     * the toggle here. Player-side, attempting to submit while frozen comes
     * back as a WsError from /answer rather than being read off this map.
     */
    freezeStateChanged(
      state,
      action: PayloadAction<{ elementId: string; frozen: boolean }>,
    ) {
      const { elementId, frozen } = action.payload;
      const idx = state.frozenElementIds.indexOf(elementId);
      if (frozen && idx < 0) state.frozenElementIds.push(elementId);
      if (!frozen && idx >= 0) state.frozenElementIds.splice(idx, 1);
    },

    wsErrorReceived(state, action: PayloadAction<WsErrorPayload>) {
      state.wsError = action.payload;
    },

    clearWsError(state) {
      state.wsError = null;
    },

    /**
     * Seed chat history on PlayPage/Lobby mount. Replaces the current
     * client-side buffer; trims to the retention window so a host hopping
     * between sessions doesn't accumulate stale rows.
     */
    chatHistoryLoaded(
      state,
      action: PayloadAction<InteractiveSessionChatMessageDto[]>,
    ) {
      const sorted = [...action.payload].sort((a, b) => {
        const ta = a.sentAt ? new Date(a.sentAt).getTime() : 0;
        const tb = b.sentAt ? new Date(b.sentAt).getTime() : 0;
        return ta - tb;
      });
      state.chat = sorted.slice(-CHAT_HISTORY_WINDOW);
    },

    /**
     * STOMP /chat broadcast. Used for both new sends AND moderation flips —
     * the server rebroadcasts the same DTO with moderated=true when the host
     * hides a message. We dedupe on id so the optimistic send (from the
     * apiEnhancements onQueryStarted) doesn't render twice when the broadcast
     * arrives.
     */
    chatMessageReceived(
      state,
      action: PayloadAction<InteractiveSessionChatMessageDto>,
    ) {
      const msg = action.payload;
      if (!msg.id) {
        state.chat.push(msg);
      } else {
        const idx = state.chat.findIndex((m) => m.id === msg.id);
        if (idx >= 0) state.chat[idx] = msg;
        else state.chat.push(msg);
      }
      if (state.chat.length > CHAT_HISTORY_WINDOW) {
        state.chat = state.chat.slice(-CHAT_HISTORY_WINDOW);
      }
    },

    /**
     * STOMP /reaction burst. Appends to the rolling window — ReactionRain
     * subscribes via useAppSelector and animates each new entry. Older entries
     * fall off when the window is exceeded; the host view itself drops the
     * DOM nodes when the CSS animation completes.
     */
    reactionReceived(state, action: PayloadAction<ReactionPayload>) {
      const p = action.payload;
      state.liveReactions.push({
        id: p.id,
        emoji: p.emoji,
        userName: p.userName,
        queuedAt: Date.now(),
      });
      if (state.liveReactions.length > LIVE_REACTION_WINDOW) {
        state.liveReactions = state.liveReactions.slice(-LIVE_REACTION_WINDOW);
      }
    },

    /**
     * Drops a single live reaction once ReactionRain finishes animating it.
     * Keeps the slice from holding onto already-rendered entries.
     */
    reactionConsumed(state, action: PayloadAction<string>) {
      state.liveReactions = state.liveReactions.filter(
        (r) => r.id !== action.payload,
      );
    },

    /**
     * STOMP /teams broadcast. Replaces the team list outright and patches the
     * teamId on every affected player in place; clients reconcile from this
     * snapshot rather than merging deltas.
     */
    teamUpdateReceived(state, action: PayloadAction<TeamUpdatePayload>) {
      state.teams = action.payload.teams;
      const byUserId = new Map(
        action.payload.memberships.map((m) => [m.userId, m.teamId]),
      );
      for (const player of state.players) {
        if (player.userId && byUserId.has(player.userId)) {
          player.teamId = byUserId.get(player.userId) ?? undefined;
        }
      }
    },

    resetSession() {
      return initialState;
    },
  },
});

export const {
  setSession,
  roundStarted,
  answerSubmittedLocally,
  answerProgressReceived,
  presenceUpdated,
  roundResultReceived,
  sessionEnded,
  wsErrorReceived,
  clearWsError,
  resetSession,
  votePhaseStarted,
  voteSubmittedLocally,
  voteProgressReceived,
  wordCloudUpdated,
  chatHistoryLoaded,
  chatMessageReceived,
  reactionReceived,
  reactionConsumed,
  teamUpdateReceived,
  sessionSummaryReceived,
  responsesRevealed,
  freezeStateChanged,
} = interactiveSessionSlice.actions;

export default interactiveSessionSlice.reducer;
