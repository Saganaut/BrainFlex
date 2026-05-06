import { getRouteApi, useNavigate } from "@tanstack/react-router";
import { useState, useEffect } from "react";
import { AnswerOptions } from "../../components/Games/AnswerOptions/AnswerOptions";
import { QuestionCard } from "../../components/Games/QuestionCard/QuestionCard";
import { RoundResult } from "../../components/Games/RoundResult/RoundResult";
import { ScoreBoard } from "../../components/Games/ScoreBoard/ScoreBoard";
import { useCurrentUser } from "../../hooks/useCurrentUser";
import { useGameSession } from "../../hooks/useGameSession";
import { useGameWebSocket } from "../../hooks/useGameWebSocket";
import { useGetSessionQuery } from "../../store/BrainFlexApi";
import { setSession, answerSelected } from "../../store/gameSlice";
import { useAppDispatch } from "../../store/hooks";
import styles from "./Game.module.css";

const routeApi = getRouteApi("/games/$roomCode/play");

const PlayPage = () => {
  const { roomCode } = routeApi.useParams();
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const userState = useCurrentUser();
  const game = useGameSession();
  const { sendAnswer, sendNextRound } = useGameWebSocket(roomCode);
  const { data: session } = useGetSessionQuery({ roomCode });
  const [timeRemaining, setTimeRemaining] = useState(0);

  const userId =
    userState.state === "registered" || userState.state === "guest"
      ? userState.user.id
      : undefined;

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
  //   const displayTime =
  //     !game.currentQuestion || !game.roundStartedAt || game.roundResult
  //       ? 0
  //       : timeRemaining;

  useEffect(() => {
    if (!game.currentQuestion || !game.roundStartedAt || game.roundResult) {
      return;
    }
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
  }, [game.currentQuestion, game.roundStartedAt, game.roundResult]);

  const handleAnswer = (index: number) => {
    if (game.myAnswer !== null || !game.currentQuestion) return;
    dispatch(answerSelected(index));
    sendAnswer(game.currentQuestion.id, index);
  };

  const isHost = !!userId && session?.hostUserId === userId;
  const isTurnBased = session?.settings?.gameMode === "TURN_BASED";

  if (!game.currentQuestion) {
    return (
      <div className={styles.waiting}>
        <p className={styles.waitingMsg}>Waiting for the first question…</p>
        <ScoreBoard players={game.players} currentUserId={userId} />
      </div>
    );
  }

  return (
    <div className={styles.play}>
      <div className={styles.main}>
        <QuestionCard
          question={game.currentQuestion}
          round={game.round}
          totalRounds={game.totalRounds}
          timeRemaining={timeRemaining}
        />
        <AnswerOptions
          options={game.currentQuestion.options}
          selectedOption={game.myAnswer}
          correctOption={game.roundResult?.correctAnswer}
          onSelect={handleAnswer}
          disabled={game.myAnswer !== null}
        />
      </div>
      <aside className={styles.sidebar}>
        <ScoreBoard players={game.players} currentUserId={userId} />
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
