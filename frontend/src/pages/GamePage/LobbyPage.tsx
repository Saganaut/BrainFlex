import { Lobby } from "../../components/Games/Lobby";
import { getRouteApi } from "@tanstack/react-router";
const routeApi = getRouteApi("/games/$roomCode/lobby");

const LobbyPage = () => {
  const { roomCode } = routeApi.useParams();
  return <Lobby roomCode={roomCode} />;
};

export { LobbyPage };
