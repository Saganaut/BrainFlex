// Opens the GalleryPicker in the global modal with a callers-supplied pick
// handler. Wraps the modal API so slide editors can pull in image-picking
// behaviour with a single call:
//
//     const openPicker = useGalleryPicker();
//     openPicker(({ galleryImageId, imageUrl }) => {
//       commitOption({ ...option, galleryImageId, imageUrl: null });
//     });
//
// `galleryImageId` is the stable reference the backend persists (so deck
// documents don't rot when presigned URLs expire). `imageUrl` is the
// freshly-signed URL the picker already had in hand — callers can use it
// for immediate visual feedback, but mustn't persist it alongside the id
// (the DeckService mutual-exclusion validator rejects writes that set
// both). Treat it as a read-time hint, not a write-time field.
//
// The picker self-closes after onPick is invoked.
import { useCallback } from "react";
import { useModal } from "@/context/useModal";
import { GalleryPicker } from "@/components/Common/GalleryPicker/GalleryPicker";

interface PickedImage {
  /**
   * Gallery image id from the gallery_images collection. Undefined when
   * the picker doesn't have an id to surface (e.g. legacy code paths) —
   * callers that require an id should treat that as a no-op.
   */
  galleryImageId?: string;
  /**
   * Fresh presigned S3 URL for the picked image. Display-only — do not
   * persist this alongside `galleryImageId`.
   */
  imageUrl: string;
}

type PickHandler = (picked: PickedImage) => void;

const useGalleryPicker = (): ((onPick: PickHandler) => void) => {
  const { openModal, closeModal } = useModal();

  return useCallback(
    (onPick: PickHandler) => {
      openModal({
        title: "Choose an image",
        content: (
          <GalleryPicker
            onPick={(picked) => {
              onPick(picked);
              closeModal();
            }}
            onClose={closeModal}
          />
        ),
      });
    },
    [openModal, closeModal],
  );
};

export { useGalleryPicker };
