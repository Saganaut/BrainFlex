/**
 * Author surface for a free-text question (TextQuestion).
 *
 * The author types a prompt plus one canonical correct answer and any number
 * of accepted variants (alternative spellings, abbreviations, etc.). Variants
 * are entered as comma-separated text — the input splits on commas at commit
 * time. `caseSensitive` toggles whether matching is exact-case.
 */
import { useState } from "react";
import { SlideContentWrapper } from "./SlideContentWrapper";
import { RichTextInput } from "@/components/Common/Input/RichTextInput";
import { useElementEditor } from "./useElementEditor";
import type { TextQuestion } from "@/store/BrainFlexApi";
import styles from "./SlideContentTypes.module.css";

const isTextQuestion = (e: { kind: string }): e is TextQuestion =>
  e.kind === "TextQuestion";

const variantsToInput = (variants: string[] | undefined) =>
  (variants ?? []).join(", ");
const inputToVariants = (raw: string) =>
  raw
    .split(",")
    .map((s) => s.trim())
    .filter((s) => s !== "");

const TextSlideContent = () => {
  const { element, schedule, flush, syncedFromId, markSynced } =
    useElementEditor<TextQuestion>(isTextQuestion);

  const [prompt, setPrompt] = useState<string>(element?.prompt ?? "");
  const [correctAnswer, setCorrectAnswer] = useState<string>(
    element?.correctAnswer ?? "",
  );
  const [variantsText, setVariantsText] = useState<string>(() =>
    variantsToInput(element?.acceptedVariants),
  );
  const [caseSensitive, setCaseSensitive] = useState<boolean>(
    element?.caseSensitive ?? false,
  );
  const [pointValue, setPointValue] = useState<number>(
    element?.pointValue ?? 0,
  );

  if (element && syncedFromId !== element.id) {
    markSynced(element.id);
    setPrompt(element.prompt ?? "");
    setCorrectAnswer(element.correctAnswer ?? "");
    setVariantsText(variantsToInput(element.acceptedVariants));
    setCaseSensitive(element.caseSensitive ?? false);
    setPointValue(element.pointValue ?? 0);
  }

  if (!element) {
    return (
      <SlideContentWrapper>
        <p>Select a slide to edit.</p>
      </SlideContentWrapper>
    );
  }

  const buildPatch = (overrides: Partial<TextQuestion>): TextQuestion => ({
    ...element,
    prompt,
    correctAnswer,
    acceptedVariants: inputToVariants(variantsText),
    caseSensitive,
    pointValue,
    ...overrides,
  });

  return (
    <SlideContentWrapper>
      <RichTextInput
        label='Question'
        id={`text-prompt-${element.id ?? ""}`}
        placeholder='Type your question…'
        value={prompt}
        onChange={(html) => {
          setPrompt(html);
          schedule(buildPatch({ prompt: html }));
        }}
        onBlur={flush}
      />

      <label className={styles.fieldLabel}>
        Correct answer
        <input
          type='text'
          className={styles.textInput}
          value={correctAnswer}
          placeholder='Canonical correct answer'
          onChange={(e) => {
            const next = e.target.value;
            setCorrectAnswer(next);
            schedule(buildPatch({ correctAnswer: next }));
          }}
          onBlur={flush}
        />
      </label>

      <label className={styles.fieldLabel}>
        Accepted variants (comma-separated)
        <input
          type='text'
          className={styles.textInput}
          value={variantsText}
          placeholder='e.g. paree, parisien'
          onChange={(e) => {
            const next = e.target.value;
            setVariantsText(next);
            schedule(buildPatch({ acceptedVariants: inputToVariants(next) }));
          }}
          onBlur={flush}
        />
      </label>

      <div className={styles.fieldRow}>
        <label className={styles.checkboxLabel}>
          <input
            type='checkbox'
            checked={caseSensitive}
            onChange={(e) => {
              const next = e.target.checked;
              setCaseSensitive(next);
              schedule(buildPatch({ caseSensitive: next }));
            }}
            onBlur={flush}
          />
          Case sensitive
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

      {!correctAnswer.trim() && (
        <p className={styles.warning} role='alert'>
          Not setting a correct answer means this slide is not scoreable in a
          game showcase.
        </p>
      )}
    </SlideContentWrapper>
  );
};

export { TextSlideContent };
