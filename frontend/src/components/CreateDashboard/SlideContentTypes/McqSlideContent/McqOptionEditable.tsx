/**
 * Full author surface for a single McqOption inside an MCQ slide.
 *
 * Layout:
 *   - Card body (always visible): index pill + popover-trigger button at the
 *     top, the option-text input below, and the "Correct" Toggle at the
 *     bottom. Anything beyond text/correct lives in the popover.
 *   - Popover (opened by clicking the trigger; dismissed by clicking outside
 *     or pressing Escape): image controls (gallery picker + paste-URL + clear),
 *     a color swatch, and the "remove option" button. Built on the shared
 *     `Popover` primitive so it stays visually consistent with the
 *     RichTextInput toolbar and any future inline-edit popovers.
 *
 * All writes route through `useMcqOptionEditor`, which owns one shared
 * debounce timer for this option and exposes:
 *   - field-level `schedule` / `commit` / `flush` (text/image/color)
 *   - option-scoped parent ops `toggleCorrect()` / `remove()`
 * Question-level concerns (prompt, addOption) live on
 * `useMcqQuestionEditor`, used by the parent.
 *
 * --- Image field: gallery vs URL ----------------------------------------
 *
 * McqOption stores two mutually-exclusive image references (validated on
 * save in `DeckService.validateOptionImages`):
 *   - `galleryImageId` — the stable reference to a `gallery_images` doc.
 *     The presigned `imageUrl` is computed at read time by
 *     DeckImageHydrationService, so authors who pick from the gallery get
 *     URLs that never go stale.
 *   - `imageUrl` — externally-hosted URL pasted by the author. Used
 *     verbatim; no hydration.
 *
 * The picker callback hands us BOTH the gallery id and a freshly-signed
 * URL. We commit `{ galleryImageId, imageUrl: undefined }` to the backend
 * (only the id is persisted), and concurrently write the picked URL into
 * the `getDeck` cache optimistically so the editor sees the new image
 * immediately. The cache-merge layer in `apiEnhancements.ts` preserves
 * that URL when the mutation response arrives (it would otherwise carry
 * `imageUrl: null` and flash the image away).
 *
 * Pasting a URL takes the opposite path: schedule a commit with
 * `{ galleryImageId: undefined, imageUrl: <typed> }`, which the backend
 * persists as-is.
 */
import { useEffect, useRef, useState } from "react";
import { getRouteApi } from "@tanstack/react-router";
import { TrashIcon } from "@heroicons/react/24/outline";
import {
  BrainFlex,
  type McqOption as McqOptionType,
} from "@/store/BrainFlexApi";
import { useAppDispatch } from "@/store/hooks";
import { Input } from "@/components/Common/Input/Input/Input";
import { IconBtn } from "@/components/Common/Buttons/IconBtn";
import { Toggle } from "@/components/Common/Input/Toggle/Toggle";
import {
  Popover,
  PopoverRow,
  PopoverButton,
  PopoverDivider,
  PopoverGroupLabel,
} from "@/components/Common/Input/Popover/Popover";
import { useGalleryPicker } from "@/hooks/useGalleryPicker";
import { useMcqOptionEditor } from "../useElementEditor";
import styles from "./McqOptionEditable.module.css";
import { EllipsisVerticalIcon } from "@heroicons/react/24/solid";

const routeApi = getRouteApi("/decks/$deckId/edit");

interface McqOptionEditableProps {
  /** Option to edit. Only `id` is read directly — the freshest field
   *  values come from `useMcqOptionEditor` (deck cache), so this prop
   *  serves purely as the lookup key. */
  option: McqOptionType;
}

