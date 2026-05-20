// Live interactiveSession round screen. Drives polymorphic element rendering via
// ElementRenderer, the host controls (boot / end interactiveSession), the scoreboard,
// the round-result overlay, and the post-round → next-round timer chrome.
import { getRouteApi, useNavigate } from "@tanstack/react-router";
import { useState, useEffect } from "react";
import { ElementRenderer } from "../../components/Games/ElementRenderer/ElementRenderer";
import { QuestionCard } from "../../components/Games/QuestionCard/QuestionCard";
import { RoundResult } from "../../components/Games/RoundResult/RoundResult";
import { ScoreBoard } from "../../components/Games/ScoreBoard/ScoreBoard";
import { VotePanel } from "../../components/Games/VotePanel/VotePanel";
import { WsErrorBanner } from "../../components/Games/WsErrorBanner/WsErrorBanner";
import { useCurrentUser } from "../../hooks/useCurrentUser";
import { useInteractiveSession } from "../../hooks/useInteractiveSession";
import { useInteractiveSessionWebSocket } from "../../hooks/useInteractiveSessionWebSocket";
import { Btn } from "@/components/Common/Buttons/Btn";
import { useConfirm } from "@/components/Common/ConfirmDialog/useConfirm";
import { useGetInteractiveSessionQuery } from "../../store/BrainFlexApi";
import { resolveInteractiveSessionBackground } from "../../utils/deckImages";
import { largestUrl } from "@/utils/image";
import {
  setSession,
  answerSubmittedLocally,
  voteSubmittedLocally,
} from "../../store/interactiveSessionSlice";
import { useAppDispatch } from "../../store/hooks";
import type { AnswerPayload } from "../../types/elements";
import styles from "./Game.module.css";

const routeApi = getRouteApi("/games/$roomCode/play");

