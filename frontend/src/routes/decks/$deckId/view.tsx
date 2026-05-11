import { DeckViewPage } from "../../../pages/DeckViewPage/DeckViewPage";
import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/decks/$deckId/view")({
  component: DeckViewPage,
});
