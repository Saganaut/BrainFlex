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

export interface WordCloudUpdatePayload {
  round: number;
  elementId: string;
  counts: Record<string, number>;
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
  phase: "SUBMIT",
  voteSubmissions: [],
  votePhaseStartedAt: null,
  votePhaseSeconds: 0,
  myVote: null,
  votedThisRound: [],
  wordCloudCounts: {},
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
  votePhaseStarted,
  voteSubmittedLocally,
  voteProgressReceived,
  wordCloudUpdated,
} = gameSlice.actions;

export default gameSlice.reducer;
