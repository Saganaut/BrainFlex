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
import {
  BrainFlex,
  type McqOption as McqOptionType,
} from "@/store/BrainFlexApi";
import { useAppDispatch } from "@/store/hooks";
import { Input } from "@/components/Common/Input/Input/Input";
import { IconBtn } from "@/components/Common/Buttons/IconBtn";
import { Toggle } from "@/components/Common/Input/Toggle/Toggle";

import { useGalleryPicker } from "@/hooks/useGalleryPicker";
import { useTheme } from "@/hooks/useTheme";
import { useMcqOptionEditor } from "../useElementEditor";
import styles from "./McqOptionEditable.module.css";
import { EllipsisVerticalIcon } from "@heroicons/react/24/solid";
import { ProgressBar } from "@/components/Common/ProgressBar/ProgressBar";
import { EditOptionToolbar } from "./EditOptionToolbar";

const routeApi = getRouteApi("/decks/$deckId/edit");

// Six swatches spaced evenly around the wheel from the theme's primary hue.
// Constant lightness/chroma keeps them visually balanced and re-themes
// cascade automatically. Authors can still override per-option via the
// color swatch in the popover (`option.color`).
const OPTION_HUE_OFFSETS = [0, 60, 120, 180, 240, 300] as const;
const MAX_OPTION_COLORS = OPTION_HUE_OFFSETS.length;
const buildOptionPalette = (huePrimary: number): string[] =>
  OPTION_HUE_OFFSETS.map(
    (offset) => `oklch(0.65 0.18 ${((huePrimary + offset) % 360).toString()})`,
  );

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
  const { huePrimary } = useTheme();

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
      // that set both fields (mutual exclusion in DeckService). Build the
      // option field-by-field rather than spreading + clearing imageUrl:
      // `option` already carries a hydrated presigned URL (apiEnhancements
      // re-merges it onto every mutation response), and that URL leaks
      // through the spread before the JSON.stringify of `imageUrl: undefined`
      // can drop it.
      commit({
        id: option.id,
        text: option.text,
        color: option.color,
        galleryImageId,
      });
    });
  };

  const handlePasteUrlChange = (next: string) => {
    setPasteUrl(next);
    // Explicit construction so a stale `galleryImageId` on `option` (carried
    // from a prior gallery pick) can't leak through alongside the new URL —
    // the backend's mutual-exclusion validator rejects writes that set both.
    schedule({
      id: option.id,
      text: option.text,
      color: option.color,
      imageUrl: next,
    });
  };

  const handleClearImage = () => {
    flush();
    setPasteUrl("");
    // Explicit construction so neither image field carries through from
    // `option` — same reasoning as above.
    commit({
      id: option.id,
      text: option.text,
      color: option.color,
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
  // Theme-derived default; only applied when the author hasn't overridden
  // via the popover swatch. Indexes past MAX_OPTION_COLORS wrap.
  const palette = buildOptionPalette(huePrimary);
  const paletteIndex = (index >= 0 ? index : 0) % MAX_OPTION_COLORS;
  const paletteColor = palette[paletteIndex] ?? palette[0];
  const color = option.color ?? paletteColor;
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
        <div>
          <span className={styles.indexPill}>{displayIndex}</span>
          <div
            className={styles.interactiveZone}
            onClick={(e) => {
              e.stopPropagation();
            }}>
            <Input
              isBordered={false}
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
        </div>
        <div className={styles.imgThumbnail}>
          <img src={option.imageUrl ?? option.galleryImageId} />
        </div>
      </div>

      <ProgressBar value={100} color={color} />
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
        <EditOptionToolbar
          canRemove={canRemove}
          pasteUrl={pasteUrl}
          handlePickFromGallery={handlePickFromGallery}
          hasImage={hasImage}
          handleRemove={handleRemove}
          handleClearImage={handleClearImage}
          handleColorChange={handleColorChange}
          handlePasteUrlChange={handlePasteUrlChange}
          displayIndex={displayIndex}
          previewUrl={previewUrl}
          option={option}
          inputIdBase={inputIdBase}
          flush={flush}
        />
      )}
    </div>
  );
};

export { McqOptionEditable };
