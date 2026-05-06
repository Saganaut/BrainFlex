import { getRouteApi } from "@tanstack/react-router";
import { useEffect } from "react";
import { GameOver } from "../../components/Games/GameOver/GameOver";
import { useCurrentUser } from "../../hooks/useCurrentUser";
import { useGameSession } from "../../hooks/useGameSession";
import {
  useGetSessionQuery,
  useGetResultsQuery,
} from "../../store/BrainFlexApi";
import { setSession } from "../../store/gameSlice";
import { useAppDispatch } from "../../store/hooks";

const routeApi = getRouteApi("/games/$roomCode/results");

const ResultsPage = () => {
  const { roomCode } = routeApi.useParams();
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
};

export { ResultsPage };
