/**
 * Pre-game waiting room — shows joined players, the shareable room code,
 * and (for the host) the Start Game button. Real-time player-list updates
 * arrive via /topic/game/{roomCode}/lobby; the WebSocket is managed here.
 */
import { useEffect } from "react";
import { useNavigate } from "@tanstack/react-router";
import { useAppDispatch } from "../../../store/hooks";
import { setSession } from "../../../store/gameSlice";
import { useGetSessionQuery } from "../../../store/BrainFlexApi";
import { useGameSession } from "../../../hooks/useGameSession";
import { useGameWebSocket } from "../../../hooks/useGameWebSocket";
import { useCurrentUser } from "../../../hooks/useCurrentUser";
import styles from "./Lobby.module.css";

type Props = { roomCode: string };

export function Lobby({ roomCode }: Props) {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { user } = useCurrentUser();
  const game = useGameSession();
  const { sendStart, sendLeave } = useGameWebSocket(roomCode);

  const { data: session } = useGetSessionQuery({ roomCode });

  useEffect(() => {
    if (session) dispatch(setSession(session));
  }, [session, dispatch]);

  useEffect(() => {
    if (game.status === "IN_PROGRESS") {
      void navigate({ to: "/games/$roomCode/play", params: { roomCode } });
    }
  }, [game.status, navigate, roomCode]);

  const isHost = !!user?.id && session?.hostUserId === user.id;
  const players = game.players;

  return (
    <div className={styles.lobby}>
      <div className={styles.header}>
        <h1 className={styles.title}>Lobby</h1>
        <div className={styles.codeBox}>
          <span className={styles.codeLabel}>Room Code</span>
          <span className={styles.codeValue}>{roomCode}</span>
          <span className={styles.codeHint}>Share this with friends</span>
        </div>
      </div>

      <div className={styles.playerSection}>
        <h2 className={styles.playerHeading}>
          Players ({players.length} / {session?.settings?.maxPlayers ?? 8})
        </h2>
        <ul className={styles.playerList}>
          {players.map((p) => (
            <li key={p.userId} className={styles.player}>
              {p.pictureUrl ? (
                <img src={p.pictureUrl} alt="" className={styles.avatar} />
              ) : (
                <div className={styles.avatarFallback}>
                  {(p.userName?.[0] ?? "?").toUpperCase()}
                </div>
              )}
              <span className={styles.playerName}>{p.userName}</span>
              {p.isGuest && <span className={styles.guestBadge}>guest</span>}
              {session?.hostUserId === p.userId && (
                <span className={styles.hostBadge}>host</span>
              )}
            </li>
          ))}
        </ul>
        {players.length === 0 && (
          <p className={styles.emptyState}>Waiting for players to join…</p>
        )}
      </div>

      <div className={styles.actions}>
        {isHost ? (
          <button
            type="button"
            className={styles.startBtn}
            onClick={sendStart}
            disabled={players.length < 1}
          >
            Start Game
          </button>
        ) : (
          <p className={styles.waitingMsg}>Waiting for the host to start…</p>
        )}
        <button type="button" className={styles.leaveBtn} onClick={sendLeave}>
          Leave
        </button>
      </div>
    </div>
  );
}
