/**
 * Lobby route (/games/$roomCode/lobby).
 * Mounts the Lobby component, which manages the WebSocket connection and
 * redirects to /play when the host starts the game.
 */
import { createFileRoute } from "@tanstack/react-router";
import { Lobby } from "../../../components/Games/Lobby";

export const Route = createFileRoute("/games/$roomCode/lobby")({
  component: LobbyPage,
});

function LobbyPage() {
  const { roomCode } = Route.useParams();
  return <Lobby roomCode={roomCode} />;
}
