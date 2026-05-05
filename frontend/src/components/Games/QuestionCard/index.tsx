/**
 * Displays the current question text, round number, point value, and
 * a countdown timer bar. Receives all data as props from the Play route.
 */
import styles from "./QuestionCard.module.css";
import type { QuestionData } from "../../../store/gameSlice";

type Props = {
  question: QuestionData;
  round: number;
  totalRounds: number;
  timeRemaining: number;
};

export function QuestionCard({ question, round, totalRounds, timeRemaining }: Props) {
  const pct = Math.max(0, (timeRemaining / question.timeLimit) * 100);
  const urgent = timeRemaining <= 5;

  return (
    <div className={styles.card}>
      <div className={styles.meta}>
        <span className={styles.roundLabel}>
          Round {round} / {totalRounds}
        </span>
        <span className={styles.points}>{question.pointValue} pts</span>
      </div>

      <div className={`${styles.timerTrack} ${urgent ? styles.urgent : ""}`}>
        <div className={styles.timerBar} style={{ width: `${pct}%` }} />
      </div>
      <span className={`${styles.timerText} ${urgent ? styles.urgentText : ""}`}>
        {timeRemaining}s
      </span>

      {question.imageUrl && (
        <img src={question.imageUrl} alt="" className={styles.image} />
      )}

      <p className={styles.questionText}>{question.questionText}</p>
    </div>
  );
}
