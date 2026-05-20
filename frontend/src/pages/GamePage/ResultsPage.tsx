// Post-interactiveSession results screen — switches between the final standings (GameOver)
// and the per-round review panel via a top-of-page toggle.
import { getRouteApi } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { GameOver } from "../../components/Games/GameOver/GameOver";
import { ReviewPanel } from "../../components/Games/ReviewPanel/ReviewPanel";
import { useCurrentUser } from "../../hooks/useCurrentUser";
import { useInteractiveSession } from "../../hooks/useInteractiveSession";
import {
  useGetInteractiveSessionQuery,
  useGetResultsQuery,
  useGetReviewQuery,
} from "../../store/BrainFlexApi";
import { setSession } from "../../store/interactiveSessionSlice";
import { useAppDispatch } from "../../store/hooks";
import { Btn } from "@/components/Common/Buttons/Btn";
import styles from "./Results.module.css";

const routeApi = getRouteApi("/games/$roomCode/results");

type View = "standings" | "review";

const ResultsPage = () => {
  const { roomCode } = routeApi.useParams();
  const dispatch = useAppDispatch();
  const userState = useCurrentUser();
  const game = useInteractiveSession();

  const { data: session } = useGetInteractiveSessionQuery({ roomCode });
  const { data: gameResult } = useGetResultsQuery({ roomCode });
  // Fetch the review lazily: only the host typically opens it, but for non-Pulse
  // interactiveSessions it's useful for everyone. Suspend nothing — just show a skeleton.
  const { data: review, isLoading: reviewLoading } = useGetReviewQuery({
    roomCode,
  });

  const [view, setView] = useState<View>("standings");

  useEffect(() => {
    if (session) dispatch(setSession(session));
  }, [session, dispatch]);

  const userId =
    userState.state === "registered" || userState.state === "guest"
      ? userState.user.id
      : undefined;

  const placements =
    game.finalPlacements.length > 0
      ? game.finalPlacements
      : (gameResult?.placements ?? []);

  if (placements.length === 0) {
    return (
      <div className={styles.loading}>
        <p>Loading results…</p>
      </div>
    );
  }

  return (
    <div className={styles.page}>
      <div className={styles.tabs} role='tablist' aria-label='Results view'>
        <Btn
          type='button'
          role='tab'
          aria-selected={view === "standings"}
          onClick={() => {
            setView("standings");
          }}>
          Final standings
        </Btn>
        <Btn
          type='button'
          role='tab'
          aria-selected={view === "review"}
          onClick={() => {
            setView("review");
          }}>
          Review questions
        </Btn>
      </div>

      {view === "standings" ? (
        <GameOver
          placements={placements}
          currentUserId={userId}
          teams={session?.teams ?? []}
        />
      ) : reviewLoading ? (
        <p className={styles.loadingMsg}>Loading review…</p>
      ) : review ? (
        <ReviewPanel review={review} />
      ) : (
        <p className={styles.loadingMsg}>Review not available.</p>
      )}
    </div>
  );
};

export { ResultsPage };
