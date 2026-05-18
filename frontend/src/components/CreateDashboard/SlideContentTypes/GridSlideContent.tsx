/**
 * Author surface for a grid question.
 *
 * Players see an N×M grid overlaid on a backing image and tap one or more
 * cells. `correctCellIndexes` is the set of correct cell indexes (row-major).
 * `multipleCorrect` toggles single-pick vs. multi-pick on the player side.
 *
 * The cell-selection UI (visual grid overlay) is TODO; we expose the raw
 * "correct cell indexes" field as a comma list so the question is at least
 * authorable until the visual editor ships. The backing image is a single
 * unified `Image` — gallery picker hands back a complete value and pasted
 * URLs become external images.
 */
import { useState } from "react";
import { SlideContentWrapper } from "./SlideContentWrapper";
import { RichTextInput } from "@/components/Common/Input/RichTextInput/RichTextInput";
import { Input } from "@/components/Common/Input/Input/Input";
import { NumberInput } from "@/components/Common/Input/NumberInput/NumberInput";
import { Checkbox } from "@/components/Common/Input/Checkbox/Checkbox";
import { Btn } from "@/components/Common/Buttons/Btn";
import { useElementEditor } from "./useElementEditor";
import { useGalleryPicker } from "@/hooks/useGalleryPicker";
import type { GridQuestion, Image } from "@/store/BrainFlexApi";
import { displayUrl, externalImage } from "@/utils/image";
import styles from "./SlideContentTypes.module.css";

const isGrid = (e: { kind: string }): e is GridQuestion =>
  e.kind === "GridQuestion";

const indexesToInput = (idxs: number[] | undefined) => (idxs ?? []).join(", ");
const inputToIndexes = (raw: string) =>
  raw
    .split(",")
    .map((s) => Number(s.trim()))
    .filter((n) => Number.isFinite(n) && n >= 0);

/** The paste-URL input only shows external URLs; gallery picks leave it blank. */
const pasteUrlOf = (image: Image | undefined): string =>
  image?.useExternalImg ? (image.imgUrl ?? "") : "";

const GridSlideContent = () => {
  const { element, schedule, flush, commit, syncedFromId, markSynced } =
    useElementEditor<GridQuestion>(isGrid);
  const openPicker = useGalleryPicker();

  const [prompt, setPrompt] = useState(element?.prompt ?? "");
  const [rows, setRows] = useState<number>(element?.rows ?? 3);
  const [cols, setCols] = useState<number>(element?.cols ?? 3);
  const [pasteUrl, setPasteUrl] = useState<string>(() =>
    pasteUrlOf(element?.cells?.backingImage),
  );
  const [correctText, setCorrectText] = useState(() =>
    indexesToInput(element?.correctCellIndexes),
  );
  const [multipleCorrect, setMultipleCorrect] = useState<boolean>(
    element?.multipleCorrect ?? false,
  );

  if (element && syncedFromId !== element.id) {
    markSynced(element.id);
    setPrompt(element.prompt ?? "");
    setRows(element.rows ?? 3);
    setCols(element.cols ?? 3);
    setPasteUrl(pasteUrlOf(element.cells?.backingImage));
    setCorrectText(indexesToInput(element.correctCellIndexes));
    setMultipleCorrect(element.multipleCorrect ?? false);
  }

  if (!element) {
    return (
      <SlideContentWrapper title='Grid'>
        <p>Select a slide to edit.</p>
      </SlideContentWrapper>
    );
  }

  const buildPatch = (overrides: Partial<GridQuestion>): GridQuestion => ({
    ...element,
    prompt,
    rows,
    cols,
    correctCellIndexes: inputToIndexes(correctText),
    multipleCorrect,
    ...overrides,
  });

  const seed = element.id ?? "grid";
  const imgSrc = displayUrl(element.cells?.backingImage, seed, 640, 360);

  return (
    <SlideContentWrapper
      title='Grid'
      description='N×M grid overlaid on an image. Players tap one or more cells.'>
      <RichTextInput
        label='Question'
        id={`grid-prompt-${element.id ?? ""}`}
        placeholder='Type your question…'
        value={prompt}
        onChange={(html) => {
          setPrompt(html);
          schedule(buildPatch({ prompt: html }));
        }}
        onBlur={flush}
      />

      <div className={styles.fieldRow}>
        <NumberInput
          label='Rows'
          id={`grid-rows-${element.id ?? ""}`}
          min={1}
          value={rows}
          onChange={(next) => {
            const safe = next < 1 ? 1 : next;
            setRows(safe);
            schedule(buildPatch({ rows: safe }));
          }}
          onBlur={flush}
        />
        <NumberInput
          label='Cols'
          id={`grid-cols-${element.id ?? ""}`}
          min={1}
          value={cols}
          onChange={(next) => {
            const safe = next < 1 ? 1 : next;
            setCols(safe);
            schedule(buildPatch({ cols: safe }));
          }}
          onBlur={flush}
        />
        <Checkbox
          label='Multiple correct cells'
          id={`grid-multi-${element.id ?? ""}`}
          checked={multipleCorrect}
          onChange={(e) => {
            const next = e.target.checked;
            setMultipleCorrect(next);
            schedule(buildPatch({ multipleCorrect: next }));
          }}
        />
      </div>

      <div className={styles.imageEditorRow}>
        <img src={imgSrc} alt='' className={styles.imagePlaceholder} />
        <div className={styles.imageEditorFields}>
          <div className={styles.urlPickerRow}>
            <div className={styles.urlPickerInput}>
              <Input
                label='Backing image URL (placeholder shown if blank)'
                id={`grid-image-${element.id ?? ""}`}
                type='text'
                fullWidth
                value={pasteUrl}
                placeholder='https://…'
                onChange={(e) => {
                  const next = e.target.value;
                  setPasteUrl(next);
                  schedule(
                    buildPatch({
                      cells: {
                        ...element.cells,
                        backingImage: externalImage(next),
                      },
                    }),
                  );
                }}
                onBlur={flush}
              />
            </div>
            <Btn
              size='sm'
              onClick={() => {
                flush();
                openPicker((image) => {
                  setPasteUrl("");
                  commit(
                    buildPatch({
                      cells: { ...element.cells, backingImage: image },
                    }),
                  );
                });
              }}>
              Gallery
            </Btn>
          </div>
          <Input
            label='Correct cell indexes (comma-separated, row-major)'
            id={`grid-correct-${element.id ?? ""}`}
            type='text'
            fullWidth
            value={correctText}
            placeholder='e.g. 0, 4, 8'
            onChange={(e) => {
              const next = e.target.value;
              setCorrectText(next);
              schedule(
                buildPatch({ correctCellIndexes: inputToIndexes(next) }),
              );
            }}
            onBlur={flush}
          />
        </div>
      </div>

      {/* TODO: Get more specs — visual cell-selection overlay on the backing
          image and per-cell labels (cells.labels). The media-library picker
          is now wired in via the `Gallery` button. */}
    </SlideContentWrapper>
  );
};

export { GridSlideContent };
