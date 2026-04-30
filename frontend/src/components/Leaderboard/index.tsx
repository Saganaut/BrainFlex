import React from "react";
import { useGetLeaderboardQuery } from "../../store/BrainFlexApi";
import "./Leaderboard.module.css";
interface LeaderBoardProps {}

const Leaderboard: React.FC<LeaderBoardProps> = ({}) => {
  const { data, isLoading, isError } = useGetLeaderboardQuery({
    page: 0,
    size: 10,
  });

  return (
    <div className='bf-leaderboard-container'>
      {data?.map((player) => (
        <div key={player.id} className={"bf-player-row"}>
          <p>{player.userName}</p>
          <p>{player.stats?.totalPoints}</p>
        </div>
      ))}
    </div>
  );
};

export { Leaderboard };
