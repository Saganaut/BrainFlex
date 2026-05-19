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
 * Image field: every option carries a single `Image` ({ useExternalImg,
 * internalImgId, imgUrl }). The gallery picker returns a complete Image and
 * we hand it straight to the editor. Pasted URLs become external Images.
 * The backend strips `imgUrl` on write for internal images and rehydrates
 * it on read, so there's nothing to sanitize client-side.
 */
import { useEffect, useRef, useState } from "react";
import { getRouteApi } from "@tanstack/react-router";
import { useSortable } from "@dnd-kit/react/sortable";
import {
  BrainFlex,
  type McqOption as McqOptionType,
} from "@/store/BrainFlexApi";
import { useAppDispatch } from "@/store/hooks";
import { TextArea } from "@/components/Common/Input/TextArea/TextArea";
import { IconBtn } from "@/components/Common/Buttons/IconBtn";

import { useGalleryPicker } from "@/hooks/useGalleryPicker";
import { useTheme } from "@/hooks/useTheme";
import { useFitText } from "@/hooks/useFitText";
import {
  emptyImage,
  externalImage,
  isImageEmpty,
  largestUrl,
  resolveImageUrl,
} from "@/utils/image";
import { useMcqOptionEditor } from "../useElementEditor";
import styles from "./McqOptionEditable.module.css";
import { EllipsisVerticalIcon, XMarkIcon } from "@heroicons/react/24/solid";
import PurpleCheckIcon from "@/assets/common/PurpleCheckIcon.svg";
import { ProgressBar } from "@/components/Common/ProgressBar/ProgressBar";
import { EditOptionToolbar } from "./EditOptionToolbar";
import { Container } from "@/components/Containers/Container";

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
  /** Position in the parent's option list. Forwarded to @dnd-kit's
   *  `useSortable` so the parent's DragDropProvider can reorder. */
  sortIndex: number;
  addOption: () => void;
  canAddOption: boolean;
}

/** The paste-URL input only shows external URLs; gallery picks leave it blank. */
const pasteUrlOf = (image: McqOptionType["image"]): string =>
  image?.useExternalImg ? (largestUrl(image, "") ?? "") : "";

