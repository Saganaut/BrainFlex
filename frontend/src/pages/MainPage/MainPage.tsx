import { Leaderboard } from "../../components/Leaderboard";
import { PlayerInfo } from "../../components/PlayerInfo";

const MainPage = () => {
  console.log("HI!");

  return (
    <div className='p-2'>
      <h3>Welcome to BrainFlex!</h3>
      <Leaderboard />
      <PlayerInfo />
    </div>
  );
};

export { MainPage };
