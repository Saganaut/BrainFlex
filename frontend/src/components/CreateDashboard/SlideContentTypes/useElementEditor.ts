/**
 * Shared plumbing for slide-content editors. Each kind-specific editor owns
 * its own local-state shape, but the read/write boundary is identical: pull
 * the active element from the deck cache, debounce a commit, and flush on
 * blur / structural change. This hook centralises that.
 *
 * Returns:
 *   - element        — the active element, narrowed to T by the caller's
 *                      `selectKind` predicate; undefined while loading or if
 *                      the active element is of a different kind.
 *   - schedule(p)    — debounced PUT /elements/{id}.
 *   - flush()        — fire the buffered commit now (use on blur).
 *   - commit(p)      — fire immediately (use for structural changes —
 *                      add/remove option, etc.).
 *   - syncedFromId   — last element.id we synced local state from. Editors
 *                      compare this against `element.id` during render to
 *                      decide whether to reset their local mirror.
 *   - markSynced(id) — update syncedFromId after a successful resync.
 */
import { useState } from "react";
import { getRouteApi } from "@tanstack/react-router";
import {
  useGetDeckQuery,
  useUpdateElementMutation,
  type DeckDto,
} from "@/store/BrainFlexApi";
import { useDebouncedCommit } from "@/hooks/useDebouncedCommit";

type DeckElement = NonNullable<DeckDto["elements"]>[number];

const routeApi = getRouteApi("/decks/$deckId/view");

interface ElementEditorApi<T extends DeckElement> {
  element: T | undefined;
  schedule: (patch: T) => void;
  flush: () => void;
  commit: (patch: T) => void;
  syncedFromId: string | undefined;
  markSynced: (id: string | undefined) => void;
}

const useElementEditor = <T extends DeckElement>(
  selectKind: (e: DeckElement) => e is T,
  delay = 500,
): ElementEditorApi<T> => {
  const { deckId } = routeApi.useParams();
  const { questionId } = routeApi.useSearch();

  const { element } = useGetDeckQuery(
    { id: deckId },
    {
      selectFromResult: ({ data }) => {
        const e = data?.elements?.find((el) => el.id === questionId);
        return { element: e && selectKind(e) ? e : undefined };
      },
    },
  );

  const [updateElement] = useUpdateElementMutation();

  const commit = (patch: T) => {
    if (!element?.id) return;
    void updateElement({
      id: deckId,
      elementId: element.id,
      body: patch,
    })
      .unwrap()
      .catch((err: unknown) => {
        console.error("Failed to update element", err);
      });
  };

  const { schedule, flush } = useDebouncedCommit<T>(commit, delay);

  const [syncedFromId, setSyncedFromId] = useState<string | undefined>(
    element?.id,
  );

  return {
    element,
    schedule,
    flush,
    commit,
    syncedFromId,
    markSynced: setSyncedFromId,
  };
};

export { useElementEditor };
