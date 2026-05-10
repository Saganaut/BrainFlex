/**
 * Create pack route (/my-packs/create).
 * Opens the pack editor in create mode. An optional `returnTo` search param
 * redirects back to the caller (e.g. /games/create) after the pack is saved.
 */
import { createFileRoute } from "@tanstack/react-router";
import { z } from "zod";
import { PackEditorPage } from "../../pages/MyPacksPage/PackEditorPage";

const searchSchema = z.object({
  returnTo: z.string().optional(),
});

export const Route = createFileRoute("/my-packs/create")({
  validateSearch: searchSchema,
  component: function CreatePackRoute() {
    const { returnTo } = Route.useSearch();
    return <PackEditorPage returnTo={returnTo} />;
  },
});
