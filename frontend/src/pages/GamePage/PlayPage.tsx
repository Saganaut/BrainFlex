// Live interactiveSession round screen. Drives polymorphic element rendering via
// ElementRenderer, the host controls (boot / end interactiveSession), the scoreboard,
// the round-result overlay, and the post-round → next-round timer chrome.
import { getRouteApi, useNavigate } from "@tanstack/react-router";
import { useState, useEffect } from "react";
import { ChatPanel } from "../../components/Games/ChatPanel/ChatPanel";
import { ElementRenderer } from "../../components/Games/ElementRenderer/ElementRenderer";
import { QuestionCard } from "../../components/Games/QuestionCard/QuestionCard";
import { ReactionBar } from "../../components/Games/ReactionBar/ReactionBar";
import { ReactionRain } from "../../components/Games/ReactionRain/ReactionRain";
import { RoundResult } from "../../components/Games/RoundResult/RoundResult";
import { RoundDataView } from "../../components/Games/RoundDataView/RoundDataView";
import { ScoreBoard } from "../../components/Games/ScoreBoard/ScoreBoard";
import { TeamLeaderboard } from "../../components/Games/TeamLeaderboard/TeamLeaderboard";
import { VotePanel } from "../../components/Games/VotePanel/VotePanel";
import { WsErrorBanner } from "../../components/Games/WsErrorBanner/WsErrorBanner";
import { useCurrentUser } from "../../hooks/useCurrentUser";
import { useInteractiveSession } from "../../hooks/useInteractiveSession";
import { useInteractiveSessionWebSocket } from "../../hooks/useInteractiveSessionWebSocket";
import { Btn } from "@/components/Common/Buttons/Btn";
import { useConfirm } from "@/components/Common/ConfirmDialog/useConfirm";
import { useGetInteractiveSessionQuery, useGetDeckQuery } from "../../store/BrainFlexApi";
import { resolveInteractiveSessionBackground } from "../../utils/deckImages";
import { largestUrl } from "@/utils/image";
import {
  setSession,
  answerSubmittedLocally,
  voteSubmittedLocally,
} from "../../store/interactiveSessionSlice";
import { useAppDispatch } from "../../store/hooks";
import type { AnswerPayload } from "../../types/elements";
import { resolveShowResponsesFor } from "../../utils/showResponsesResolver";
import styles from "./Game.module.css";

const routeApi = getRouteApi("/games/$roomCode/play");

