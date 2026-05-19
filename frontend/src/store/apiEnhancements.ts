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
  type DeckCollaboratorDto,
  type DeckCollectionDto,
  type DeckCommentDto,
  type DeckDto,
  type ExploreDecksApiArg,
  type ListCommentsApiArg,
  type ListMyFavoritesApiArg,
  type ListRepliesApiArg,
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

/**
 * After a collaborator mutation, splice the new row into the listCollaborators
 * cache for {deckId} so the Share modal updates without a refetch. For
 * removes/role updates we re-fetch to pick up the full list state — the
 * mutation response is either void or a single row, which isn't enough to
 * rebuild the list locally for cases like transferOwnership that touch two
 * rows.
 */
interface CollaboratorSyncApi {
  dispatch: (action: unknown) => unknown;
  getState: () => RootState;
  queryFulfilled: Promise<{ data: unknown }>;
}

const refetchCollaborators = (deckId: string, api: CollaboratorSyncApi) => {
  void api.queryFulfilled.then(
    () => {
      api.dispatch(
        BrainFlex.endpoints.listCollaborators.initiate(
          { id: deckId },
          { subscribe: false, forceRefetch: true },
        ),
      );
    },
    () => {
      // Mutation rejected — leave the cache alone.
    },
  );
};

const upsertCollaboratorRow = async (
  deckId: string,
  api: CollaboratorSyncApi,
) => {
  try {
    const { data } = await api.queryFulfilled;
    const next = data as DeckCollaboratorDto;
    api.dispatch(
      BrainFlex.util.updateQueryData(
        "listCollaborators",
        { id: deckId },
        (draft) => {
          const idx = draft.findIndex((r) => r.userId === next.userId
            && next.userId != null);
          if (idx >= 0) draft[idx] = next;
          else draft.push(next);
        },
      ),
    );
  } catch {
    // ignore
  }
};

/**
 * Optimistic favorite/unfavorite toggle. Flips {@code isFavorited} and adjusts
 * {@code favoriteCount} on every cached representation of the deck so the
 * heart and counter update before the round-trip completes. Reconciles on
 * fulfillment with the authoritative counter from the server, and rolls
 * everything back on reject.
 *
 * Covered caches:
 *  - {@code getDeck} for the deck itself
 *  - {@code listDecks} and {@code listMyDecks} (void-arg lists)
 *  - every materialized {@code exploreDecks} and {@code listMyFavorites}
 *    page (args parsed back out of {@code state.api.queries}); when
 *    unfavoriting we also drop the deck from {@code listMyFavorites} pages
 *    because the server would no longer return it
 */
interface ApiQueryEntry {
  endpointName?: string;
  originalArgs?: unknown;
}

const adjustDeckRow = (deck: DeckDto, desiredIsFavorited: boolean) => {
  deck.isFavorited = desiredIsFavorited;
  const current = deck.favoriteCount ?? 0;
  const next = desiredIsFavorited ? current + 1 : current - 1;
  deck.favoriteCount = Math.max(0, next);
};

