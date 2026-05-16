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
import { Input } from "@/components/Common/Input/Input";
import { NumberInput } from "@/components/Common/Input/NumberInput";
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
      <SlideContentWrapper title='Numeric answer'>
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
    <SlideContentWrapper
      title='Numeric answer'
      description='Players enter a number; a submission counts when it lands within ± tolerance of the target.'>
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
        <NumberInput
          label='Correct value'
          id={`num-correct-${element.id ?? ""}`}
          value={correctValue}
          onChange={(next) => {
            setCorrectValue(next);
            schedule(buildPatch({ correctValue: next }));
          }}
          onBlur={flush}
        />

        <NumberInput
          label='Tolerance (±)'
          id={`num-tolerance-${element.id ?? ""}`}
          min={0}
          value={tolerance}
          onChange={(next) => {
            setTolerance(next);
            schedule(buildPatch({ tolerance: next }));
          }}
          onBlur={flush}
        />

        <NumberInput
          label='Decimal places'
          id={`num-decimals-${element.id ?? ""}`}
          min={0}
          max={10}
          value={decimalPlaces}
          onChange={(next) => {
            setDecimalPlaces(next);
            schedule(buildPatch({ decimalPlaces: next }));
          }}
          onBlur={flush}
        />

        <Input
          label='Unit label'
          id={`num-unit-${element.id ?? ""}`}
          type='text'
          value={unitLabel}
          placeholder='e.g. km, $, %'
          onChange={(e) => {
            const next = e.target.value;
            setUnitLabel(next);
            schedule(buildPatch({ unitLabel: next }));
          }}
          onBlur={flush}
        />

        <NumberInput
          label='Points'
          id={`num-points-${element.id ?? ""}`}
          min={0}
          value={pointValue}
          onChange={(next) => {
            setPointValue(next);
            schedule(buildPatch({ pointValue: next }));
          }}
          onBlur={flush}
        />
      </div>
    </SlideContentWrapper>
  );
};

export { NumberSlideContent };
