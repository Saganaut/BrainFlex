/**
 * Deck-image resolution helpers.
 *
 * Cover and background URLs cascade through several layers; this module is the
 * single place that defines the fallback chain. For now the chain is:
 *   element override (future) → deck value → Lorem Picsum placeholder seeded by id.
 *
 * Theme-driven backgrounds will slot in between "deck" and "Lorem Picsum" once
 * the showcase passes the host's active theme through to clients.
 */

const COVER_WIDTH = 480;
const COVER_HEIGHT = 280;
const BG_WIDTH = 1600;
const BG_HEIGHT = 1000;

const picsumUrl = (seed: string, w: number, h: number): string =>
  `https://picsum.photos/seed/${encodeURIComponent(seed)}/${w}/${h}`;

/** Returns the cover thumbnail URL for a deck, falling back to a deterministic Lorem Picsum. */
export const resolveDeckCover = (
  override: string | null | undefined,
  deckId: string | null | undefined,
): string => {
  if (override && override.trim().length > 0) return override;
  return picsumUrl(`brainflex-deck-cover-${deckId ?? "unknown"}`, COVER_WIDTH, COVER_HEIGHT);
};

/** Returns the showcase background URL, with Lorem Picsum as the placeholder fallback. */
export const resolveShowcaseBackground = (
  deckBackgroundUrl: string | null | undefined,
  deckId: string | null | undefined,
): string => {
  if (deckBackgroundUrl && deckBackgroundUrl.trim().length > 0) return deckBackgroundUrl;
  return picsumUrl(`brainflex-deck-bg-${deckId ?? "unknown"}`, BG_WIDTH, BG_HEIGHT);
};