const optimisticToggleFavorite = async (
  arg: { id: string },
  api: CacheSyncApi,
  desiredIsFavorited: boolean,
) => {
  const patches: { undo: () => void }[] = [];
  const patch = (action: unknown) => {
    patches.push(api.dispatch(action) as { undo: () => void });
  };

  patch(
    BrainFlex.util.updateQueryData("getDeck", { id: arg.id }, (draft) => {
      if (draft.id === arg.id) adjustDeckRow(draft, desiredIsFavorited);
    }),
  );

  const patchListInPlace = (
    endpointName: "listDecks" | "listMyDecks",
  ) => {
    patch(
      BrainFlex.util.updateQueryData(endpointName, undefined, (draft) => {
        for (const deck of draft) {
          if (deck.id === arg.id) adjustDeckRow(deck, desiredIsFavorited);
        }
      }),
    );
  };
  patchListInPlace("listDecks");
  patchListInPlace("listMyDecks");

  const queries =
    (api.getState() as unknown as {
      api?: { queries?: Record<string, ApiQueryEntry | undefined> };
    }).api?.queries ?? {};

  for (const entry of Object.values(queries)) {
    if (!entry?.endpointName) continue;
    if (entry.endpointName === "exploreDecks") {
      const queryArg = (entry.originalArgs ?? {}) as ExploreDecksApiArg;
      patch(
        BrainFlex.util.updateQueryData("exploreDecks", queryArg, (draft) => {
          if (!draft.items) return;
          for (const deck of draft.items) {
            if (deck.id === arg.id) adjustDeckRow(deck, desiredIsFavorited);
          }
        }),
      );
    } else if (entry.endpointName === "listMyFavorites") {
      const queryArg = (entry.originalArgs ?? {}) as ListMyFavoritesApiArg;
      patch(
        BrainFlex.util.updateQueryData("listMyFavorites", queryArg, (draft) => {
          if (!draft.items) return;
          if (desiredIsFavorited) {
            for (const deck of draft.items) {
              if (deck.id === arg.id) adjustDeckRow(deck, desiredIsFavorited);
            }
          } else {
            const before = draft.items.length;
            draft.items = draft.items.filter((deck) => deck.id !== arg.id);
            const removed = before - draft.items.length;
            if (removed > 0 && draft.totalElements != null) {
              draft.totalElements = Math.max(0, draft.totalElements - removed);
            }
          }
        }),
      );
    }
  }

  try {
    const { data } = await api.queryFulfilled;
    const response = data as {
      deckId?: string;
      isFavorited?: boolean;
      favoriteCount?: number;
    };
    if (response.favoriteCount == null) return;
    const authoritative = response.favoriteCount;
    api.dispatch(
      BrainFlex.util.updateQueryData("getDeck", { id: arg.id }, (draft) => {
        if (draft.id === arg.id) draft.favoriteCount = authoritative;
      }),
    );
  } catch {
    for (const p of patches) p.undo();
  }
};

/**
 * Optimistic comment-upvote toggle. The server endpoint is idempotent (POSTing
 * twice in a row leaves the row in its original state), so the optimistic flip
 * derives its target state from whichever cache copy of the comment we find
 * first — `listComments` pages for the deck, then `listReplies` pages. Every
 * cached row that matches is patched in lockstep so a comment that appears in
 * both a top-level page (as a parent counted by `replyCount`) and a reply page
 * (as a sibling reply) stays in sync.
 *
 * On fulfillment we splice the authoritative {@link DeckCommentDto} into every
 * matching cache so the counters reconcile to what the server actually wrote;
 * on reject we undo every optimistic patch.
 */
