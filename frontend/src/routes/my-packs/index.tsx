/**
 * My Packs route (/my-packs).
 * Lists user-owned content packs alongside system packs.
 * Registered users can create, edit, and delete their own packs.
 */
import { createFileRoute } from "@tanstack/react-router";
import { MyPacksPage } from "../../pages/MyPacksPage/MyPacksPage";

export const Route = createFileRoute("/my-packs/")({
  component: MyPacksPage,
});
