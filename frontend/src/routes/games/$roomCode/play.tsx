/**
 * Active game route (/games/$roomCode/play).
 * Orchestrates the round lifecycle: countdown timer, question display, answer
 * selection, round-result overlay, and auto-navigation to /results on GAME_OVER.
 */

import { createFileRoute } from "@tanstack/react-router";
import { PlayPage } from "../../../pages/GamePage/PlayPage";

export const Route = createFileRoute("/games/$roomCode/play")({
  component: PlayPage,
});