const PlayPage = () => {
  const { roomCode } = routeApi.useParams();
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const userState = useCurrentUser();
  const game = useInteractiveSession();
  const {
    sendAnswer,
    sendVote,
    sendNextRound,
    sendBoot,
    sendEndInteractiveSession,
    sendRevealNow,
    sendFreezeResponses,
  } = useInteractiveSessionWebSocket(roomCode);
  const { data: session } = useGetInteractiveSessionQuery({ roomCode });
  // Chunk 24 — deck is fetched here only so the host's Reveal button can
  // resolve the showResponses cascade (session > deck > element). Player
  // surfaces don't need it.
  const { data: deck } = useGetDeckQuery(
    { id: session?.deckId ?? "" },
    { skip: !session?.deckId },
  );
  const [timeRemaining, setTimeRemaining] = useState(0);
  const confirm = useConfirm();

  const userId =
    userState.state === "registered" || userState.state === "guest"
      ? userState.user.id
      : undefined;

  const isHost = !!userId && session?.hostUserId === userId;
  const isTurnBased = session?.settings?.answerSubmissionMode === "TURN_BASED";
  // Chunk 24 — frozen session format drives chrome (GAME = persistent
  // leaderboard, PRESENTATION = no leaderboard, aggregated data view at
  // round-end). Fall back to GAME for legacy sessions written before the
  // field landed.
  const isPresentation = (session?.format ?? "GAME") === "PRESENTATION";
  // Host disables the question timer by setting timePerQuestion = 0.
  // Per-element displaySeconds always overrides on the server; the frontend
  // here just respects "is there any countdown?" for the QuestionCard chrome.
  const interactiveSessionUnlimited = (session?.settings?.timePerQuestion ?? 1) === 0;
  const hideScoresDuringPlay =
    session?.settings?.showScoresImmediately === false;
  // Chunk 11 / 12 surfaces — gated by per-session settings + the live state
  // of teams[]. Defaults match the backend: reactions + chat default on,
  // teamMode + teams default empty / off.
  const reactionsEnabled = session?.settings?.reactionsEnabled !== false;
  const chatEnabled = session?.settings?.chatEnabled !== false;
  const teams = session?.teams ?? [];
  const teamMode =
    (session?.teamMode ?? session?.settings?.teamMode ?? false) &&
    teams.length > 0;

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
        {/* Chunk 24 — PRESENTATION sessions don't carry a persistent
            leaderboard at all (the format spec is explicit about chrome:
            "no persistent leaderboard, round-end focuses on aggregated
            data"). Skip the scoreboard mounts entirely for that flavor. */}
        {!isPresentation && teamMode && (
          <TeamLeaderboard
            teams={teams}
            players={game.players}
            currentUserId={userId}
            hideScores={hideScoresDuringPlay}
          />
        )}
        {!isPresentation && (
          <ScoreBoard
            players={game.players}
            currentUserId={userId}
            hideScores={hideScoresDuringPlay}
            offlineUserIds={game.offlineUserIds}
            isHost={isHost}
            onBootPlayer={(id) => {
              void handleBoot(id);
            }}
            teams={teamMode ? teams : undefined}
          />
        )}
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
        {/* Player-side reaction bar — sits below the active element so a
            tap doesn't accidentally interfere with answer submission. Hidden
            for the host (the host sees ReactionRain instead) and gated by
            the per-session reactionsEnabled flag. */}
        {!isHost && reactionsEnabled && !isSlide && (
          <ReactionBar roomCode={roomCode} />
        )}
      </div>
      <aside className={styles.sidebar}>
        {/* Chunk 24 — leaderboard chrome is GAME-only. PRESENTATION hosts
            see only chat + reveal/freeze controls in the sidebar. */}
        {!isPresentation && teamMode && (
          <TeamLeaderboard
            teams={teams}
            players={game.players}
            currentUserId={userId}
            hideScores={hideScoresDuringPlay}
          />
        )}
        {!isPresentation && (
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
            teams={teamMode ? teams : undefined}
          />
        )}
        {chatEnabled && (
          <ChatPanel
            roomCode={roomCode}
            isHost={isHost}
            currentUserId={userId}
          />
        )}
        {isHost && !isSlide && (
          <HostRoundControls
            elementId={element.id ?? ""}
            elementFrozen={game.frozenElementIds.includes(element.id ?? "")}
            elementRevealed={game.revealedElementIds.includes(element.id ?? "")}
            inSubmitPhase={game.phase === "SUBMIT" && !game.roundResult}
            resolvedShowResponses={resolveShowResponsesFor(
              session,
              deck,
              element,
            )}
            onReveal={() => {
              sendRevealNow(element.id ?? "");
            }}
            onToggleFreeze={() => {
              sendFreezeResponses(
                element.id ?? "",
                !game.frozenElementIds.includes(element.id ?? ""),
              );
            }}
          />
        )}
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
      {/* Host-only emoji burst overlay. Pointer-events: none so it doesn't
          intercept clicks on the underlying scoreboard / end-game button. */}
      {isHost && reactionsEnabled && <ReactionRain />}
      {/* Chunk 24 — PRESENTATION uses the aggregated RoundDataView overlay
          instead of RoundResult (no leaderboard, no per-player rankings). */}
      {game.roundResult && isPresentation && (
        <RoundDataView
          result={game.roundResult}
          isHost={isHost}
          isTurnBased={isTurnBased}
          onNextRound={sendNextRound}
        />
      )}
      {game.roundResult && !isPresentation && (
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

/**
 * Chunk 24 — host's per-round control strip. "Reveal results" is enabled
 * only when the resolved showResponses cascade lands on ON_CLICK and the
 * round is still in its SUBMIT phase (post-reveal is a no-op on the server,
 * but we disable client-side so the button doesn't look interactive). The
 * Freeze toggle is universally available — it flips the round's response
 * mode without touching the authored element.
 */
interface HostRoundControlsProps {
  elementId: string;
  elementFrozen: boolean;
  elementRevealed: boolean;
  inSubmitPhase: boolean;
  resolvedShowResponses: "INSTANT" | "ON_CLICK" | "PRIVATE";
  onReveal: () => void;
  onToggleFreeze: () => void;
}

const HostRoundControls = ({
  elementFrozen,
  elementRevealed,
  inSubmitPhase,
  resolvedShowResponses,
  onReveal,
  onToggleFreeze,
}: HostRoundControlsProps) => {
  const canReveal =
    resolvedShowResponses === "ON_CLICK" && inSubmitPhase && !elementRevealed;

  return (
    <div className={styles.hostRoundControls}>
      <Btn
        type='button'
        size='sm'
        disabled={!canReveal}
        onClick={onReveal}>
        {elementRevealed ? "Responses revealed" : "Reveal responses"}
      </Btn>
      <Btn
        type='button'
        size='sm'
        variant={elementFrozen ? "warning" : undefined}
        onClick={onToggleFreeze}>
        {elementFrozen ? "Unfreeze answers" : "Freeze answers"}
      </Btn>
    </div>
  );
};

export { PlayPage };
