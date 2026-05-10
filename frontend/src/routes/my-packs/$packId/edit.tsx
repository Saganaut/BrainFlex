/**
 * Edit pack route (/my-packs/$packId/edit).
 * Opens the pack editor for an existing pack. Loads pack metadata and
 * questions from the API so the owner can update them inline.
 */
import { createFileRoute } from "@tanstack/react-router";
import { PackEditorPage } from "../../../pages/MyPacksPage/PackEditorPage";

export const Route = createFileRoute("/my-packs/$packId/edit")({
  component: function EditPackRoute() {
    const { packId } = Route.useParams();
    return <PackEditorPage packId={packId} />;
  },
});
