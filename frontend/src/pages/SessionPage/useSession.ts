// Live read model for the Gen-2 session page. A pure reader (no side effects):
// the SessionConnectionProvider owns the STOMP subscription + REST seed, so this
// just combines the two sources every consumer needs into one session view.
//
//   - The REST snapshot (getInteractiveSession, a shared/deduped cache) supplies
//     the frozen content: deckSnapshot, settings, host identity, room code.
//   - The interactiveSession Redux slice supplies everything that changes during
//     the session — status, round, phase, players, reveals — kept current by the
//     STOMP topics. This is why the board flips from lobby to question to results
//     without any navigation: status changes here, the board re-derives its stage.
//
// The slice is seeded from the same snapshot, so once seeded it is the source of
// truth for the live fields and the snapshot only backs the static ones.
import type {
  DeckResponse,
  InteractiveSessionResponse,
} from "@/store/BrainFlexApi";
import {
  useGetDeckQuery,
  useGetInteractiveSessionQuery,
} from "@/store/BrainFlexApi";
import { useInteractiveSession } from "@/hooks/useInteractiveSession";
import { getRouteApi } from "@tanstack/react-router";

const routeApi = getRouteApi("/sessions/$sessionId/");

interface useSessionResponse {
  sessionId: string;
  interactiveSession: InteractiveSessionResponse;
  // Resolved from the session's deckId for the header title. Optional: a
  // non-host participant may not be able to read the deck, and it is briefly
  // undefined while loading.
  currentDeck?: DeckResponse;
}

type SliceState = ReturnType<typeof useInteractiveSession>;

const mergeSessionView = (
  roomCode: string,
  snapshot: InteractiveSessionResponse | undefined,
  live: SliceState,
): InteractiveSessionResponse => {
  // The slice is "seeded" once setSession has run for this room; before that the
  // REST snapshot is authoritative for live fields too.
  const seeded = live.status !== null && live.roomCode === roomCode;

  if (!snapshot) {
    throw Error("No snapshot provided");
  }

  if (!seeded) return snapshot;

  return {
    ...snapshot,
    status: live.status ?? snapshot.status,
    phase: live.phase,
    currentRound: live.round,
    totalRounds: live.totalRounds || snapshot.totalRounds,
    players: live.players,
    teams: live.teams,
    revealedElementIds: live.revealedElementIds,
    viewerPlayerId: live.viewerPlayerId ?? snapshot.viewerPlayerId,
    timerPaused: live.timerPaused,
    timerRemainingMillis: live.timerRemainingMillis ?? undefined,
  };
};

const useSession = (): useSessionResponse => {
  // The `$sessionId` route param carries the room code (the join code).
  const { sessionId: roomCode } = routeApi.useParams();
  const { data: snapshot } = useGetInteractiveSessionQuery({ roomCode });
  const live = useInteractiveSession();
  const { data: currentDeck } = useGetDeckQuery(
    { id: snapshot?.deckId ?? "" },
    { skip: !snapshot?.deckId },
  );

  return {
    sessionId: roomCode,
    interactiveSession: mergeSessionView(roomCode, snapshot, live),
    currentDeck,
  };
};

export { useSession };
