/**
 * Author surface for a "place on image" question.
 *
 * The player taps a point on a target image; the click counts as correct when
 * the distance to (correctX, correctY) is ≤ tolerance. All three of those
 * fields are normalised 0–1, so the same question works at any rendered
 * image size.
 *
 * Image upload is TODO (waiting on the media library). For now we use a
 * Lorem Picsum placeholder, a plain URL field, and the (x, y, tolerance)
 * numeric knobs.
 */
import { useState } from "react";
import { SlideContentWrapper } from "./SlideContentWrapper";
import { RichTextInput } from "@/components/Common/Input/RichTextInput";
import { useElementEditor } from "./useElementEditor";
import type { PlaceOnImageQuestion } from "@/store/BrainFlexApi";
import styles from "./SlideContentTypes.module.css";

const isPlaceOnImage = (e: { kind: string }): e is PlaceOnImageQuestion =>
  e.kind === "PlaceOnImageQuestion";

const placeholderImageUrl = (seed: string) =>
  `https://picsum.photos/seed/${encodeURIComponent(seed)}/640/360`;

const PlaceOnImageSlideContent = () => {
  const { element, schedule, flush, syncedFromId, markSynced } =
    useElementEditor<PlaceOnImageQuestion>(isPlaceOnImage);

  const [prompt, setPrompt] = useState(element?.prompt ?? "");
  const [targetImageUrl, setTargetImageUrl] = useState(
    element?.targetImageUrl ?? "",
  );
  const [correctX, setCorrectX] = useState<number>(element?.correctX ?? 0.5);
  const [correctY, setCorrectY] = useState<number>(element?.correctY ?? 0.5);
  const [tolerance, setTolerance] = useState<number>(
    element?.tolerance ?? 0.1,
  );

  if (element && syncedFromId !== element.id) {
    markSynced(element.id);
    setPrompt(element.prompt ?? "");
    setTargetImageUrl(element.targetImageUrl ?? "");
    setCorrectX(element.correctX ?? 0.5);
    setCorrectY(element.correctY ?? 0.5);
    setTolerance(element.tolerance ?? 0.1);
  }

  if (!element) {
    return (
      <SlideContentWrapper>
        <p>Select a slide to edit.</p>
      </SlideContentWrapper>
    );
  }

  const buildPatch = (
    overrides: Partial<PlaceOnImageQuestion>,
  ): PlaceOnImageQuestion => ({
    ...element,
    prompt,
    targetImageUrl,
    correctX,
    correctY,
    tolerance,
    ...overrides,
  });

  const imgSrc =
    targetImageUrl.trim() !== ""
      ? targetImageUrl
      : placeholderImageUrl(element.id ?? "place");

  return (
    <SlideContentWrapper>
      <RichTextInput
        label='Question'
        id={`place-prompt-${element.id ?? ""}`}
        placeholder='Tap the spot where…'
        value={prompt}
        onChange={(html) => {
          setPrompt(html);
          schedule(buildPatch({ prompt: html }));
        }}
        onBlur={flush}
      />

      <label className={styles.fieldLabel}>
        Target image URL (placeholder shown if blank)
        <input
          type='text'
          className={styles.textInput}
          value={targetImageUrl}
          placeholder='https://…'
          onChange={(e) => {
            const next = e.target.value;
            setTargetImageUrl(next);
            schedule(buildPatch({ targetImageUrl: next }));
          }}
          onBlur={flush}
        />
      </label>

      <img src={imgSrc} alt='' className={styles.imagePlaceholder} />

      <div className={styles.fieldRow}>
        <label className={styles.fieldLabel}>
          Correct X (0–1)
          <input
            type='number'
            min={0}
            max={1}
            step={0.01}
            className={styles.numberInput}
            value={correctX}
            onChange={(e) => {
              const next = Number(e.target.value) || 0;
              setCorrectX(next);
              schedule(buildPatch({ correctX: next }));
            }}
            onBlur={flush}
          />
        </label>
        <label className={styles.fieldLabel}>
          Correct Y (0–1)
          <input
            type='number'
            min={0}
            max={1}
            step={0.01}
            className={styles.numberInput}
            value={correctY}
            onChange={(e) => {
              const next = Number(e.target.value) || 0;
              setCorrectY(next);
              schedule(buildPatch({ correctY: next }));
            }}
            onBlur={flush}
          />
        </label>
        <label className={styles.fieldLabel}>
          Tolerance (0–1)
          <input
            type='number'
            min={0}
            max={1}
            step={0.01}
            className={styles.numberInput}
            value={tolerance}
            onChange={(e) => {
              const next = Number(e.target.value) || 0;
              setTolerance(next);
              schedule(buildPatch({ tolerance: next }));
            }}
            onBlur={flush}
          />
        </label>
      </div>

      {/* TODO: Get more specs — click-on-image to set (correctX, correctY)
          visually; draw the tolerance radius overlay; replace URL field with
          media-library picker. */}
    </SlideContentWrapper>
  );
};

export { PlaceOnImageSlideContent };
