/**
 * Helpers around the wire `Image` shape.
 *
 * The backend models every image-bearing field with the same record:
 *   { useExternalImg, internalImgId, imgUrl }
 * — see `model/element/Image.java`. `useExternalImg` decides the source of
 * truth: external (author pasted a URL) holds the URL in `imgUrl`; internal
 * (gallery-backed) holds the gallery id in `internalImgId` and the backend
 * rehydrates `imgUrl` from S3 on every read.
 *
 * The constructors below produce the standard "blank / external / internal"
 * shapes so callers don't reach into the object literal. `displayUrl` is the
 * single place renderers go to turn an `Image | undefined` into an actual
 * <img src=…> value (falling back to a seeded Lorem Picsum placeholder).
 */
import type { Image } from "@/store/BrainFlexApi";

const PLACEHOLDER_W = 640;
const PLACEHOLDER_H = 360;

export const externalImage = (url: string): Image => ({
  useExternalImg: true,
  internalImgId: "",
  imgUrl: url,
});

export const internalImage = (
  galleryImageId: string,
  hydratedUrl = "",
): Image => ({
  useExternalImg: false,
  internalImgId: galleryImageId,
  imgUrl: hydratedUrl,
});

export const emptyImage = (): Image => ({
  useExternalImg: true,
  internalImgId: "",
  imgUrl: "",
});

export const isImageEmpty = (img: Image | null | undefined): boolean =>
  !img?.imgUrl || img.imgUrl.trim() === "";

/** Seeded Lorem Picsum placeholder used when an image slot is empty. */
export const placeholderImageUrl = (
  seed: string,
  w: number = PLACEHOLDER_W,
  h: number = PLACEHOLDER_H,
): string => `https://picsum.photos/seed/${encodeURIComponent(seed)}/${w}/${h}`;

/**
 * Resolve an `Image | undefined` to a renderable URL. Falls back to a Lorem
 * Picsum placeholder seeded on `seed` (typically the parent element/option
 * id) so empty slots stay stable across re-renders.
 */
export const displayUrl = (
  img: Image | null | undefined,
  seed: string,
  w?: number,
  h?: number,
  includePlaceholder = false,
): string | null => {
  const url = img?.imgUrl;

  const placeHolderOrNot = includePlaceholder
    ? placeholderImageUrl(seed, w, h)
    : null;
  return url && url.trim() !== "" ? url : placeHolderOrNot;
};
