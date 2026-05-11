// Pulse create route — audience polling creation entry, currently stubbed.
import { createFileRoute } from "@tanstack/react-router";
import { PulseCreatePage } from "../../pages/PulsePage/PulseCreatePage";

export const Route = createFileRoute("/pulse/create")({
  component: PulseCreatePage,
});
