/**
 * Renders the most recent WebSocket-handler error from Redux state.
 * Used in the Lobby and Play views where actions (Start, Submit Answer, etc.)
 * are fire-and-forget STOMP messages with no natural place for server feedback.
 * Hidden when there is no error. Dismiss clears the slice's wsError.
 */
import { useAppDispatch, useAppSelector } from "../../../store/hooks";
import { clearWsError } from "../../../store/gameSlice";
import styles from "./WsErrorBanner.module.css";

const WsErrorBanner = () => {
  const dispatch = useAppDispatch();
  const error = useAppSelector((s) => s.game.wsError);

  if (!error) return null;

  return (
    <div className={styles.banner} role='alert'>
      <span className={styles.message}>
        <strong className={styles.operation}>{labelFor(error.operation)}:</strong>{" "}
        {error.message}
      </span>
      <button
        type='button'
        className={styles.dismiss}
        onClick={() => {
          dispatch(clearWsError());
        }}
        aria-label='Dismiss error'>
        ✕
      </button>
    </div>
  );
};

const labelFor = (op: string): string => {
  switch (op) {
    case "start":
      return "Couldn't start the game";
    case "answer":
      return "Couldn't submit answer";
    case "nextRound":
      return "Couldn't advance round";
    case "leave":
      return "Couldn't leave";
    default:
      return "Error";
  }
};

export { WsErrorBanner };
