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
import { BrainFlex, type DeckDto } from "./BrainFlexApi";
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
  },
});
