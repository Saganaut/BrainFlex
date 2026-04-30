import React, { useState } from "react";
import { useGetLeaderboardQuery } from "../../store/BrainFlexApi";
import styles from "./Leaderboard.module.css";
interface LeaderBoardProps {}

const Leaderboard: React.FC<LeaderBoardProps> = ({}) => {
  const { data, isLoading, isError } = useGetLeaderboardQuery({
    page: 0,
    size: 10,
  });

  const [isCollapsed, collapse] = useState(false);

  return (
    <div className={styles.leaderboardContainer}>
      <div className={styles.leaderboard}>
        <div className={styles.leaderboardHeader}>
          <h3 className={styles.leaderboardTitle}>Leaderboard </h3>
          <button
            onClick={() => {
              collapse(!isCollapsed);
            }}>
            <svg
              xmlns='http://www.w3.org/2000/svg'
              fill='none'
              viewBox='0 0 24 24'
              strokeWidth={1.5}
              stroke='white'
              className={`${isCollapsed ? styles.isCollapsed : ""}`}>
              <path
                strokeLinecap='round'
                strokeLinejoin='round'
                d='m19.5 8.25-7.5 7.5-7.5-7.5'
              />
            </svg>
          </button>
        </div>
        {isLoading ? (
          <div>Loading</div>
        ) : isError ? (
          <div>Error</div>
        ) : (
          <div
            className={`${styles.playerList} ${isCollapsed ? styles.isCollapsed : ""}`}>
            {data?.map((player) => (
              <div key={player.id} className={styles.playerRow}>
                <p>{player.userName}</p>
                <p>{player.stats?.totalPoints}</p>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export { Leaderboard };
