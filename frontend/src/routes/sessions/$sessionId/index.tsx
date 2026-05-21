import { SessionPage } from "../../../pages/SessionPage/SessionPage";
import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/sessions/$sessionId/")({
  component: SessionPage,
});
