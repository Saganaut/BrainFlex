/**
 * Results route (/games/$roomCode/results).
 * Shows the final placement table after a game ends. Uses the GAME_OVER
 * payload already in Redux state; falls back to GET /api/games/{roomCode}/results
 * if the user arrived here directly (e.g. after a page refresh).
 */
import { createFileRoute } from "@tanstack/react-router";
import { ResultsPage } from "../../../pages/GamePage/ResultsPage";

export const Route = createFileRoute("/games/$roomCode/results")({
  component: ResultsPage,
});