const McqOptionEditable = ({
  option: initialOption,
}: McqOptionEditableProps) => {
  const optionId = initialOption.id;
  const { deckId } = routeApi.useParams();
  const dispatch = useAppDispatch();
  const openPicker = useGalleryPicker();

  const {
    option,
    parent,
    schedule,
    commit,
    flush,
    index,
    isCorrect,
    canRemove,
    toggleCorrect,
    remove,
    syncedFromId,
    markSynced,
  } = useMcqOptionEditor(optionId);

  // --- local mirrors for debounced fields (text + paste-link URL) -------
  // Color uses a debounced commit while dragging; no separate local mirror
  // is needed because the native <input type="color"> owns the swatch DOM.
  const [text, setText] = useState(option?.text ?? "");
  const [pasteUrl, setPasteUrl] = useState(
    option?.galleryImageId ? "" : (option?.imageUrl ?? ""),
  );

  // Popover open state. Dismissed on outside pointerdown / Escape so the
  // popover behaves like the rest of the app's floating surfaces (HuePicker,
  // Dropdown).
  const [popoverOpen, setPopoverOpen] = useState(false);
  const cardRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!popoverOpen) return;
    const handlePointerDown = (e: PointerEvent) => {
      if (!cardRef.current?.contains(e.target as Node)) setPopoverOpen(false);
    };
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") setPopoverOpen(false);
    };
    document.addEventListener("pointerdown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("pointerdown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [popoverOpen]);

  // Resync local mirrors when the active option id changes (slide switch
  // or option re-order). React's "derive state during render" pattern.
  if (option && syncedFromId !== option.id) {
    markSynced(option.id);
    setText(option.text ?? "");
    setPasteUrl(option.galleryImageId ? "" : (option.imageUrl ?? ""));
  }

  if (!option || !optionId) {
    return null;
  }

  // --- handlers --------------------------------------------------------

  const handleTextChange = (next: string) => {
    setText(next);
    schedule({ ...option, text: next });
  };

  /** Pick from the gallery: persist only `galleryImageId`, write the
   *  freshly-signed URL into the local cache optimistically so the editor
   *  sees the new image without waiting for the round trip. The cache-
   *  merge layer in apiEnhancements then preserves that URL when the
   *  mutation response (which carries `imageUrl: null`) lands.
   *  Closes the popover up front — the gallery modal takes over, and the
   *  outside-click handler would otherwise close it as soon as the modal
   *  swallows pointer events. */
  const handlePickFromGallery = () => {
    flush();
    setPopoverOpen(false);
    openPicker(({ galleryImageId, imageUrl }) => {
      if (!parent?.id) return;

      // Optimistic cache write: keyed lookup into the right deck/element/option.
      dispatch(
        BrainFlex.util.updateQueryData("getDeck", { id: deckId }, (draft) => {
          if (!draft.elements) return;
          const el = draft.elements.find((e) => e.id === parent.id);
          if (el?.kind !== "McqQuestion") return;
          const opt = el.options?.find((o) => o.id === optionId);
          if (!opt) return;
          opt.galleryImageId = galleryImageId;
          opt.imageUrl = imageUrl;
        }),
      );

      // Persisted write: galleryImageId only. The backend rejects writes
      // that set both fields (mutual exclusion in DeckService).
      commit({
        ...option,
        galleryImageId,
        imageUrl: undefined,
      });
    });
  };

  const handlePasteUrlChange = (next: string) => {
    setPasteUrl(next);
    schedule({
      ...option,
      galleryImageId: undefined,
      imageUrl: next,
    });
  };

  const handleClearImage = () => {
    flush();
    setPasteUrl("");
    commit({
      ...option,
      galleryImageId: undefined,
      imageUrl: undefined,
    });
  };

  const handleColorChange = (next: string) => {
    // Color picker fires on every drag tick; debounce the writes.
    schedule({ ...option, color: next });
  };

  const handleRemove = () => {
    setPopoverOpen(false);
    remove();
  };

  // --- derived display state ------------------------------------------

  const previewUrl = option.imageUrl ?? "";
  const hasImage = previewUrl.trim() !== "" || !!option.galleryImageId;
  const inputIdBase = `mcq-opt-${optionId}`;
  const displayIndex = index >= 0 ? index + 1 : 0;

  // Clicking anywhere on the card toggles the popover EXCEPT inside the
  // "interactive zones" below (text input + correct toggle), which call
  // `e.stopPropagation()` so their own click never bubbles up here. The
  // ellipsis trigger also stops propagation and toggles directly so it
  // doesn't double-toggle via the card handler.
  const handleCardClick = () => {
    setPopoverOpen((o) => !o);
  };

  return (
    <div
      ref={cardRef}
      className={`${styles.card} ${isCorrect ? styles.cardCorrect : ""}`}
      onClick={handleCardClick}>
      <div className={styles.topRow}>
        <span className={styles.indexPill}>{displayIndex}</span>
      </div>

      <div
        className={styles.interactiveZone}
        onClick={(e) => {
          e.stopPropagation();
        }}>
        <Input
          type='text'
          id={`${inputIdBase}-text`}
          fullWidth
          value={text}
          placeholder='Type the option…'
          onChange={(e) => {
            handleTextChange(e.target.value);
          }}
          onBlur={flush}
        />
      </div>
      <div className={styles.footer}>
        <div
          className={[styles.interactiveZone, styles.correctToggle].join(" ")}
          onClick={(e) => {
            e.stopPropagation();
          }}>
          <Toggle
            label={isCorrect ? "Correct" : "Wrong"}
            id={`mcq-correct-${optionId}`}
            checked={isCorrect}
            onChange={() => {
              toggleCorrect();
            }}
          />
        </div>{" "}
        <IconBtn
          type='default'
          size='xs'
          icon={<EllipsisVerticalIcon />}
          aria-label={`Edit option ${displayIndex.toString()}`}
          aria-expanded={popoverOpen}
          aria-haspopup='dialog'
          onClick={(e) => {
            e.stopPropagation();
            setPopoverOpen((o) => !o);
          }}
        />
      </div>
      {popoverOpen && (
        <div
          className={styles.popoverWrap}
          onClick={(e) => {
            e.stopPropagation();
          }}>
          <Popover
            role='dialog'
            ariaLabel={`Option ${displayIndex.toString()} settings`}>
            <PopoverRow>
              <PopoverGroupLabel>Image</PopoverGroupLabel>
              <button
                type='button'
                className={[
                  styles.imageThumb,
                  hasImage ? "" : styles.imageThumbEmpty,
                ]
                  .filter(Boolean)
                  .join(" ")}
                style={
                  previewUrl
                    ? { backgroundImage: `url(${previewUrl})` }
                    : undefined
                }
                aria-label={hasImage ? "Change image" : "Pick image"}
                onClick={handlePickFromGallery}>
                {!hasImage && <span aria-hidden='true'>+</span>}
              </button>
              {hasImage && (
                <PopoverButton
                  ariaLabel='Clear image'
                  onClick={handleClearImage}>
                  Clear
                </PopoverButton>
              )}

              <PopoverDivider />

              <PopoverGroupLabel>Color</PopoverGroupLabel>
              <input
                type='color'
                id={`${inputIdBase}-color`}
                className={styles.colorSwatch}
                value={option.color ?? "#ffffff"}
                onChange={(e) => {
                  handleColorChange(e.target.value);
                }}
                onBlur={flush}
                aria-label='Option color'
              />

              <PopoverDivider />

              <PopoverButton
                ariaLabel={`Remove option ${displayIndex.toString()}`}
                disabled={!canRemove}
                onClick={handleRemove}>
                <TrashIcon />
              </PopoverButton>
            </PopoverRow>

            <PopoverRow>
              <PopoverGroupLabel>URL</PopoverGroupLabel>
              <input
                type='text'
                id={`${inputIdBase}-url`}
                placeholder='Or paste an image URL'
                value={pasteUrl}
                onChange={(e) => {
                  handlePasteUrlChange(e.target.value);
                }}
                onBlur={flush}
                className={styles.popoverUrlInput}
              />
            </PopoverRow>
          </Popover>
        </div>
      )}
    </div>
  );
};

export { McqOptionEditable };
