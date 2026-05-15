import { MyDecksPage } from "../../pages/MyDecksPage/MyDecksPage";
import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/decks/")({
  component: MyDecksPage,
});
