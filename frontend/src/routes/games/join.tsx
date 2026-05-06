/**
 * Join game route (/games/join).
 * Any authenticated user (registered or guest) can enter a 6-character room
 * code here. On success, redirects to the lobby for that session.
 */

import { createFileRoute } from "@tanstack/react-router";

import { JoinGamePage } from "../../pages/GamePage/JoinGamePage";

export const Route = createFileRoute("/games/join")({
  component: JoinGamePage,
});
