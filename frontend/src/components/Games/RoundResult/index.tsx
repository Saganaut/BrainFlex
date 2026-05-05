/**
 * Overlay shown after each round ends — reveals the correct answer,
 * per-player results, and (for the host in turn-based mode) a Next Round button.
 * In simultaneous mode the server auto-advances; a message reflects that.
 */
import styles from "./RoundResult.module.css";
import type { RoundResultPayload } from "../../../store/gameSlice";

type Props = {
  result: RoundResultPayload;
  currentUserId?: string;
  isHost: boolean;
  isTurnBased: boolean;
  onNextRound: () => void;
};

export function RoundResult({
  result,
  currentUserId,
  isHost,
  isTurnBased,
  onNextRound,
}: Props) {
  const myResult = result.playerResults.find((r) => r.userId === currentUserId);
  const sorted = [...result.playerResults].sort(
    (a, b) => b.totalScore - a.totalScore,
  );

  return (
    <div className={styles.overlay}>
      <div className={styles.panel}>
        <h2 className={styles.title}>Round {result.round}</h2>

        <div className={styles.answer}>
          <span className={styles.answerLabel}>Correct answer</span>
          <span className={styles.answerText}>{result.correctAnswerText}</span>
        </div>

        {myResult && (
          <div
            className={`${styles.myResult} ${myResult.wasCorrect ? styles.myCorrect : styles.myWrong}`}
          >
            {myResult.wasCorrect
              ? `Correct! +${myResult.pointsAwarded} pts`
              : "Incorrect"}
          </div>
        )}

        <ol className={styles.results}>
          {sorted.map((r) => (
            <li
              key={r.userId}
              className={`${styles.resultRow} ${r.userId === currentUserId ? styles.me : ""}`}
            >
              <span className={styles.playerName}>{r.userName}</span>
              <span
                className={`${styles.badge} ${r.wasCorrect ? styles.badgeCorrect : styles.badgeWrong}`}
              >
                {r.wasCorrect ? `+${r.pointsAwarded}` : "x"}
              </span>
              <span className={styles.total}>{r.totalScore}</span>
            </li>
          ))}
        </ol>

        {isHost && isTurnBased ? (
          <button type="button" onClick={onNextRound} className={styles.nextBtn}>
            Next Round
          </button>
        ) : (
          <p className={styles.autoAdvance}>Next round starting soon…</p>
        )}
      </div>
    </div>
  );
}
