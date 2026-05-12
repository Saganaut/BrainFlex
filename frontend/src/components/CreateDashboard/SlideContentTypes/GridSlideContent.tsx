/**
 * Author surface for a grid question.
 *
 * Players see an N×M grid overlaid on a backing image and tap one or more
 * cells. `correctCellIndexes` is the set of correct cell indexes (row-major).
 * `multipleCorrect` toggles single-pick vs. multi-pick on the player side.
 *
 * The backing image is a Lorem Picsum placeholder for now — the real picker
 * lands with the media library. The cell-selection UI (visual grid overlay)
 * is TODO; we expose the raw "correct cell indexes" field as a comma list so
 * the question is at least authorable until the visual editor ships.
 */
import { useState } from "react";
import { SlideContentWrapper } from "./SlideContentWrapper";
import { RichTextInput } from "@/components/Common/Input/RichTextInput";
import { useElementEditor } from "./useElementEditor";
import type { GridQuestion } from "@/store/BrainFlexApi";
import styles from "./SlideContentTypes.module.css";

const isGrid = (e: { kind: string }): e is GridQuestion =>
  e.kind === "GridQuestion";

const placeholderImageUrl = (seed: string) =>
  `https://picsum.photos/seed/${encodeURIComponent(seed)}/640/360`;

const indexesToInput = (idxs: number[] | undefined) =>
  (idxs ?? []).join(", ");
const inputToIndexes = (raw: string) =>
  raw
    .split(",")
    .map((s) => Number(s.trim()))
    .filter((n) => Number.isFinite(n) && n >= 0);

const GridSlideContent = () => {
  const { element, schedule, flush, syncedFromId, markSynced } =
    useElementEditor<GridQuestion>(isGrid);

  const [prompt, setPrompt] = useState(element?.prompt ?? "");
  const [rows, setRows] = useState<number>(element?.rows ?? 3);
  const [cols, setCols] = useState<number>(element?.cols ?? 3);
  const [backingImageUrl, setBackingImageUrl] = useState<string>(
    element?.cells?.backingImageUrl ?? "",
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
    setBackingImageUrl(element.cells?.backingImageUrl ?? "");
    setCorrectText(indexesToInput(element.correctCellIndexes));
    setMultipleCorrect(element.multipleCorrect ?? false);
  }

  if (!element) {
    return (
      <SlideContentWrapper>
        <p>Select a slide to edit.</p>
      </SlideContentWrapper>
    );
  }

  const buildPatch = (overrides: Partial<GridQuestion>): GridQuestion => ({
    ...element,
    prompt,
    rows,
    cols,
    cells: {
      ...element.cells,
      backingImageUrl: backingImageUrl || undefined,
    },
    correctCellIndexes: inputToIndexes(correctText),
    multipleCorrect,
    ...overrides,
  });

  const seed = element.id ?? "grid";
  const imgSrc =
    backingImageUrl.trim() !== "" ? backingImageUrl : placeholderImageUrl(seed);

  return (
    <SlideContentWrapper>
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
        <label className={styles.fieldLabel}>
          Rows
          <input
            type='number'
            min={1}
            className={styles.numberInput}
            value={rows}
            onChange={(e) => {
              const next = Number(e.target.value) || 1;
              setRows(next);
              schedule(buildPatch({ rows: next }));
            }}
            onBlur={flush}
          />
        </label>
        <label className={styles.fieldLabel}>
          Cols
          <input
            type='number'
            min={1}
            className={styles.numberInput}
            value={cols}
            onChange={(e) => {
              const next = Number(e.target.value) || 1;
              setCols(next);
              schedule(buildPatch({ cols: next }));
            }}
            onBlur={flush}
          />
        </label>
        <label className={styles.checkboxLabel}>
          <input
            type='checkbox'
            checked={multipleCorrect}
            onChange={(e) => {
              const next = e.target.checked;
              setMultipleCorrect(next);
              schedule(buildPatch({ multipleCorrect: next }));
            }}
            onBlur={flush}
          />
          Multiple correct cells
        </label>
      </div>

      <label className={styles.fieldLabel}>
        Backing image URL (placeholder shown if blank)
        <input
          type='text'
          className={styles.textInput}
          value={backingImageUrl}
          placeholder='https://…'
          onChange={(e) => {
            const next = e.target.value;
            setBackingImageUrl(next);
            schedule(
              buildPatch({
                cells: {
                  ...element.cells,
                  backingImageUrl: next || undefined,
                },
              }),
            );
          }}
          onBlur={flush}
        />
      </label>

      <img src={imgSrc} alt='' className={styles.imagePlaceholder} />

      <label className={styles.fieldLabel}>
        Correct cell indexes (comma-separated, row-major)
        <input
          type='text'
          className={styles.textInput}
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
      </label>

      {/* TODO: Get more specs — visual cell-selection overlay on the backing
          image, per-cell labels (cells.labels), and the media-library picker
          for the backing image. */}
    </SlideContentWrapper>
  );
};

export { GridSlideContent };
