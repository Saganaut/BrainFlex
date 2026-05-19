/**
 * Author surface for a Drawing question (DrawingQuestion).
 *
 * Players draw on a blank or image-backed canvas; the submitted answer is a
 * list of strokes that the host's reveal view renders as a grid of mini
 * canvases. Survey-only — never scored. Best Answer mode is a natural fit
 * (vote the best sketch) so the editor exposes the prompt + canvas geometry
 * + caps; the live-play canvas itself ships in the player surface.
 *
 * The backing image is optional. Authors who want a blank canvas leave it
 * empty; the player view paints over the image when present. The image
 * picker mirrors the PlaceOnImage editor — paste a URL or pick from the
 * gallery.
 *
 * Palette is comma-separated to keep the schema cheap (the player canvas
 * accepts hex strings or design-system token names). Empty palette means
 * "use the canvas defaults".
 */
import { useState } from "react";
import { SlideContentWrapper } from "./SlideContentWrapper";
import { RichTextInput } from "@/components/Common/Input/RichTextInput/RichTextInput";
import { Input } from "@/components/Common/Input/Input/Input";
import { NumberInput } from "@/components/Common/Input/NumberInput/NumberInput";
import { Btn } from "@/components/Common/Buttons/Btn";
import { useElementEditor } from "./useElementEditor";
import { useGalleryPicker } from "@/hooks/useGalleryPicker";
import type { DrawingQuestion, Image } from "@/store/BrainFlexApi";
import { displayUrl, externalImage, largestUrl } from "@/utils/image";
import styles from "./SlideContentTypes.module.css";

const isDrawing = (e: { kind: string }): e is DrawingQuestion =>
  e.kind === "DrawingQuestion";

const paletteToInput = (palette: string[] | undefined) =>
  (palette ?? []).join(", ");
const inputToPalette = (raw: string) =>
  raw
    .split(",")
    .map((s) => s.trim())
    .filter((s) => s !== "");

/** The paste-URL field only carries external URLs; gallery picks blank it. */
const pasteUrlOf = (image: Image | undefined): string =>
  image?.useExternalImg ? (largestUrl(image, "") ?? "") : "";

const DrawingSlideContent = () => {
  const { element, schedule, flush, commit, syncedFromId, markSynced } =
    useElementEditor<DrawingQuestion>(isDrawing);
  const openPicker = useGalleryPicker();

  const [prompt, setPrompt] = useState(element?.prompt ?? "");
  const [pasteUrl, setPasteUrl] = useState(() =>
    pasteUrlOf(element?.backingImage),
  );
  const [canvasWidth, setCanvasWidth] = useState<number>(
    element?.canvasWidth ?? 1920,
  );
  const [canvasHeight, setCanvasHeight] = useState<number>(
    element?.canvasHeight ?? 1080,
  );
  const [maxStrokes, setMaxStrokes] = useState<number>(
    element?.maxStrokesPerPlayer ?? 200,
  );
  const [maxPoints, setMaxPoints] = useState<number>(
    element?.maxPointsPerStroke ?? 500,
  );
  const [paletteText, setPaletteText] = useState<string>(() =>
    paletteToInput(element?.palette),
  );

  if (element && syncedFromId !== element.id) {
    markSynced(element.id);
    setPrompt(element.prompt ?? "");
    setPasteUrl(pasteUrlOf(element.backingImage));
    setCanvasWidth(element.canvasWidth ?? 1920);
    setCanvasHeight(element.canvasHeight ?? 1080);
    setMaxStrokes(element.maxStrokesPerPlayer ?? 200);
    setMaxPoints(element.maxPointsPerStroke ?? 500);
    setPaletteText(paletteToInput(element.palette));
  }

  if (!element) {
    return (
      <SlideContentWrapper title='Drawing'>
        <p>Select a slide to edit.</p>
      </SlideContentWrapper>
    );
  }

  const buildPatch = (
    overrides: Partial<DrawingQuestion>,
  ): DrawingQuestion => ({
    ...element,
    prompt,
    canvasWidth,
    canvasHeight,
    maxStrokesPerPlayer: maxStrokes,
    maxPointsPerStroke: maxPoints,
    palette: inputToPalette(paletteText),
    ...overrides,
  });

  const imgSrc = displayUrl(
    element.backingImage,
    element.id ?? "draw",
    640,
    360,
  );

  return (
    <SlideContentWrapper
      title='Drawing'
      description='Open canvas. Players sketch on a blank or image-backed surface — never scored. Pair with Best Answer to vote the favourite.'>
      <RichTextInput
        label='Prompt'
        id={`draw-prompt-${element.id ?? ""}`}
        placeholder='Sketch your team logo'
        value={prompt}
        onChange={(html) => {
          setPrompt(html);
          schedule(buildPatch({ prompt: html }));
        }}
        onBlur={flush}
      />

      <div className={styles.imageEditorRow}>
        <img src={imgSrc} alt='' className={styles.imagePlaceholder} />
        <div className={styles.imageEditorFields}>
          <div className={styles.urlPickerRow}>
            <div className={styles.urlPickerInput}>
              <Input
                label='Backing image URL (optional — leave blank for a clean canvas)'
                id={`draw-image-${element.id ?? ""}`}
                type='text'
                fullWidth
                value={pasteUrl}
                placeholder='https://…'
                onChange={(e) => {
                  const next = e.target.value;
                  setPasteUrl(next);
                  schedule(buildPatch({ backingImage: externalImage(next) }));
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
                  commit(buildPatch({ backingImage: image }));
                });
              }}>
              Gallery
            </Btn>
          </div>
        </div>
      </div>

      <div className={styles.fieldRow}>
        <NumberInput
          label='Canvas width (logical units)'
          id={`draw-w-${element.id ?? ""}`}
          min={1}
          value={canvasWidth}
          onChange={(next) => {
            setCanvasWidth(next);
            schedule(buildPatch({ canvasWidth: next }));
          }}
          onBlur={flush}
        />
        <NumberInput
          label='Canvas height (logical units)'
          id={`draw-h-${element.id ?? ""}`}
          min={1}
          value={canvasHeight}
          onChange={(next) => {
            setCanvasHeight(next);
            schedule(buildPatch({ canvasHeight: next }));
          }}
          onBlur={flush}
        />
      </div>

      <div className={styles.fieldRow}>
        <NumberInput
          label='Max strokes per player'
          id={`draw-max-strokes-${element.id ?? ""}`}
          min={1}
          value={maxStrokes}
          onChange={(next) => {
            setMaxStrokes(next);
            schedule(buildPatch({ maxStrokesPerPlayer: next }));
          }}
          onBlur={flush}
        />
        <NumberInput
          label='Max points per stroke'
          id={`draw-max-points-${element.id ?? ""}`}
          min={1}
          value={maxPoints}
          onChange={(next) => {
            setMaxPoints(next);
            schedule(buildPatch({ maxPointsPerStroke: next }));
          }}
          onBlur={flush}
        />
      </div>

      <Input
        label='Palette (comma-separated hex or token names)'
        id={`draw-palette-${element.id ?? ""}`}
        type='text'
        value={paletteText}
        placeholder='e.g. #6019FF, #54FFF1'
        onChange={(e) => {
          const next = e.target.value;
          setPaletteText(next);
          schedule(buildPatch({ palette: inputToPalette(next) }));
        }}
        onBlur={flush}
      />

      {/* TODO: Get more specs — visual palette swatch editor, on-canvas live
          preview, and stroke-thickness picker. The player canvas (pointer
          events, undo, clear, submit) ships separately. */}
    </SlideContentWrapper>
  );
};

export { DrawingSlideContent };
