import type { InteractiveSessionPlayerResponse } from "@/store/BrainFlexApi";
import styles from "./SessionPlayerList.module.css";

interface PlayerListItemInterface {
  player: InteractiveSessionPlayerResponse;
}
const PlayerListItem = ({ player }: PlayerListItemInterface) => {
  return (
    <div className={styles.playerListItem}>
      <div className={styles.name}> {player.user.name}</div>
      <div className={styles.score}>{player.score}</div>
    </div>
  );
};

export { PlayerListItem };
