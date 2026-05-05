/**
 * Game hub route (/games).
 * Entry point for all game activity: browse packs, create a new session
 * as a registered user, or join an existing session via room code.
 */
import { createFileRoute } from "@tanstack/react-router";
import { GameHub } from "../../components/Games/GameHub";

export const Route = createFileRoute("/games/")({
  component: GameHub,
});
