// Opens the GalleryPicker in the global modal with a caller-supplied pick
// handler. The picker hands back a fully-populated `Image` (the unified
// wire shape) — internal type, gallery id, and the picker's already-fresh
// presigned URL all in one value. Callers drop it directly into whatever
// slot they're editing:
//
//     const openPicker = useGalleryPicker();
//     openPicker((image) => {
//       commitOption({ ...option, image });
//     });
//
// The backend strips `imgUrl` on write for internal images and rehydrates
// it on read, so there's no field-by-field merge dance for callers.
//
// The picker self-closes after onPick is invoked.
import { useCallback } from "react";
import { useModal } from "@/context/useModal";
import { GalleryPicker } from "@/components/Common/GalleryPicker/GalleryPicker";
import type { Image } from "@/store/BrainFlexApi";

type PickHandler = (image: Image) => void;

const useGalleryPicker = (): ((onPick: PickHandler) => void) => {
  const { openModal, closeModal } = useModal();

  return useCallback(
    (onPick: PickHandler) => {
      openModal({
        title: "Choose an image",
        content: (
          <GalleryPicker
            onPick={(image) => {
              onPick(image);
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