const PlayPage = () => {
  const { roomCode } = routeApi.useParams();
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const userState = useCurrentUser();
  const game = useInteractiveSession();
  const { sendAnswer, sendVote, sendNextRound, sendBoot, sendEndInteractiveSession } =
    useInteractiveSessionWebSocket(roomCode);
  const { data: session } = useGetInteractiveSessionQuery({ roomCode });
  const [timeRemaining, setTimeRemaining] = useState(0);
  const confirm = useConfirm();

  const userId =
    userState.state === "registered" || userState.state === "guest"
      ? userState.user.id
      : undefined;

  const isHost = !!userId && session?.hostUserId === userId;
  const isTurnBased = session?.settings?.mode === "TURN_BASED";
  // Host disables the question timer by setting timePerQuestion = 0.
  // Per-element displaySeconds always overrides on the server; the frontend
  // here just respects "is there any countdown?" for the QuestionCard chrome.
  const interactiveSessionUnlimited = (session?.settings?.timePerQuestion ?? 1) === 0;
  const hideScoresDuringPlay =
    session?.settings?.showScoresImmediately === false;

  useEffect(() => {
    if (session) dispatch(setSession(session));
  }, [session, dispatch]);

  useEffect(() => {
    if (game.status === "FINISHED") {
      void navigate({
        to: "/games/$roomCode/results",
        params: { roomCode },
      });
    }
  }, [game.status, navigate, roomCode]);

  // Self-boot detection — if the host removes us from the player list, go home.
  useEffect(() => {
    if (!userId || game.players.length === 0) return;
    if (game.status === "FINISHED" || game.status === "CANCELLED") return;
    const stillInGame = game.players.some((p) => p.userId === userId);
    if (!stillInGame) {
      void navigate({ to: "/" });
    }
  }, [userId, game.players, game.status, navigate]);

  const handleBoot = async (targetUserId: string) => {
    const ok = await confirm({
      title: "Remove player",
      message: "Remove this player from the interactiveSession?",
      confirmLabel: "Remove",
      variant: "danger",
    });
    if (!ok) return;
    sendBoot(targetUserId);
  };

  const handleEndInteractiveSession = async () => {
    const ok = await confirm({
      title: "End interactiveSession",
      message: "End the interactiveSession now? Scores so far will be final.",
      confirmLabel: "End interactiveSession",
      variant: "danger",
    });
    if (!ok) return;
    sendEndInteractiveSession();
  };

  // Per-phase timer countdown.
  //   SUBMIT phase — count down from roundStartedAt using the effective
  //                  per-element / per-interactiveSession question duration.
  //   VOTE phase   — count down from votePhaseStartedAt using votePhaseSeconds.
  //   roundResult set — pause; the overlay handles the post-round timing.
  useEffect(() => {
    if (!game.currentElement || game.roundResult) return;

    let totalSeconds: number;
    let start: number;
    if (game.phase === "VOTE" && game.votePhaseStartedAt) {
      if (game.votePhaseSeconds <= 0) return; // unlimited vote phase
      totalSeconds = game.votePhaseSeconds;
      start = new Date(game.votePhaseStartedAt).getTime();
    } else {
      if (!game.roundStartedAt) return;
      const isSlide = game.currentElement.kind === "Slide";
      const elementSeconds = game.currentElement.displaySeconds ?? 0;
      const effective = elementSeconds > 0
        ? elementSeconds
        : interactiveSessionUnlimited ? 0 : (session?.settings?.timePerQuestion ?? 0);
      if (effective <= 0 && !isSlide) return;
      totalSeconds = effective > 0 ? effective : 8; // slide fallback
      start = new Date(game.roundStartedAt).getTime();
    }

    const tick = () => {
      // eslint-disable-next-line react-x/set-state-in-effect
      setTimeRemaining(
        Math.max(0, Math.ceil(totalSeconds - (Date.now() - start) / 1000)),
      );
    };
    tick();
    const id = setInterval(tick, 250);
    return () => {
      clearInterval(id);
    };
  }, [
    game.currentElement,
    game.roundStartedAt,
    game.roundResult,
    game.phase,
    game.votePhaseStartedAt,
    game.votePhaseSeconds,
    interactiveSessionUnlimited,
    session,
  ]);

  const handleAnswer = (payload: AnswerPayload) => {
    if (game.myAnswer !== null || !game.currentElement) return;
    dispatch(answerSubmittedLocally(payload));
    sendAnswer(game.currentElement.id ?? "", payload);
  };

  const handleVote = (submissionId: string) => {
    if (game.myVote !== null || !game.currentElement) return;
    dispatch(voteSubmittedLocally(submissionId));
    sendVote(game.currentElement.id ?? "", submissionId);
  };

  const backgroundUrl = resolveInteractiveSessionBackground(
    session?.deckBackgroundImageUrl,
    session?.deckId,
  );
  const bgStyle: React.CSSProperties = {
    "--interactive-session-bg": `url(${backgroundUrl})`,
  } as React.CSSProperties;

  if (!game.currentElement) {
    return (
      <div className={styles.waiting} style={bgStyle}>
        <WsErrorBanner />
        <p className={styles.waitingMsg}>Waiting for the first element…</p>
        <ScoreBoard
          players={game.players}
          currentUserId={userId}
          hideScores={hideScoresDuringPlay}
          offlineUserIds={game.offlineUserIds}
          isHost={isHost}
          onBootPlayer={(id) => {
            void handleBoot(id);
          }}
        />
      </div>
    );
  }

  const element = game.currentElement;
  const isSlide = element.kind === "Slide";
  const isVotePhase = game.phase === "VOTE";
  const elementSeconds = element.displaySeconds ?? 0;
  const showCountdownChrome = !isSlide && (
    isVotePhase
      ? game.votePhaseSeconds > 0
      : (elementSeconds > 0 || !interactiveSessionUnlimited)
  );
  const questionCardTimeLimit = isVotePhase
    ? game.votePhaseSeconds
    : elementSeconds > 0
      ? elementSeconds
      : (session?.settings?.timePerQuestion ?? 0);

  return (
    <div className={styles.play} style={bgStyle}>
      <div className={styles.main}>
        <WsErrorBanner />
        {!isSlide && (
          <QuestionCard
            question={{
              questionText: "prompt" in element ? (element.prompt ?? "") : "",
              pointValue:
                "pointValue" in element ? (element.pointValue ?? 0) : 0,
              timeLimit: questionCardTimeLimit,
              imageUrl: largestUrl(element.image, element.id ?? "") ?? undefined,
            }}
            round={game.round}
            totalRounds={game.totalRounds}
            timeRemaining={timeRemaining}
            noTimer={!showCountdownChrome}
          />
        )}
        {isVotePhase ? (
          <VotePanel
            element={element}
            submissions={game.voteSubmissions}
            myVote={game.myVote}
            totalPlayers={game.players.length}
            votedCount={game.votedThisRound.length}
            timeRemaining={timeRemaining}
            unlimited={game.votePhaseSeconds <= 0}
            onVote={handleVote}
          />
        ) : (
          <ElementRenderer
            element={element}
            round={game.round}
            totalRounds={game.totalRounds}
            timeRemaining={timeRemaining}
            mySubmission={game.myAnswer}
            roundResultElement={game.roundResult?.element ?? null}
            onSubmit={handleAnswer}
          />
        )}
      </div>
      <aside className={styles.sidebar}>
        <ScoreBoard
          players={game.players}
          currentUserId={userId}
          hideScores={hideScoresDuringPlay}
          answeredUserIds={
            isSlide
              ? undefined
              : isVotePhase
                ? game.votedThisRound
                : game.answeredThisRound
          }
          offlineUserIds={game.offlineUserIds}
          isHost={isHost}
          onBootPlayer={(id) => {
            void handleBoot(id);
          }}
        />
        {isHost && (
          <Btn
            type='button'
            variant='error'
            className={styles.endInteractiveSessionBtn}
            onClick={() => {
              void handleEndInteractiveSession();
            }}>
            End InteractiveSession
          </Btn>
        )}
      </aside>
      {game.roundResult && (
        <RoundResult
          result={game.roundResult}
          currentUserId={userId}
          isHost={isHost}
          isTurnBased={isTurnBased}
          onNextRound={sendNextRound}
        />
      )}
    </div>
  );
};

export { PlayPage };
