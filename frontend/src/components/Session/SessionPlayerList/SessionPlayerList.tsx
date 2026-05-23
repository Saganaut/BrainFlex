import { useSession } from "@/pages/SessionPage/useSession";
import styles from "./SessionPlayerList.module.css";
import { PlayerListItem } from "./PlayerListeItem";
const SessionPlayerList = () => {
  const { interactiveSession } = useSession();

  console.log("player list", interactiveSession.players);
  return (
    <div className={styles.sessionPlayerList}>
      {interactiveSession.players.map((player) => (
        <PlayerListItem key={player.playerId} player={player} />
      ))}
    </div>
  );
};

export { SessionPlayerList };
