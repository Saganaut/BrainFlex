import { getRouteApi, useNavigate } from "@tanstack/react-router";
import { useState, useEffect } from "react";
import { AnswerOptions } from "../../components/Games/AnswerOptions/AnswerOptions";
import { QuestionCard } from "../../components/Games/QuestionCard/QuestionCard";
import { RoundResult } from "../../components/Games/RoundResult/RoundResult";
import { ScoreBoard } from "../../components/Games/ScoreBoard/ScoreBoard";
import { TextAnswerInput } from "../../components/Games/TextAnswerInput/TextAnswerInput";
import { SlideView } from "../../components/Games/SlideView/SlideView";
import { WsErrorBanner } from "../../components/Games/WsErrorBanner/WsErrorBanner";
import { useCurrentUser } from "../../hooks/useCurrentUser";
import { useGameSession } from "../../hooks/useGameSession";
import { useGameWebSocket } from "../../hooks/useGameWebSocket";
import { Btn } from "@/components/Common/Buttons/Btn";
import { useGetShowcaseQuery } from "../../store/BrainFlexApi";
import { resolveShowcaseBackground } from "../../utils/deckImages";
import {
  setSession,
  answerSelected,
  textAnswerSubmitted,
} from "../../store/gameSlice";
import { useAppDispatch } from "../../store/hooks";
import styles from "./Game.module.css";

const routeApi = getRouteApi("/games/$roomCode/play");

const PlayPage = () => {
  const { roomCode } = routeApi.useParams();
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const userState = useCurrentUser();
  const game = useGameSession();
  const { sendAnswer, sendNextRound, sendBoot, sendEndShowcase } =
    useGameWebSocket(roomCode);
  const { data: session } = useGetShowcaseQuery({ roomCode });
  const [timeRemaining, setTimeRemaining] = useState(0);

  const userId =
    userState.state === "registered" || userState.state === "guest"
      ? userState.user.id
      : undefined;

  const isHost = !!userId && session?.hostUserId === userId;
  const isTurnBased = session?.settings?.gameMode === "TURN_BASED";
  // The host disables the timer by setting timePerQuestion = 0; slides
  // always have their own display timer regardless and are handled below.
  const noTimer = (session?.settings?.timePerQuestion ?? 1) === 0;
  const hideScoresDuringPlay = session?.settings?.showScoresImmediately === false;

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

  // If the host boots us, the lobby broadcast no longer includes our userId.
  // Send the player home rather than leaving them on a now-unjoinable page.
  useEffect(() => {
    if (!userId || game.players.length === 0) return;
    if (game.status === "FINISHED" || game.status === "CANCELLED") return;
    const stillInGame = game.players.some((p) => p.userId === userId);
    if (!stillInGame) {
      void navigate({ to: "/" });
    }
  }, [userId, game.players, game.status, navigate]);

  const handleBoot = (targetUserId: string) => {
    if (!confirm("Remove this player from the showcase?")) return;
    sendBoot(targetUserId);
  };

  const handleEndShowcase = () => {
    if (!confirm("End the showcase now? Scores so far will be final.")) return;
    sendEndShowcase();
  };
  //   const displayTime =
  //     !game.currentQuestion || !game.roundStartedAt || game.roundResult
  //       ? 0
  //       : timeRemaining;

  useEffect(() => {
    if (!game.currentQuestion || !game.roundStartedAt || game.roundResult) return;
    // Slides always have a display timer — even when the host set noTimer on
    // questions, the slide still auto-advances. Only skip the countdown for
    // question rounds when noTimer is enabled.
    const isSlideRound = game.currentQuestion.kind === "SLIDE";
    if (!isSlideRound && noTimer) return;
    const timeLimit = game.currentQuestion.timeLimit;
    const start = new Date(game.roundStartedAt).getTime();
    const tick = () => {
      // eslint-disable-next-line react-x/set-state-in-effect
      setTimeRemaining(
        Math.max(0, Math.ceil(timeLimit - (Date.now() - start) / 1000)),
      );
    };
    tick();
    const id = setInterval(tick, 250);
    return () => {
      clearInterval(id);
    };
  }, [game.currentQuestion, game.roundStartedAt, game.roundResult, noTimer]);

  const handleAnswer = (index: number) => {
    if (game.myAnswer !== null || !game.currentQuestion) return;
    dispatch(answerSelected(index));
    sendAnswer(game.currentQuestion.id, { selectedOption: index });
  };

  const handleTextAnswer = (text: string) => {
    if (game.myTextAnswer !== null || !game.currentQuestion) return;
    dispatch(textAnswerSubmitted(text));
    sendAnswer(game.currentQuestion.id, { textAnswer: text });
  };

  const backgroundUrl = resolveShowcaseBackground(
    session?.deckBackgroundImageUrl,
    session?.deckId,
  );
  const bgStyle: React.CSSProperties = {
    "--showcase-bg": `url(${backgroundUrl})`,
  } as React.CSSProperties;

  if (!game.currentQuestion) {
    return (
      <div className={styles.waiting} style={bgStyle}>
        <WsErrorBanner />
        <p className={styles.waitingMsg}>Waiting for the first question…</p>
        <ScoreBoard
          players={game.players}
          currentUserId={userId}
          hideScores={hideScoresDuringPlay}
          offlineUserIds={game.offlineUserIds}
          isHost={isHost}
          onBootPlayer={handleBoot}
        />
      </div>
    );
  }

  const isSlide = game.currentQuestion.kind === "SLIDE";

  return (
    <div className={styles.play} style={bgStyle}>
      <div className={styles.main}>
        <WsErrorBanner />
        {isSlide ? (
          <SlideView
            slide={game.currentQuestion}
            round={game.round}
            totalRounds={game.totalRounds}
            timeRemaining={timeRemaining}
          />
        ) : (
          <>
            <QuestionCard
              question={game.currentQuestion}
              round={game.round}
              totalRounds={game.totalRounds}
              timeRemaining={timeRemaining}
              noTimer={noTimer}
            />
            {game.currentQuestion.type === "TEXT_INPUT" ? (
              <TextAnswerInput
                key={game.currentQuestion.id}
                questionId={game.currentQuestion.id}
                submittedAnswer={game.myTextAnswer}
                correctAnswerText={game.roundResult?.correctAnswerText}
                wasCorrect={
                  game.roundResult?.playerResults.find((r) => r.userId === userId)
                    ?.wasCorrect
                }
                onSubmit={handleTextAnswer}
                disabled={false}
              />
            ) : (
              <AnswerOptions
                options={game.currentQuestion.options ?? []}
                selectedOption={game.myAnswer}
                correctOption={
                  game.roundResult && game.roundResult.correctAnswer >= 0
                    ? game.roundResult.correctAnswer
                    : undefined
                }
                onSelect={handleAnswer}
                disabled={game.myAnswer !== null}
              />
            )}
          </>
        )}
      </div>
      <aside className={styles.sidebar}>
        <ScoreBoard
          players={game.players}
          currentUserId={userId}
          hideScores={hideScoresDuringPlay}
          // Slides have no concept of "answered" — pass undefined so the indicator hides.
          answeredUserIds={isSlide ? undefined : game.answeredThisRound}
          offlineUserIds={game.offlineUserIds}
          isHost={isHost}
          onBootPlayer={handleBoot}
        />
        {isHost && (
          <Btn
            type='button'
            variant='error'
            className={styles.endShowcaseBtn}
            onClick={handleEndShowcase}>
            End Showcase
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
