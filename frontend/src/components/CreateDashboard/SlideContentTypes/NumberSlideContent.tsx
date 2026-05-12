/**
 * Author surface for a numeric question (NumberQuestion).
 *
 * Players submit a number; their submission counts as correct when
 * |submitted - correctValue| ≤ tolerance. `unitLabel` is a display-only
 * suffix (e.g. " km", "$"); `decimalPlaces` controls the player's input
 * precision in the live renderer.
 */
import { useState } from "react";
import { SlideContentWrapper } from "./SlideContentWrapper";
import { RichTextInput } from "@/components/Common/Input/RichTextInput";
import { useElementEditor } from "./useElementEditor";
import type { NumberQuestion } from "@/store/BrainFlexApi";
import styles from "./SlideContentTypes.module.css";

const isNumberQuestion = (e: { kind: string }): e is NumberQuestion =>
  e.kind === "NumberQuestion";

const NumberSlideContent = () => {
  const { element, schedule, flush, syncedFromId, markSynced } =
    useElementEditor<NumberQuestion>(isNumberQuestion);

  const [prompt, setPrompt] = useState(element?.prompt ?? "");
  const [correctValue, setCorrectValue] = useState<number>(
    element?.correctValue ?? 0,
  );
  const [tolerance, setTolerance] = useState<number>(element?.tolerance ?? 0);
  const [unitLabel, setUnitLabel] = useState(element?.unitLabel ?? "");
  const [decimalPlaces, setDecimalPlaces] = useState<number>(
    element?.decimalPlaces ?? 0,
  );
  const [pointValue, setPointValue] = useState<number>(
    element?.pointValue ?? 0,
  );

  if (element && syncedFromId !== element.id) {
    markSynced(element.id);
    setPrompt(element.prompt ?? "");
    setCorrectValue(element.correctValue ?? 0);
    setTolerance(element.tolerance ?? 0);
    setUnitLabel(element.unitLabel ?? "");
    setDecimalPlaces(element.decimalPlaces ?? 0);
    setPointValue(element.pointValue ?? 0);
  }

  if (!element) {
    return (
      <SlideContentWrapper>
        <p>Select a slide to edit.</p>
      </SlideContentWrapper>
    );
  }

  const buildPatch = (overrides: Partial<NumberQuestion>): NumberQuestion => ({
    ...element,
    prompt,
    correctValue,
    tolerance,
    unitLabel,
    decimalPlaces,
    pointValue,
    ...overrides,
  });

  return (
    <SlideContentWrapper>
      <RichTextInput
        label='Question'
        id={`num-prompt-${element.id ?? ""}`}
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
          Correct value
          <input
            type='number'
            className={styles.numberInput}
            value={correctValue}
            onChange={(e) => {
              const next = Number(e.target.value) || 0;
              setCorrectValue(next);
              schedule(buildPatch({ correctValue: next }));
            }}
            onBlur={flush}
          />
        </label>

        <label className={styles.fieldLabel}>
          Tolerance (±)
          <input
            type='number'
            min={0}
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

        <label className={styles.fieldLabel}>
          Decimal places
          <input
            type='number'
            min={0}
            max={10}
            className={styles.numberInput}
            value={decimalPlaces}
            onChange={(e) => {
              const next = Number(e.target.value) || 0;
              setDecimalPlaces(next);
              schedule(buildPatch({ decimalPlaces: next }));
            }}
            onBlur={flush}
          />
        </label>

        <label className={styles.fieldLabel}>
          Unit label
          <input
            type='text'
            className={styles.textInput}
            value={unitLabel}
            placeholder='e.g. km, $, %'
            onChange={(e) => {
              const next = e.target.value;
              setUnitLabel(next);
              schedule(buildPatch({ unitLabel: next }));
            }}
            onBlur={flush}
          />
        </label>

        <label className={styles.fieldLabel}>
          Points
          <input
            type='number'
            min={0}
            className={styles.numberInput}
            value={pointValue}
            onChange={(e) => {
              const next = Number(e.target.value) || 0;
              setPointValue(next);
              schedule(buildPatch({ pointValue: next }));
            }}
            onBlur={flush}
          />
        </label>
      </div>
    </SlideContentWrapper>
  );
};

export { NumberSlideContent };
