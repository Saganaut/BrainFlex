/**
 * Live player score list, sorted by descending score.
 * Rendered during active play so all participants can track standings.
 * The local player's row is highlighted for quick self-identification.
 */
import styles from "./ScoreBoard.module.css";
import type { SessionPlayerDto } from "../../../store/BrainFlexApi";

type Props = {
  players: SessionPlayerDto[];
  currentUserId?: string;
};

export function ScoreBoard({ players, currentUserId }: Props) {
  const sorted = [...players].sort((a, b) => (b.score ?? 0) - (a.score ?? 0));

  return (
    <div className={styles.board}>
      <h3 className={styles.title}>Scores</h3>
      <ol className={styles.list}>
        {sorted.map((p, i) => (
          <li
            key={p.userId}
            className={`${styles.row} ${p.userId === currentUserId ? styles.me : ""}`}
          >
            <span className={styles.rank}>{i + 1}</span>
            <span className={styles.name}>{p.userName}</span>
            {p.isGuest && <span className={styles.guest}>guest</span>}
            <span className={styles.score}>{p.score ?? 0}</span>
          </li>
        ))}
      </ol>
    </div>
  );
}
