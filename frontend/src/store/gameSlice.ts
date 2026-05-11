/**
 * Redux slice for active game session state.
 * All WebSocket game events (ROUND_START, ROUND_RESULT, GAME_OVER, lobby updates)
 * are dispatched here so any component can read the current game state without
 * passing props through the route tree.
 */
import { createSlice, type PayloadAction } from "@reduxjs/toolkit";
import type {
  ShowcaseDto,
  PlayerPlacement,
  ShowcasePlayerDto,
} from "./BrainFlexApi";

// Types for WebSocket broadcast payloads (not in the REST API client)
export type QuestionKind = "MULTIPLE_CHOICE" | "IMAGE_CHOICE" | "TEXT_INPUT";

export interface QuestionData {
  id: string;
  questionText: string;
  // MCQ only — server omits this field entirely for TEXT_INPUT rounds.
  options?: string[];
  pointValue: number;
  timeLimit: number;
  type: QuestionKind;
  imageUrl?: string;
}

export interface RoundStartPayload {
  round: number;
  totalRounds: number;
  question: QuestionData;
  startedAt: string;
}

export interface PlayerRoundResult {
  userId: string;
  userName: string;
  selectedOption: number;     // MCQ: -1 = timed out / not applicable
  textAnswer?: string | null; // TEXT_INPUT only
  wasCorrect: boolean;
  pointsAwarded: number;
  totalScore: number;
}

export interface RoundResultPayload {
  round: number;
  // MCQ: option index of the right answer. TEXT_INPUT: -1.
  correctAnswer: number;
  // MCQ: the right option's label. TEXT_INPUT: the canonical correct text.
  correctAnswerText: string;
  playerResults: PlayerRoundResult[];
}

export interface GameOverPayload {
  placements: PlayerPlacement[];
}

export interface WsErrorPayload {
  operation: string;  // e.g. "start", "answer", "nextRound", "leave"
  roomCode: string;
  status: number;     // 4xx user error, 5xx infrastructure
  message: string;
}

export interface AnswerProgressPayload {
  round: number;
  answeredUserIds: string[];
  totalPlayers: number;
}

export interface PresencePayload {
  userId: string;
  online: boolean;
}

interface GameState {
  roomCode: string | null;
  status: ShowcaseDto["status"] | null;
  players: ShowcasePlayerDto[];
  currentQuestion: QuestionData | null;
  round: number;
  totalRounds: number;
  // MCQ: option index the local user selected; null = not yet answered.
  myAnswer: number | null;
  // TEXT_INPUT: text the local user submitted; null = not yet answered.
  myTextAnswer: string | null;
  roundResult: RoundResultPayload | null;
  finalPlacements: PlayerPlacement[];
  roundStartedAt: string | null; // ISO timestamp from server for countdown timer
  // Most recent error from a WebSocket message handler (e.g. failed Start).
  // Cleared on dismissal or when the user successfully completes the same operation.
  wsError: WsErrorPayload | null;
  // userIds of players who have submitted an answer for the CURRENT round. Reset on
  // roundStarted; kept in sync via /topic/showcase/{code}/answered.
  answeredThisRound: string[];
  // userIds of players whose WebSocket session is currently disconnected. Default
  // assumption is online; entries are added on /topic/presence false events.
  offlineUserIds: string[];
}

const initialState: GameState = {
  roomCode: null,
  status: null,
  players: [],
  currentQuestion: null,
  round: 0,
  totalRounds: 0,
  myAnswer: null,
  myTextAnswer: null,
  roundResult: null,
  finalPlacements: [],
  roundStartedAt: null,
  wsError: null,
  answeredThisRound: [],
  offlineUserIds: [],
};

export const gameSlice = createSlice({
  name: "game",
  initialState,
  reducers: {
    /** Syncs session metadata from a REST response or a lobby WebSocket broadcast. */
    setSession(state, action: PayloadAction<ShowcaseDto>) {
      const s = action.payload;
      state.roomCode = s.roomCode ?? null;
      state.status = s.status ?? null;
      state.players = s.players ?? [];
      state.totalRounds = s.settings?.totalRounds ?? 0;
      state.round = s.currentRound ?? 0;
    },

    /** A new round has started; stores the question and clears the previous answer/result. */
    roundStarted(state, action: PayloadAction<RoundStartPayload>) {
      state.status = "IN_PROGRESS";
      state.round = action.payload.round;
      state.totalRounds = action.payload.totalRounds;
      state.currentQuestion = action.payload.question;
      state.roundStartedAt = action.payload.startedAt;
      state.myAnswer = null;
      state.myTextAnswer = null;
      state.roundResult = null;
      state.answeredThisRound = [];
    },

    /** Live "who has answered" update for the current round. */
    answerProgressReceived(state, action: PayloadAction<AnswerProgressPayload>) {
      // Stale broadcasts from a previous round can arrive after roundStarted; ignore them.
      if (action.payload.round !== state.round) return;
      state.answeredThisRound = action.payload.answeredUserIds;
    },

    /** Presence transition for a single user (from /topic/presence). */
    presenceUpdated(state, action: PayloadAction<PresencePayload>) {
      const { userId, online } = action.payload;
      if (online) {
        state.offlineUserIds = state.offlineUserIds.filter((id) => id !== userId);
      } else if (!state.offlineUserIds.includes(userId)) {
        state.offlineUserIds.push(userId);
      }
    },

    /** Records the option index the local user clicked (MCQ rounds). */
    answerSelected(state, action: PayloadAction<number>) {
      state.myAnswer = action.payload;
    },

    /** Records the free-text answer the local user submitted (TEXT_INPUT rounds). */
    textAnswerSubmitted(state, action: PayloadAction<string>) {
      state.myTextAnswer = action.payload;
    },

    /** Stores the round result so the result overlay can be shown. */
    roundResultReceived(state, action: PayloadAction<RoundResultPayload>) {
      state.roundResult = action.payload;
      // Update live player scores from server's authoritative totals
      for (const pr of action.payload.playerResults) {
        const player = state.players.find((p) => p.userId === pr.userId);
        if (player) player.score = pr.totalScore;
      }
    },

    /** Game has ended; stores final placements and flips status to FINISHED. */
    gameOver(state, action: PayloadAction<GameOverPayload>) {
      state.status = "FINISHED";
      state.finalPlacements = action.payload.placements;
      state.currentQuestion = null;
    },

    /** Stores a per-user error broadcast from /user/queue/errors. */
    wsErrorReceived(state, action: PayloadAction<WsErrorPayload>) {
      state.wsError = action.payload;
    },

    /** Clears the displayed WebSocket error (user dismissed or moved on). */
    clearWsError(state) {
      state.wsError = null;
    },

    /** Clears all game state when the user leaves or starts a new session. */
    resetGame() {
      return initialState;
    },
  },
});

export const {
  setSession,
  roundStarted,
  answerSelected,
  textAnswerSubmitted,
  answerProgressReceived,
  presenceUpdated,
  roundResultReceived,
  gameOver,
  wsErrorReceived,
  clearWsError,
  resetGame,
} = gameSlice.actions;

export default gameSlice.reducer;