const optimisticToggleCommentUpvote = async (
  arg: { deckId: string; commentId: string },
  api: CacheSyncApi,
) => {
  const queries =
    (api.getState() as unknown as {
      api?: { queries?: Record<string, ApiQueryEntry | undefined> };
    }).api?.queries ?? {};

  // First pass: find a cached copy so we know which way the toggle should go.
  // The endpoint is idempotent on the server, but the optimistic flip needs a
  // concrete next state to write into the cache; without a cached row we let
  // the round-trip happen unoptimistically.
  let nextUpvoted: boolean | null = null;
  for (const entry of Object.values(queries)) {
    if (!entry?.endpointName) continue;
    if (entry.endpointName !== "listComments"
        && entry.endpointName !== "listReplies") continue;
    const cacheKey = `${entry.endpointName}(${JSON.stringify(entry.originalArgs ?? null)})`;
    const cached = (
      api.getState() as unknown as {
        api?: { queries?: Record<string, { data?: { items?: DeckCommentDto[] } }> };
      }
    ).api?.queries?.[cacheKey]?.data;
    const items = cached?.items ?? [];
    const found = items.find((c) => c.id === arg.commentId);
    if (found) {
      nextUpvoted = !(found.upvotedByMe ?? false);
      break;
    }
  }

  const patches: { undo: () => void }[] = [];
  const flipRow = (row: DeckCommentDto, target: boolean) => {
    const current = row.upvotes ?? 0;
    const wasUpvoted = row.upvotedByMe ?? false;
    if (wasUpvoted === target) return;
    row.upvotedByMe = target;
    row.upvotes = Math.max(0, target ? current + 1 : current - 1);
  };

  if (nextUpvoted !== null) {
    const target = nextUpvoted;
    for (const entry of Object.values(queries)) {
      if (!entry?.endpointName) continue;
      if (entry.endpointName === "listComments") {
        const queryArg = (entry.originalArgs ?? {}) as ListCommentsApiArg;
        if (queryArg.id !== arg.deckId) continue;
        patches.push(
          api.dispatch(
            BrainFlex.util.updateQueryData("listComments", queryArg, (draft) => {
              if (!draft.items) return;
              for (const row of draft.items) {
                if (row.id === arg.commentId) flipRow(row, target);
              }
            }),
          ) as { undo: () => void },
        );
      } else if (entry.endpointName === "listReplies") {
        const queryArg = (entry.originalArgs ?? {}) as ListRepliesApiArg;
        if (queryArg.deckId !== arg.deckId) continue;
        patches.push(
          api.dispatch(
            BrainFlex.util.updateQueryData("listReplies", queryArg, (draft) => {
              if (!draft.items) return;
              for (const row of draft.items) {
                if (row.id === arg.commentId) flipRow(row, target);
              }
            }),
          ) as { undo: () => void },
        );
      }
    }
  }

  try {
    const { data } = await api.queryFulfilled;
    const authoritative = data as DeckCommentDto;
    if (authoritative.id == null) return;
    for (const entry of Object.values(queries)) {
      if (!entry?.endpointName) continue;
      if (entry.endpointName === "listComments") {
        const queryArg = (entry.originalArgs ?? {}) as ListCommentsApiArg;
        if (queryArg.id !== arg.deckId) continue;
        api.dispatch(
          BrainFlex.util.updateQueryData("listComments", queryArg, (draft) => {
            if (!draft.items) return;
            for (let i = 0; i < draft.items.length; i++) {
              if (draft.items[i].id === authoritative.id) {
                draft.items[i] = {
                  ...draft.items[i],
                  upvotes: authoritative.upvotes,
                  upvotedByMe: authoritative.upvotedByMe,
                  updatedAt: authoritative.updatedAt,
                };
              }
            }
          }),
        );
      } else if (entry.endpointName === "listReplies") {
        const queryArg = (entry.originalArgs ?? {}) as ListRepliesApiArg;
        if (queryArg.deckId !== arg.deckId) continue;
        api.dispatch(
          BrainFlex.util.updateQueryData("listReplies", queryArg, (draft) => {
            if (!draft.items) return;
            for (let i = 0; i < draft.items.length; i++) {
              if (draft.items[i].id === authoritative.id) {
                draft.items[i] = {
                  ...draft.items[i],
                  upvotes: authoritative.upvotes,
                  upvotedByMe: authoritative.upvotedByMe,
                  updatedAt: authoritative.updatedAt,
                };
              }
            }
          }),
        );
      }
    }
  } catch {
    for (const p of patches) p.undo();
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
    inviteCollaborator: {
      onQueryStarted: (arg, api) => upsertCollaboratorRow(arg.id, api),
    },
    updateCollaboratorRole: {
      onQueryStarted: (arg, api) => upsertCollaboratorRow(arg.id, api),
    },
    removeCollaborator: {
      onQueryStarted: (arg, api) => {
        refetchCollaborators(arg.id, api);
      },
    },
    favoriteDeck: {
      onQueryStarted: (arg, api) => optimisticToggleFavorite(arg, api, true),
    },
    unfavoriteDeck: {
      onQueryStarted: (arg, api) => optimisticToggleFavorite(arg, api, false),
    },
    toggleCommentUpvote: {
      onQueryStarted: (arg, api) => optimisticToggleCommentUpvote(arg, api),
    },
    transferOwnership: {
      onQueryStarted: async (arg, api) => {
        refetchCollaborators(arg.id, api);
        // Ownership change flips the caller's myRole; refetch the deck so the
        // Share button's owner-only controls disappear instantly for the
        // demoted owner.
        try {
          await api.queryFulfilled;
          void api.dispatch(
            BrainFlex.endpoints.getDeck.initiate(
              { id: arg.id },
              { subscribe: false, forceRefetch: true },
            ),
          );
        } catch {
          // Mutation rejected — caller stays owner, no cache change.
        }
      },
    },
  },
});
