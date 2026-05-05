/**
 * Final results screen shown after a game ends.
 * Renders the top-3 podium, a full placement table, and navigation back to the hub.
 * Data comes from the GAME_OVER WebSocket payload stored in the game Redux slice.
 */
import { Link } from "@tanstack/react-router";
import styles from "./GameOver.module.css";
import type { PlayerPlacement } from "../../../store/BrainFlexApi";

type Props = {
  placements: PlayerPlacement[];
  currentUserId?: string;
};

const PLACE_LABELS = ["1st", "2nd", "3rd"];

export function GameOver({ placements, currentUserId }: Props) {
  const top3 = placements.slice(0, 3);
  const rest = placements.slice(3);

  return (
    <div className={styles.container}>
      <h1 className={styles.title}>Game Over</h1>

      <div className={styles.podium}>
        {top3.map((p, i) => (
          <div
            key={p.userId}
            className={`${styles.place} ${styles[`place${i + 1}`]} ${p.userId === currentUserId ? styles.me : ""}`}
          >
            <span className={styles.placeLabel}>{PLACE_LABELS[i]}</span>
            <span className={styles.placeName}>{p.userName}</span>
            <span className={styles.placeScore}>{p.finalScore ?? 0} pts</span>
          </div>
        ))}
      </div>

      {rest.length > 0 && (
        <ol className={styles.restList} start={4}>
          {rest.map((p) => (
            <li
              key={p.userId}
              className={`${styles.restRow} ${p.userId === currentUserId ? styles.me : ""}`}
            >
              <span className={styles.restName}>{p.userName}</span>
              {p.guest && <span className={styles.guestBadge}>guest</span>}
              <span className={styles.restScore}>{p.finalScore ?? 0} pts</span>
            </li>
          ))}
        </ol>
      )}

      <div className={styles.footer}>
        <Link to="/games" className={styles.footerLink}>
          Play Again
        </Link>
        <Link to="/" className={styles.footerLink}>
          Home
        </Link>
      </div>
    </div>
  );
}
