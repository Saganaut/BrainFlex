/**
 * Create game route (/games/create).
 * Registered users choose a content deck and configure session settings.
 * On submission, POST /api/games returns the roomCode and the user is
 * redirected to the lobby.
 */

import { createFileRoute } from "@tanstack/react-router";

import { CreateGamePage } from "../../../pages/GamePage/CreateGamePage";

export const Route = createFileRoute("/_authenticated/games/create")({
  component: CreateGamePage,
});
