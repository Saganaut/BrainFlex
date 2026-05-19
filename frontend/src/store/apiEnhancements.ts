/**
 * Cross-cutting cache-sync rules for the auto-generated BrainFlexApi.
 *
 * The generated mutations (addElement, moveElement, deleteElement,
 * updateElement, updateDeck) all return the canonical updated DeckDto with
 * presigned `imgUrl` values fully hydrated (the controller runs the same
 * `DeckImageHydrationService.hydrate` on every mutation response that it
 * runs on `getDeck`). We splice that response into the `getDeck` query
 * cache so any subscribed component re-renders without a refetch and
 * without any stale-URL merge dance on the client.
 *
 * This file is imported for its side effect from `store.ts`; do not remove
 * the import there or these mutations will silently fall out of sync.
 *
 * `BrainFlexApi.ts` is regenerated from the OpenAPI schema, so we layer
 * this behavior on top via `enhanceEndpoints` instead of editing the
 * generated file.
 */
import {
  BrainFlex,
  type DeckCollectionDto,
  type DeckDto,
} from "./BrainFlexApi";
import type { RootState } from "./store";

interface CacheSyncApi {
  dispatch: (action: unknown) => unknown;
  getState: () => RootState;
  queryFulfilled: Promise<{ data: unknown }>;
}

const syncDeckCache = async (arg: { id: string }, api: CacheSyncApi) => {
  try {
    const { data } = await api.queryFulfilled;
    api.dispatch(
      BrainFlex.util.upsertQueryData("getDeck", { id: arg.id }, data as DeckDto),
    );
  } catch {
    // Mutation rejected — leave the cache untouched; the failing component
    // is responsible for surfacing the error.
  }
};

/**
 * Cache sync for the deck-collection surface. Every mutation that touches a
 * single collection (metadata edit, deck add / remove) returns the
 * authoritative {@link DeckCollectionDto} summary — splice it into the
 * detail-view cache so /collections/$collectionId re-renders instantly, and
 * patch the same id in any my-collections list pages the user is browsing.
 *
 * Mutations that return the summary keep `decks` null on the wire (the list
 * endpoint stays cheap). When patching the detail cache we preserve the
 * locally-hydrated `decks` array unless the server fills it in — this keeps
 * the drag-reorder UI smooth without a follow-up refetch.
 */
interface CollectionSyncApi {
  dispatch: (action: unknown) => unknown;
  getState: () => RootState;
  queryFulfilled: Promise<{ data: DeckCollectionDto }>;
}

const syncCollectionCaches = async (
  collectionId: string | undefined,
  api: CollectionSyncApi,
) => {
  if (collectionId == null) return;
  try {
    const { data } = await api.queryFulfilled;
    api.dispatch(
      BrainFlex.util.updateQueryData(
        "getCollection",
        { id: collectionId },
        (draft) => {
          const preservedDecks = draft.decks;
          Object.assign(draft, data);
          if (data.decks == null) draft.decks = preservedDecks;
        },
      ),
    );
    // Patch any materialized my-collections list page. We don't know which
    // pages the user has visited, so we hit a small range — enough for the
    // common case without thrashing every cache key.
    for (let p = 0; p < 5; p++) {
      api.dispatch(
        BrainFlex.util.updateQueryData(
          "listMyCollections",
          { page: p, size: 24 },
          (draft) => {
            if (!draft.items) return;
            for (let i = 0; i < draft.items.length; i++) {
              if (draft.items[i].id === data.id) draft.items[i] = data;
            }
          },
        ),
      );
    }
  } catch {
    // Mutation rejected — leave the cache untouched.
  }
};

/**
 * Optimistic deck-reorder inside a collection: rewrites
 * {@link DeckCollectionDto#deckIds} (and the embedded {@code decks} array)
 * immediately so the drag-released list doesn't snap back while the round-
 * trip is in flight. On reject, undo the patch.
 */
interface ReorderApi {
  dispatch: (action: unknown) => unknown;
  getState: () => RootState;
  queryFulfilled: Promise<{ data: DeckCollectionDto }>;
}

const optimisticReorderCollectionDecks = async (
  arg: {
    id: string;
    reorderCollectionDecksRequest: { deckIds: string[] };
  },
  api: ReorderApi,
) => {
  const nextOrder = arg.reorderCollectionDecksRequest.deckIds;
  const patch = api.dispatch(
    BrainFlex.util.updateQueryData(
      "getCollection",
      { id: arg.id },
      (draft) => {
        draft.deckIds = [...nextOrder];
        if (draft.decks) {
          const byId = new Map(draft.decks.map((d) => [d.id ?? "", d]));
          const reordered: typeof draft.decks = [];
          for (const id of nextOrder) {
            const deck = byId.get(id);
            if (deck) reordered.push(deck);
          }
          draft.decks = reordered;
        }
      },
    ),
  ) as { undo: () => void };
  try {
    await api.queryFulfilled;
  } catch {
    patch.undo();
  }
};

BrainFlex.enhanceEndpoints({
  endpoints: {
    addElement: {
      onQueryStarted: (arg, api) => syncDeckCache(arg, api),
    },
    moveElement: {
      onQueryStarted: (arg, api) => syncDeckCache(arg, api),
    },
    moveMcqOption: {
      onQueryStarted: (arg, api) => syncDeckCache(arg, api),
    },
    deleteElement: {
      onQueryStarted: (arg, api) => syncDeckCache(arg, api),
    },
    updateElement: {
      onQueryStarted: (arg, api) => syncDeckCache(arg, api),
    },
    updateDeck: {
      onQueryStarted: (arg, api) => syncDeckCache(arg, api),
    },
    // Publish lifecycle mutations also return the canonical DeckDto, so the
    // status pill in the editor navbar updates instantly without a refetch.
    publishDeck: {
      onQueryStarted: (arg, api) => syncDeckCache(arg, api),
    },
    unpublishDeck: {
      onQueryStarted: (arg, api) => syncDeckCache(arg, api),
    },
    archiveDeck: {
      onQueryStarted: (arg, api) => syncDeckCache(arg, api),
    },
    updateCollection: {
      onQueryStarted: (arg, api) => syncCollectionCaches(arg.id, api),
    },
    addDeckToCollection: {
      onQueryStarted: (arg, api) => syncCollectionCaches(arg.id, api),
    },
    removeDeckFromCollection: {
      onQueryStarted: (arg, api) => syncCollectionCaches(arg.id, api),
    },
    reorderCollectionDecks: {
      onQueryStarted: (arg, api) => optimisticReorderCollectionDecks(arg, api),
    },
  },
});
