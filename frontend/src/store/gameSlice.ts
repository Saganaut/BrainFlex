/**
 * Redux slice for active showcase state.
 *
 * All WebSocket showcase events (ROUND_START, ROUND_RESULT, GAME_OVER, lobby
 * updates, presence, answer-progress) are dispatched here so any component can
 * read the current state without prop drilling.
 *
 * Element types are polymorphic — see `types/elements.ts` for the discriminated
 * unions. Per-element answer payloads are stored locally as `myAnswer` until
 * the round completes; the server confirms scoring via roundResultReceived.
 */
import { createSlice, type PayloadAction } from "@reduxjs/toolkit";
import type {
  ShowcaseDto,
  PlayerPlacement,
  ShowcasePlayerDto,
} from "./BrainFlexApi";
import type { AnswerPayload, DeckElement } from "../types/elements";

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
}

export interface GameOverPayload {
  placements: PlayerPlacement[];
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

export interface PresencePayload {
  userId: string;
  online: boolean;
}

interface GameState {
  roomCode: string | null;
  status: ShowcaseDto["status"] | null;
  players: ShowcasePlayerDto[];
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
}

const initialState: GameState = {
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
};

export const gameSlice = createSlice({
  name: "game",
  initialState,
  reducers: {
    setSession(state, action: PayloadAction<ShowcaseDto>) {
      const s = action.payload;
      state.roomCode = s.roomCode ?? null;
      state.status = s.status ?? null;
      state.players = s.players ?? [];
      state.totalRounds = s.settings?.totalRounds ?? 0;
      state.round = s.currentRound ?? 0;
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
    },

    /** Local-only: record what the player submitted so we can disable inputs etc. */
    answerSubmittedLocally(state, action: PayloadAction<AnswerPayload>) {
      state.myAnswer = action.payload;
    },

    answerProgressReceived(state, action: PayloadAction<AnswerProgressPayload>) {
      if (action.payload.round !== state.round) return;
      state.answeredThisRound = action.payload.answeredUserIds;
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
    },

    gameOver(state, action: PayloadAction<GameOverPayload>) {
      state.status = "FINISHED";
      state.finalPlacements = action.payload.placements;
      state.currentElement = null;
    },

    wsErrorReceived(state, action: PayloadAction<WsErrorPayload>) {
      state.wsError = action.payload;
    },

    clearWsError(state) {
      state.wsError = null;
    },

    resetGame() {
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
  gameOver,
  wsErrorReceived,
  clearWsError,
  resetGame,
} = gameSlice.actions;

export default gameSlice.reducer;
