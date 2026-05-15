/**
 * Pre-game waiting room — shows joined players, the shareable room code,
 * and (for the host) the Start Game button. Real-time player-list updates
 * arrive via /topic/showcase/{roomCode}/lobby; the WebSocket is managed here.
 */
import { useEffect } from "react";
import { useNavigate } from "@tanstack/react-router";
import { useAppDispatch } from "../../../store/hooks";
import { setSession } from "../../../store/gameSlice";
import { useGetShowcaseQuery } from "../../../store/BrainFlexApi";
import { useGameSession } from "../../../hooks/useGameSession";
import { useGameWebSocket } from "../../../hooks/useGameWebSocket";
import { useCurrentUser } from "../../../hooks/useCurrentUser";
import { WsErrorBanner } from "../WsErrorBanner/WsErrorBanner";
import { resolveShowcaseBackground } from "../../../utils/deckImages";
import styles from "./Lobby.module.css";
import { Btn } from "@/components/Common/Buttons/Btn";
import { useConfirm } from "@/components/Common/ConfirmDialog/useConfirm";

interface LobbyProps {
  roomCode: string;
}

const Lobby = ({ roomCode }: LobbyProps) => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const userState = useCurrentUser();
  const game = useGameSession();
  const { sendStart, sendLeave, sendBoot } = useGameWebSocket(roomCode);
  const confirm = useConfirm();

  const { data: session } = useGetShowcaseQuery({ roomCode });

  useEffect(() => {
    if (session) dispatch(setSession(session));
  }, [session, dispatch]);

  useEffect(() => {
    if (game.status === "IN_PROGRESS") {
      void navigate({ to: "/games/$roomCode/play", params: { roomCode } });
    }
  }, [game.status, navigate, roomCode]);

  const userId =
    userState.state === "registered" || userState.state === "guest"
      ? userState.user.id
      : undefined;
  const isHost = !!userId && session?.hostUserId === userId;
  const players = game.players;

  // If the host boots us (or anything else removes us from the player list),
  // navigate home rather than stranding the user on an empty lobby.
  useEffect(() => {
    if (!userId || players.length === 0) return;
    const stillInLobby = players.some((p) => p.userId === userId);
    if (!stillInLobby) {
      void navigate({ to: "/" });
    }
  }, [userId, players, navigate]);

  const handleBoot = async (targetUserId: string) => {
    const ok = await confirm({
      title: "Remove player",
      message: "Remove this player from the lobby?",
      confirmLabel: "Remove",
      variant: "danger",
    });
    if (!ok) return;
    sendBoot(targetUserId);
  };

  const backgroundUrl = resolveShowcaseBackground(
    session?.deckBackgroundImageUrl,
    session?.deckId,
  );

  return (
    <div
      className={styles.lobby}
      style={{ "--showcase-bg": `url(${backgroundUrl})` } as React.CSSProperties}>
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
          {players.map((p) => {
            const playerId = p.userId;
            const isPlayerHost = session?.hostUserId === playerId;
            const isOffline = !!playerId && game.offlineUserIds.includes(playerId);
            return (
              <li
                key={playerId}
                className={`${styles.player} ${isOffline ? styles.offline : ""}`}>
                {p.pictureUrl ? (
                  <img src={p.pictureUrl} alt='' className={styles.avatar} />
                ) : (
                  <div className={styles.avatarFallback}>
                    {(p.userName?.[0] ?? "?").toUpperCase()}
                  </div>
                )}
                <span className={styles.playerName}>{p.userName}</span>
                {p.isGuest && <span className={styles.guestBadge}>guest</span>}
                {isOffline && (
                  <span className={styles.offlineBadge} title='Disconnected'>
                    offline
                  </span>
                )}
                {isPlayerHost && (
                  <span className={styles.hostBadge}>host</span>
                )}
                {isHost && !isPlayerHost && playerId && (
                  <Btn
                    size='sm'
                    variant='error'
                    type='button'
                    onClick={() => {
                      void handleBoot(playerId);
                    }}
                    aria-label={`Remove ${p.userName ?? "player"} from the lobby`}>
                    Boot
                  </Btn>
                )}
              </li>
            );
          })}
        </ul>
        {players.length === 0 && (
          <p className={styles.emptyState}>Waiting for players to join…</p>
        )}
      </div>

      <div className={styles.actions}>
        <WsErrorBanner />
        {isHost ? (
          <Btn
            type='button'
            className={styles.startBtn}
            onClick={sendStart}
            disabled={players.length < 1}>
            Start Game
          </Btn>
        ) : (
          <p className={styles.waitingMsg}>Waiting for the host to start…</p>
        )}
        <Btn type='button' className={styles.leaveBtn} onClick={sendLeave}>
          Leave
        </Btn>
      </div>
    </div>
  );
};

export { Lobby };
