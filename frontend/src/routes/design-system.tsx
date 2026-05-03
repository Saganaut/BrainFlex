import { createFileRoute } from "@tanstack/react-router";
import { DesignSystem } from "../pages/DesignSystemPage";

export const Route = createFileRoute("/design-system")({
  component: DesignSystem,
});
