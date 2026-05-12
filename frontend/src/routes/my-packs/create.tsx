/**
 * Create-pack route (/my-packs/create).
 *
 * Optimistic create flow: the frontend mints a UUID, immediately navigates to
 * /decks/$deckId/view, and fires POST /api/decks in parallel. The deck cache
 * is pre-populated so the editor renders with no flash. Once the server
 * responds, the cache is replaced with the canonical record.
 */
import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useEffect, useRef } from "react";
import { z } from "zod";
import {
  BrainFlex,
  useCreateDeckMutation,
  type DeckDto,
} from "../../store/BrainFlexApi";
import { useAppDispatch } from "../../store/hooks";

const searchSchema = z.object({
  returnTo: z.string().optional(),
});

const buildOptimisticDeck = (id: string, name: string): DeckDto => ({
  id,
  name,
  description: "",
  tags: [],
  isSystem: false,
  visibility: "PRIVATE",
  recommendedPreset: "GAME",
  elementCount: 0,
  elements: [],
});

export const Route = createFileRoute("/my-packs/create")({
  validateSearch: searchSchema,
  component: function CreatePackRoute() {
    const navigate = useNavigate();
    const dispatch = useAppDispatch();
    const [createDeck] = useCreateDeckMutation();
    const startedRef = useRef(false);

    useEffect(() => {
      if (startedRef.current) return;
      startedRef.current = true;

      const id = crypto.randomUUID();
      const name = "Untitled Deck";

      // Seed the cache so the destination editor renders instantly.
      void dispatch(
        BrainFlex.util.upsertQueryData(
          "getDeck",
          { id },
          buildOptimisticDeck(id, name),
        ),
      );

      void navigate({
        to: "/decks/$deckId/view",
        params: { deckId: id },
        search: { questionId: undefined },
        replace: true,
      });

      void createDeck({ createDeckRequest: { id, name } })
        .unwrap()
        .catch((err: unknown) => {
          console.error("Failed to create deck", err);
        });
    }, [createDeck, dispatch, navigate]);

    return <p style={{ padding: "2rem" }}>Creating deck…</p>;
  },
});
