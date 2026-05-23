// Post-interactiveSession results screen — switches between the final standings (GameOver)
// and the per-round review panel via a top-of-page toggle. Chunk 24:
// PRESENTATION-format sessions short-circuit the standings/review tabs in favor
// of `SessionSummary`, which renders aggregated room responses with no
// leaderboard.
import { getRouteApi } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { GameOver } from "../../components/Games/GameOver/GameOver";
import { ReviewPanel } from "../../components/Games/ReviewPanel/ReviewPanel";
import { SessionSummary } from "../../components/Games/SessionSummary/SessionSummary";
import { useInteractiveSession } from "../../hooks/useInteractiveSession";
import {
  useGetInteractiveSessionQuery,
  useGetResultsQuery,
  useGetReviewQuery,
} from "../../store/BrainFlexApi";
import { setSession } from "../../store/interactiveSessionSlice";
import { useAppDispatch, useAppSelector } from "../../store/hooks";
import { Btn } from "@/components/Common/Buttons/Btn";
import styles from "./Results.module.css";

const routeApi = getRouteApi("/games/$roomCode/results");

type View = "standings" | "review";

const ResultsPage = () => {
  const { roomCode } = routeApi.useParams();
  const dispatch = useAppDispatch();
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

  // Session-scoped playerId of the caller — latched in the slice on the
  // initial REST fetch above. Used to highlight the viewer's own placement.
  const viewerPlayerId = useAppSelector(
    (s) => s.interactiveSession.viewerPlayerId,
  );

  // Chunk 24 — PRESENTATION sessions never have placements; the wire payload
  // is `SessionSummaryMessage` on /summary instead of `/ended`. The slice's
  // `format` defaults to GAME if no session has loaded yet, so this is the
  // authoritative read once `setSession` has run.
  const isPresentation = game.format === "PRESENTATION";

  if (isPresentation) {
    if (!game.sessionSummary) {
      return (
        <div className={styles.loading}>
          <p>Aggregating responses…</p>
        </div>
      );
    }
    return (
      <div className={styles.page}>
        <SessionSummary summary={game.sessionSummary} roomCode={roomCode} />
      </div>
    );
  }

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
          currentPlayerId={viewerPlayerId ?? undefined}
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
