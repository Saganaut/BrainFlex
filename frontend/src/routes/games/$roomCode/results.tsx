/**
 * Results route (/games/$roomCode/results).
 * Shows the final placement table after a game ends. Uses the GAME_OVER
 * payload already in Redux state; falls back to GET /api/games/{roomCode}/results
 * if the user arrived here directly (e.g. after a page refresh).
 */
import { useEffect } from "react";
import { createFileRoute } from "@tanstack/react-router";
import { useAppDispatch } from "../../../store/hooks";
import { setSession } from "../../../store/gameSlice";
import {
  useGetSessionQuery,
  useGetResultsQuery,
} from "../../../store/BrainFlexApi";
import { useGameSession } from "../../../hooks/useGameSession";
import { useCurrentUser } from "../../../hooks/useCurrentUser";
import { GameOver } from "../../../components/Games/GameOver";

export const Route = createFileRoute("/games/$roomCode/results")({
  component: ResultsPage,
});

function ResultsPage() {
  const { roomCode } = Route.useParams();
  const dispatch = useAppDispatch();
  const { user } = useCurrentUser();
  const game = useGameSession();

  const { data: session } = useGetSessionQuery({ roomCode });
  const { data: gameResult } = useGetResultsQuery({ roomCode });

  useEffect(() => {
    if (session) dispatch(setSession(session));
  }, [session, dispatch]);

  const placements =
    game.finalPlacements.length > 0
      ? game.finalPlacements
      : (gameResult?.placements ?? []);

  if (placements.length === 0) {
    return (
      <div style={{ padding: "2rem", textAlign: "center" }}>
        <p>Loading results…</p>
      </div>
    );
  }

  return <GameOver placements={placements} currentUserId={user?.id} />;
}
