/**
 * Final results screen shown after a game ends.
 * Renders the top-3 podium, a full placement table, and navigation back to the hub.
 * Data comes from the GAME_OVER WebSocket payload stored in the game Redux slice.
 */
import { Link } from "@tanstack/react-router";
import styles from "./GameOver.module.css";
import { TeamPodium } from "../TeamPodium/TeamPodium";
import type { PlayerPlacement, Team } from "../../../store/BrainFlexApi";

interface GameOverProps {
  placements: PlayerPlacement[];
  currentUserId?: string;
  // Team-mode chunk 12: when provided alongside placements that carry teamId,
  // GameOver renders a team podium above the individual podium. Empty array
  // (or undefined) preserves the original individual-only layout.
  teams?: Team[];
}

const PLACE_LABELS = ["1st", "2nd", "3rd"];

const GameOver = ({ placements, currentUserId, teams }: GameOverProps) => {
  const top3 = placements.slice(0, 3);
  const rest = placements.slice(3);
  const teamModeActive =
    !!teams && teams.length > 0 && placements.some((p) => !!p.teamId);

  return (
    <div className={styles.container}>
      <h1 className={styles.title}>Game Over</h1>

      {teamModeActive && (
        <TeamPodium
          placements={placements}
          teams={teams}
          currentUserId={currentUserId}
        />
      )}

      <div className={styles.podium}>
        {top3.map((p, i) => (
          <div
            key={p.userId}
            className={`${styles.place} ${styles[`place${i + 1}`]} ${p.userId === currentUserId ? styles.me : ""}`}>
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
              className={`${styles.restRow} ${p.userId === currentUserId ? styles.me : ""}`}>
              <span className={styles.restName}>{p.userName}</span>
              {p.guest && <span className={styles.guestBadge}>guest</span>}
              <span className={styles.restScore}>{p.finalScore ?? 0} pts</span>
            </li>
          ))}
        </ol>
      )}

      <div className={styles.footer}>
        <Link to='/decks' className={styles.footerLink} viewTransition>
          Play Again
        </Link>
        <Link to='/' className={styles.footerLink} viewTransition>
          Home
        </Link>
      </div>
    </div>
  );
};

export { GameOver };
