/**
 * Game hub landing page — entry point for all game activity.
 * Shows "Create Game" (registered users only) and "Join Game" actions,
 * routing users to the create or join flows from a single screen.
 */
import { Link } from "@tanstack/react-router";
import { useCurrentUser } from "../../../hooks/useCurrentUser";
import styles from "./GameHub.module.css";

export function GameHub() {
  const { authenticated } = useCurrentUser();

  return (
    <div className={styles.hub}>
      <h1 className={styles.title}>Play a Game</h1>
      <p className={styles.subtitle}>
        Challenge your brain in a live multiplayer trivia session.
      </p>
      <div className={styles.cards}>
        {authenticated ? (
          <Link to="/games/create" className={styles.card}>
            <span className={styles.icon}>+</span>
            <span className={styles.cardTitle}>Create Game</span>
            <span className={styles.cardDesc}>
              Pick a content pack and invite friends with a room code.
            </span>
          </Link>
        ) : (
          <div className={`${styles.card} ${styles.disabled}`}>
            <span className={styles.icon}>+</span>
            <span className={styles.cardTitle}>Create Game</span>
            <span className={styles.cardDesc}>Sign in to host a game.</span>
          </div>
        )}
        <Link to="/games/join" className={styles.card}>
          <span className={styles.icon}>-&gt;</span>
          <span className={styles.cardTitle}>Join Game</span>
          <span className={styles.cardDesc}>
            Enter a 6-character room code from your host.
          </span>
        </Link>
      </div>
    </div>
  );
}
