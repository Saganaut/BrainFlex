import { SessionLobbyPage } from "../../../pages/SessionLobbyPage/SessionLobbyPage";
import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/sessions/$sessionId/lobby")({
  component: SessionLobbyPage,
});