const McqOptionEditable = ({
  option: initialOption,
  sortIndex,
  canAddOption,
  addOption,
}: McqOptionEditableProps) => {
  const optionId = initialOption.id;
  const { deckId } = routeApi.useParams();
  const dispatch = useAppDispatch();
  const openPicker = useGalleryPicker();
  const { huePrimary } = useTheme();

  // dnd-kit sortable: id must be stable per option so DragDropProvider can
  // identify the source on drop. The parent (McqSlideContent) wraps the grid
  // in a DragDropProvider and routes the drop to `handleOptionDragEnd`.
  const { ref: sortableRef, isDragging } = useSortable({
    id: optionId ?? "",
    index: sortIndex,
  });

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
  const [pasteUrl, setPasteUrl] = useState(() => pasteUrlOf(option?.image));

  // Auto-shrink the option text so it fits inside the bounded card
  // (the grid caps row height at `--mcq-option-max-h`). Below 11px the
  // hook stops shrinking and the card clips — at that point the author
  // has way too much text in an answer option anyway.
  const fitRef = useFitText<HTMLTextAreaElement>(text, {
    minPx: 11,
    maxPx: 18,
  });

  // Popover open state. Dismissed on outside pointerdown / Escape so the
  // popover behaves like the rest of the app's floating surfaces (Dropdown).
  const [popoverOpen, setPopoverOpen] = useState(false);
  const cardRef = useRef<HTMLDivElement>(null);

  // Combine @dnd-kit's sortable ref with our local cardRef (used by the
  // outside-click detector below). Same pattern as SlideThumbnail.
  const setCardRef = (node: HTMLDivElement | null) => {
    cardRef.current = node;
    if (typeof sortableRef === "function") sortableRef(node);
  };

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
    setPasteUrl(pasteUrlOf(option.image));
  }

  if (!option || !optionId) {
    return null;
  }

  // --- handlers --------------------------------------------------------

  const handleTextChange = (next: string) => {
    setText(next);
    schedule({ ...option, text: next });
  };

  /** Pick from the gallery: write the picker-supplied Image directly into
   *  both the optimistic cache and the persisted record. The backend will
   *  drop `imgUrl` on save and rehydrate it on read, but the cache write
   *  keeps the freshly-signed URL on screen until the mutation response
   *  (already hydrated by the controller) lands.
   *  Closes the popover up front — the gallery modal takes over, and the
   *  outside-click handler would otherwise close it as soon as the modal
   *  swallows pointer events. */
  const handlePickFromGallery = () => {
    flush();
    setPopoverOpen(false);
    openPicker((image) => {
      if (!parent?.id) return;

      // Optimistic cache write: keyed lookup into the right deck/element/option.
      dispatch(
        BrainFlex.util.updateQueryData("getDeck", { id: deckId }, (draft) => {
          if (!draft.elements) return;
          const el = draft.elements.find((e) => e.id === parent.id);
          if (el?.kind !== "McqQuestion") return;
          const opt = el.options?.find((o) => o.id === optionId);
          if (!opt) return;
          opt.image = image;
        }),
      );

      // Persisted write: the same Image. The paste-URL input also clears
      // because the option is now internal.
      setPasteUrl("");
      commit({ ...option, image });
    });
  };

  const handlePasteUrlChange = (next: string) => {
    setPasteUrl(next);
    schedule({ ...option, image: externalImage(next) });
  };

  const handleClearImage = () => {
    flush();
    setPasteUrl("");
    commit({ ...option, image: emptyImage() });
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

  const previewUrl = largestUrl(option.image, optionId) ?? "";
  const hasImage = !isImageEmpty(option.image);
  const thumbnailSrc = resolveImageUrl(
    option.image,
    "SM",
    optionId,
    200,
    200,
    false,
  );
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
    <Container ref={setCardRef} name='McqOptionCard'>
      <div
        className={`${styles.card} ${isCorrect ? styles.cardCorrect : ""} ${isDragging ? styles.isDragging : ""}`}
        onClick={handleCardClick}>
        <div className={styles.topRow}>
          <div className={styles.textColumn}>
            <span className={styles.indexPill}>{displayIndex}</span>
            <div
              className={styles.interactiveZone}
              onClick={(e) => {
                e.stopPropagation();
              }}>
              <TextArea
                isBordered={false}
                id={`${inputIdBase}-text`}
                fullWidth
                autoGrow={false}
                ref={fitRef}
                rows={1}
                value={text}
                placeholder='Type the option…'
                onChange={(e) => {
                  handleTextChange(e.target.value);
                }}
                onBlur={flush}
              />
            </div>
          </div>
          <div
            className={styles.imgThumbnail}
            style={thumbnailSrc ? {} : { backgroundColor: color }}>
            {thumbnailSrc && <img src={thumbnailSrc} alt='' />}
          </div>
        </div>

        <ProgressBar value={100} color={color} />
        <div className={styles.footer}>
          <button
            type='button'
            className={[styles.interactiveZone, styles.correctBtn].join(" ")}
            ariaLabel={isCorrect ? "Mark as wrong" : "Mark as correct"}
            aria-pressed={isCorrect}
            onClick={(e) => {
              e.stopPropagation();
              toggleCorrect();
            }}>
            {isCorrect ? (
              <img
                src={PurpleCheckIcon}
                alt=''
                className={styles.correctIcon}
              />
            ) : (
              <XMarkIcon className={styles.wrongIcon} />
            )}
          </button>
          <IconBtn
            variant='ghost'
            size='xs'
            icon={<EllipsisVerticalIcon />}
            ariaLabel={`Edit option ${displayIndex.toString()}`}
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
            handleClose={() => {
              setPopoverOpen(false);
            }}
            displayIndex={displayIndex}
            previewUrl={previewUrl}
            color={color}
            inputIdBase={inputIdBase}
            flush={flush}
          />
        )}
        {canAddOption && (
          <div className={styles.canAddBtn}>
            <IconBtn
              size='sm'
              shape='round'
              variant='info'
              onClick={addOption}
              disabled={!canAddOption}
              icon={
                <svg
                  width='100pt'
                  height='100pt'
                  version='1.1'
                  viewBox='0 0 100 100'
                  xmlns='http://www.w3.org/2000/svg'>
                  <path
                    d='m50 26.699c-1.3906 0-2.5195 1.1289-2.5195 2.5195v18.262h-18.262c-1.3906 0-2.5195 1.1289-2.5195 2.5195s1.1289 2.5195 2.5195 2.5195h18.262v18.262c0 1.3906 1.1289 2.5195 2.5195 2.5195s2.5195-1.1289 2.5195-2.5195v-18.262h18.262c1.3906 0 2.5195-1.1289 2.5195-2.5195s-1.1289-2.5195-2.5195-2.5195h-18.262v-18.262c0-1.3906-1.1289-2.5195-2.5195-2.5195z'
                    fill='green'
                  />
                </svg>
              }
            />
          </div>
        )}
      </div>{" "}
    </Container>
  );
};

export { McqOptionEditable };
