/**
 * Active game route (/games/$roomCode/play).
 * Orchestrates the round lifecycle: countdown timer, question display, answer
 * selection, round-result overlay, and auto-navigation to /results on GAME_OVER.
 */
import { useEffect, useState } from "react";
import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useAppDispatch } from "../../../store/hooks";
import { setSession, answerSelected } from "../../../store/gameSlice";
import { useGetSessionQuery } from "../../../store/BrainFlexApi";
import { useGameSession } from "../../../hooks/useGameSession";
import { useGameWebSocket } from "../../../hooks/useGameWebSocket";
import { useCurrentUser } from "../../../hooks/useCurrentUser";
import { QuestionCard } from "../../../components/Games/QuestionCard";
import { AnswerOptions } from "../../../components/Games/AnswerOptions";
import { ScoreBoard } from "../../../components/Games/ScoreBoard";
import { RoundResult } from "../../../components/Games/RoundResult";
import styles from "./play.module.css";

export const Route = createFileRoute("/games/$roomCode/play")({
  component: PlayPage,
});

function PlayPage() {
  const { roomCode } = Route.useParams();
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { user } = useCurrentUser();
  const game = useGameSession();
  const { sendAnswer, sendNextRound } = useGameWebSocket(roomCode);
  const { data: session } = useGetSessionQuery({ roomCode });
  const [timeRemaining, setTimeRemaining] = useState(0);

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

  useEffect(() => {
    if (!game.currentQuestion || !game.roundStartedAt || game.roundResult) {
      setTimeRemaining(0);
      return;
    }
    const timeLimit = game.currentQuestion.timeLimit;
    const start = new Date(game.roundStartedAt).getTime();
    const tick = () =>
      setTimeRemaining(
        Math.max(0, Math.ceil(timeLimit - (Date.now() - start) / 1000)),
      );
    tick();
    const id = setInterval(tick, 250);
    return () => clearInterval(id);
  }, [game.currentQuestion, game.roundStartedAt, game.roundResult]);

  const handleAnswer = (index: number) => {
    if (game.myAnswer !== null || !game.currentQuestion) return;
    dispatch(answerSelected(index));
    sendAnswer(game.currentQuestion.id, index);
  };

  const isHost = !!user?.id && session?.hostUserId === user.id;
  const isTurnBased = session?.settings?.gameMode === "TURN_BASED";

  if (!game.currentQuestion) {
    return (
      <div className={styles.waiting}>
        <p className={styles.waitingMsg}>Waiting for the first question…</p>
        <ScoreBoard players={game.players} currentUserId={user?.id} />
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
        <ScoreBoard players={game.players} currentUserId={user?.id} />
      </aside>
      {game.roundResult && (
        <RoundResult
          result={game.roundResult}
          currentUserId={user?.id}
          isHost={isHost}
          isTurnBased={isTurnBased}
          onNextRound={sendNextRound}
        />
      )}
    </div>
  );
}
