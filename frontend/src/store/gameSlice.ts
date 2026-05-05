/**
 * Redux slice for active game session state.
 * All WebSocket game events (ROUND_START, ROUND_RESULT, GAME_OVER, lobby updates)
 * are dispatched here so any component can read the current game state without
 * passing props through the route tree.
 */
import { createSlice, type PayloadAction } from "@reduxjs/toolkit";
import type { GameSessionDto, PlayerPlacement, SessionPlayerDto } from "./BrainFlexApi";

// Types for WebSocket broadcast payloads (not in the REST API client)
export type QuestionData = {
  id: string;
  questionText: string;
  options: string[];
  pointValue: number;
  timeLimit: number;
  type: string;
  imageUrl?: string;
};

export type RoundStartPayload = {
  round: number;
  totalRounds: number;
  question: QuestionData;
  startedAt: string;
};

export type PlayerRoundResult = {
  userId: string;
  userName: string;
  selectedOption: number;
  wasCorrect: boolean;
  pointsAwarded: number;
  totalScore: number;
};

export type RoundResultPayload = {
  round: number;
  correctAnswer: number;
  correctAnswerText: string;
  playerResults: PlayerRoundResult[];
};

export type GameOverPayload = {
  placements: PlayerPlacement[];
};

interface GameState {
  roomCode: string | null;
  status: GameSessionDto["status"] | null;
  players: SessionPlayerDto[];
  currentQuestion: QuestionData | null;
  round: number;
  totalRounds: number;
  myAnswer: number | null; // option index the local user selected; null = not yet answered
  roundResult: RoundResultPayload | null;
  finalPlacements: PlayerPlacement[];
  roundStartedAt: string | null; // ISO timestamp from server for countdown timer
}

const initialState: GameState = {
  roomCode: null,
  status: null,
  players: [],
  currentQuestion: null,
  round: 0,
  totalRounds: 0,
  myAnswer: null,
  roundResult: null,
  finalPlacements: [],
  roundStartedAt: null,
};

export const gameSlice = createSlice({
  name: "game",
  initialState,
  reducers: {
    /** Syncs session metadata from a REST response or a lobby WebSocket broadcast. */
    setSession(state, action: PayloadAction<GameSessionDto>) {
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
      state.roundResult = null;
    },

    /** Records the option index the local user clicked. */
    answerSelected(state, action: PayloadAction<number>) {
      state.myAnswer = action.payload;
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
  roundResultReceived,
  gameOver,
  resetGame,
} = gameSlice.actions;

export default gameSlice.reducer;
