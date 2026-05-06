/**
 * Lobby route (/games/$roomCode/lobby).
 * Mounts the Lobby component, which manages the WebSocket connection and
 * redirects to /play when the host starts the game.
 */
import { createFileRoute } from "@tanstack/react-router";
import { LobbyPage } from "../../../pages/GamePage/LobbyPage";

export const Route = createFileRoute("/games/$roomCode/lobby")({
  component: LobbyPage,
});
