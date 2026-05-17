/**
 * Cross-cutting cache-sync rules for the auto-generated BrainFlexApi.
 *
 * The generated mutations (addElement, moveElement, deleteElement,
 * updateElement, updateDeck) all return the canonical updated DeckDto. We
 * splice that response into the `getDeck` query cache so any subscribed
 * component re-renders without a manual refetch. This file is imported for
 * its side effect from `store.ts`; do not remove the import there or these
 * mutations will silently fall out of sync.
 *
 * `BrainFlexApi.ts` is regenerated from the OpenAPI schema, so we layer
 * this behavior on top via `enhanceEndpoints` instead of editing the
 * generated file.
 *
 * --- Gallery-image URL preservation ---------------------------------------
 *
 * Why this is here at all:
 *   The backend persists only `galleryImageId` on `McqOption` — the
 *   `imageUrl` for gallery-backed options is a presigned S3 URL that is
 *   computed at READ time by `DeckImageHydrationService` and returned on
 *   `GET /api/decks/{id}` only. Mutation endpoints intentionally do NOT
 *   hydrate (refreshing 50 HMACs and hitting `gallery_images` on every
 *   keystroke would burn cycles for no UX benefit — presigned URLs are
 *   valid for 7 days per `S3Properties.presignedUrlExpiry`).
 *
 * The problem this creates:
 *   The mutation response carries `galleryImageId: <id>, imageUrl: null`
 *   for every gallery-backed option. Splicing that straight into the
 *   `getDeck` cache wipes the URLs the editor was just rendering — every
 *   gallery image on the page flashes to a broken/empty state until the
 *   next full `getDeck` round trip.
 *
 * The fix:
 *   Before splicing, copy each *old* cached option's hydrated `imageUrl`
 *   onto its corresponding *new* option. We key the lookup on the tuple
 *   (elementId, optionId, galleryImageId) so that:
 *     - A pure non-image edit (text/color/correct/etc.) keeps the same
 *       gallery id → match → URL preserved.
 *     - An image *swap* (author picks a different gallery image) changes
 *       galleryImageId → no match → new option keeps `imageUrl: null`,
 *       and the next `getDeck` (slide switch / reopen) repopulates it.
 *     - A swap to a paste-link URL sets `imageUrl` directly in the response
 *       → we skip the merge for that option (predicate below).
 *
 * Limits:
 *   If the editor stays open continuously for 7+ days without ever
 *   triggering a fresh `getDeck`, the preserved URLs would eventually
 *   expire. Any slide switch, deck reopen, or browser refresh refetches and
 *   re-hydrates, so realistic editing sessions never hit this.
 */
import type { RootState } from "./store";
import { BrainFlex, type DeckDto, type McqOption } from "./BrainFlexApi";

type DeckElement = NonNullable<DeckDto["elements"]>[number];

interface CacheSyncApi {
  dispatch: (action: unknown) => unknown;
  getState: () => RootState;
  queryFulfilled: Promise<{ data: unknown }>;
}

/**
 * Pull options off an element if the element kind owns an option list.
 * Returns `undefined` for slides and non-option questions.
 */
const optionsOf = (el: DeckElement): McqOption[] | undefined => {
  if (el.kind === "McqQuestion") {
    return el.options;
  }
  return undefined;
};

/**
 * Build a `(elementId::optionId::galleryImageId) → imageUrl` index from the
 * previously-cached deck, restricted to options that actually carry a
 * hydrated URL paired with a gallery id (the only case worth carrying
 * forward).
 */
const indexHydratedUrls = (deck: DeckDto | undefined): Map<string, string> => {
  const map = new Map<string, string>();
  if (!deck?.elements) return map;
  for (const el of deck.elements) {
    const options = optionsOf(el);
    if (!options || !el.id) continue;
    for (const opt of options) {
      if (opt.id && opt.galleryImageId && opt.imageUrl) {
        map.set(`${el.id}::${opt.id}::${opt.galleryImageId}`, opt.imageUrl);
      }
    }
  }
  return map;
};

/**
 * Return a structurally-equal copy of `newDeck` with gallery-backed option
 * URLs reinstated from `oldDeck` where the (element, option, image) tuple
 * is unchanged. Pure — neither input is mutated.
 */
const preserveGalleryUrls = (
  newDeck: DeckDto,
  oldDeck: DeckDto | undefined,
): DeckDto => {
  if (!newDeck.elements) return newDeck;
  const oldUrlByKey = indexHydratedUrls(oldDeck);
  if (oldUrlByKey.size === 0) return newDeck;

  const mergedElements = newDeck.elements.map((el) => {
    const options = optionsOf(el);
    if (!options || !el.id) return el;

    const mergedOptions = options.map((opt) => {
      // Skip when:
      //   - option has no id (shouldn't happen post-save but be defensive)
      //   - option isn't gallery-backed (paste-link URL or no image)
      //   - response already carries a URL (e.g. another author hydrated it
      //     via getDeck before our mutation landed)
      if (!opt.id || !opt.galleryImageId || opt.imageUrl) return opt;
      const elementId = el.id ?? "";
      const oldUrl = oldUrlByKey.get(
        `${elementId}::${opt.id}::${opt.galleryImageId}`,
      );
      if (!oldUrl) return opt;
      return { ...opt, imageUrl: oldUrl };
    });

    return { ...el, options: mergedOptions };
  });

  return { ...newDeck, elements: mergedElements };
};

const syncDeckCache = async (arg: { id: string }, api: CacheSyncApi) => {
  try {
    const { data } = await api.queryFulfilled;
    const newDeck = data as DeckDto;
    const oldDeck = BrainFlex.endpoints.getDeck.select({ id: arg.id })(
      api.getState(),
    ).data;
    const merged = preserveGalleryUrls(newDeck, oldDeck);
    api.dispatch(
      BrainFlex.util.upsertQueryData("getDeck", { id: arg.id }, merged),
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
