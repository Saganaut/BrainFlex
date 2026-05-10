import { useNavigate, Link } from "@tanstack/react-router";
import { useState } from "react";
import { useJoinByRoomCodeMutation } from "../../store/BrainFlexApi";
import styles from "./GameHub.module.css";
const JoinGamePage = () => {
  const navigate = useNavigate();
  const [code, setCode] = useState("");
  const [joinGame, { isLoading, error }] = useJoinByRoomCodeMutation();

  const handleSubmit = async (e: React.SubmitEvent) => {
    e.preventDefault();
    const trimmed = code.trim().toUpperCase();
    if (trimmed.length !== 6) return;
    try {
      await joinGame({ roomCode: trimmed }).unwrap();
      await navigate({
        to: "/games/$roomCode/lobby",
        params: { roomCode: trimmed },
      });
    } catch {
      // error shown via `error` state
    }
  };

  return (
    <div className={styles.page}>
      <h1 className={styles.title}>Join Game</h1>
      <p className={styles.subtitle}>
        Enter the 6-character room code from your host.
      </p>

      <form className={styles.form} onSubmit={(e) => void handleSubmit(e)}>
        <input
          className={styles.codeInput}
          value={code}
          onChange={(e) => {
            setCode(e.target.value.toUpperCase());
          }}
          placeholder='ABCD12'
          maxLength={6}
          autoFocus
          autoComplete='off'
          spellCheck={false}
        />
        <button
          type='submit'
          className={styles.joinBtn}
          disabled={code.trim().length !== 6 || isLoading}>
          {isLoading ? "Joining…" : "Join Game"}
        </button>
        {error && (
          <p className={styles.errorMsg}>
            Could not join — check the room code and try again.
          </p>
        )}
      </form>

      <Link to='/games' className={styles.backLink} viewTransition>
        Back to hub
      </Link>
    </div>
  );
};

export { JoinGamePage };
