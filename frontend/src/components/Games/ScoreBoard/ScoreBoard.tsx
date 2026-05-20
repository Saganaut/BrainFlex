/**
 * Live player score list, sorted by descending score.
 * Rendered during active play so all participants can track standings.
 * The local player's row is highlighted for quick self-identification.
 *
 * Optional dashboard extensions:
 *  - `answeredUserIds`: shows ✓ next to players who've submitted for the current round.
 *  - `isHost` + `onBootPlayer`: shows a Boot button on other players' rows when the
 *    viewer is the host.
 */
import { Btn } from "@/components/Common/Buttons/Btn";
import styles from "./ScoreBoard.module.css";
import type { InteractiveSessionPlayerDto } from "../../../store/BrainFlexApi";

export interface ScoreBoardProps {
  players: InteractiveSessionPlayerDto[];
  currentUserId?: string;
  // When true the host configured scores to stay hidden during play — render the
  // player list with no rank ordering and no point values.
  hideScores?: boolean;
  // userIds that have already submitted an answer for the current round.
  answeredUserIds?: string[];
  // userIds whose WebSocket session has dropped — rendered with a dimmed style.
  offlineUserIds?: string[];
  // Whether the viewer is the host (controls whether boot buttons render).
  isHost?: boolean;
  // Called when the host clicks Boot on another player's row.
  onBootPlayer?: (userId: string) => void;
}

const ScoreBoard = ({
  players,
  currentUserId,
  hideScores,
  answeredUserIds,
  offlineUserIds,
  isHost,
  onBootPlayer,
}: ScoreBoardProps) => {
  const sorted = hideScores
    ? players
    : [...players].sort((a, b) => (b.score ?? 0) - (a.score ?? 0));

  const answeredSet = new Set(answeredUserIds ?? []);
  const offlineSet = new Set(offlineUserIds ?? []);
  const showAnswered = !!answeredUserIds;

  return (
    <div className={styles.board}>
      <h3 className={styles.title}>{hideScores ? "Players" : "Scores"}</h3>
      <ol className={styles.list}>
        {sorted.map((p, i) => {
          const playerId = p.userId;
          const isSelf = playerId === currentUserId;
          const answered = !!playerId && answeredSet.has(playerId);
          const isOffline = !!playerId && offlineSet.has(playerId);
          return (
            <li
              key={playerId}
              className={`${styles.row} ${isSelf ? styles.me : ""} ${isOffline ? styles.offline : ""}`}>
              {!hideScores && <span className={styles.rank}>{i + 1}</span>}
              <span className={styles.name}>{p.userName}</span>
              {p.isGuest && <span className={styles.guest}>guest</span>}
              {isOffline && (
                <span className={styles.offlineLabel} title='Disconnected'>
                  offline
                </span>
              )}
              {showAnswered && (
                <span
                  className={`${styles.statusDot} ${answered ? styles.answered : styles.pending}`}
                  aria-label={answered ? "Answered" : "Still answering"}
                  title={answered ? "Answered" : "Still answering"}>
                  {answered ? "✓" : "…"}
                </span>
              )}
              {!hideScores && (
                <span className={styles.score}>{p.score ?? 0}</span>
              )}
              {isHost && !isSelf && onBootPlayer && playerId && (
                <Btn
                  size='sm'
                  variant='error'
                  type='button'
                  className={styles.bootBtn}
                  onClick={() => {
                    onBootPlayer(playerId);
                  }}
                  aria-label={`Remove ${p.userName ?? "player"} from the interactiveSession`}>
                  Boot
                </Btn>
              )}
            </li>
          );
        })}
      </ol>
    </div>
  );
};
export { ScoreBoard };
