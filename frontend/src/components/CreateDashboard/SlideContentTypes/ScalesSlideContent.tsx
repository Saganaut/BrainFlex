/**
 * Author surface for a scales / Likert question (ScalesQuestion).
 *
 * The author defines a numeric scale (min / max, optional anchor labels) and
 * one or more statements players rate on that scale. `scored` flips the
 * question between a pulse-style data-gathering round and a scored round
 * (when scored, each statement needs a `correctRating`).
 *
 * For now the editor exposes statement text + the scale knobs. Per-statement
 * correct ratings are a TODO once we have the spec for scored mode.
 */
import { useState } from "react";
import { SlideContentWrapper } from "./SlideContentWrapper";
import { RichTextInput } from "@/components/Common/Input/RichTextInput";
import { useElementEditor } from "./useElementEditor";
import { Btn } from "@/components/Common/Buttons/Btn";
import type { ScaleStatement, ScalesQuestion } from "@/store/BrainFlexApi";
import styles from "./SlideContentTypes.module.css";

const isScales = (e: { kind: string }): e is ScalesQuestion =>
  e.kind === "ScalesQuestion";

const MIN_STATEMENTS = 1;
const MAX_STATEMENTS = 10;

const ScalesSlideContent = () => {
  const { element, schedule, flush, commit, syncedFromId, markSynced } =
    useElementEditor<ScalesQuestion>(isScales);

  const [prompt, setPrompt] = useState(element?.prompt ?? "");
  const [statements, setStatements] = useState<ScaleStatement[]>(
    element?.statements ?? [],
  );
  const [scaleMin, setScaleMin] = useState<number>(element?.scaleMin ?? 1);
  const [scaleMax, setScaleMax] = useState<number>(element?.scaleMax ?? 5);
  const [minLabel, setMinLabel] = useState(element?.minLabel ?? "");
  const [maxLabel, setMaxLabel] = useState(element?.maxLabel ?? "");
  const [scored, setScored] = useState<boolean>(element?.scored ?? false);

  if (element && syncedFromId !== element.id) {
    markSynced(element.id);
    setPrompt(element.prompt ?? "");
    setStatements(element.statements ?? []);
    setScaleMin(element.scaleMin ?? 1);
    setScaleMax(element.scaleMax ?? 5);
    setMinLabel(element.minLabel ?? "");
    setMaxLabel(element.maxLabel ?? "");
    setScored(element.scored ?? false);
  }

  if (!element) {
    return (
      <SlideContentWrapper>
        <p>Select a slide to edit.</p>
      </SlideContentWrapper>
    );
  }

  const buildPatch = (
    overrides: Partial<ScalesQuestion>,
  ): ScalesQuestion => ({
    ...element,
    prompt,
    statements,
    scaleMin,
    scaleMax,
    minLabel,
    maxLabel,
    scored,
    ...overrides,
  });

  const handleAddStatement = () => {
    if (statements.length >= MAX_STATEMENTS) return;
    flush();
    const next = [...statements, { id: crypto.randomUUID(), text: "" }];
    setStatements(next);
    commit(buildPatch({ statements: next }));
  };

  const handleRemoveStatement = (id: string) => {
    if (statements.length <= MIN_STATEMENTS) return;
    flush();
    const next = statements.filter((s) => s.id !== id);
    setStatements(next);
    commit(buildPatch({ statements: next }));
  };

  const handleStatementTextChange = (id: string, text: string) => {
    const next = statements.map((s) => (s.id === id ? { ...s, text } : s));
    setStatements(next);
    schedule(buildPatch({ statements: next }));
  };

  return (
    <SlideContentWrapper>
      <RichTextInput
        label='Question'
        id={`scales-prompt-${element.id ?? ""}`}
        placeholder='What is the player rating?'
        value={prompt}
        onChange={(html) => {
          setPrompt(html);
          schedule(buildPatch({ prompt: html }));
        }}
        onBlur={flush}
      />

      <div className={styles.fieldRow}>
        <label className={styles.fieldLabel}>
          Scale min
          <input
            type='number'
            className={styles.numberInput}
            value={scaleMin}
            onChange={(e) => {
              const next = Number(e.target.value) || 0;
              setScaleMin(next);
              schedule(buildPatch({ scaleMin: next }));
            }}
            onBlur={flush}
          />
        </label>
        <label className={styles.fieldLabel}>
          Scale max
          <input
            type='number'
            className={styles.numberInput}
            value={scaleMax}
            onChange={(e) => {
              const next = Number(e.target.value) || 0;
              setScaleMax(next);
              schedule(buildPatch({ scaleMax: next }));
            }}
            onBlur={flush}
          />
        </label>
        <label className={styles.fieldLabel}>
          Min label
          <input
            type='text'
            className={styles.textInput}
            value={minLabel}
            placeholder='e.g. Strongly disagree'
            onChange={(e) => {
              const next = e.target.value;
              setMinLabel(next);
              schedule(buildPatch({ minLabel: next }));
            }}
            onBlur={flush}
          />
        </label>
        <label className={styles.fieldLabel}>
          Max label
          <input
            type='text'
            className={styles.textInput}
            value={maxLabel}
            placeholder='e.g. Strongly agree'
            onChange={(e) => {
              const next = e.target.value;
              setMaxLabel(next);
              schedule(buildPatch({ maxLabel: next }));
            }}
            onBlur={flush}
          />
        </label>
        <label className={styles.checkboxLabel}>
          <input
            type='checkbox'
            checked={scored}
            onChange={(e) => {
              const next = e.target.checked;
              setScored(next);
              schedule(buildPatch({ scored: next }));
            }}
            onBlur={flush}
          />
          Scored (vs. pulse-style)
        </label>
      </div>

      <div className={styles.sectionHeader}>
        <span className={styles.sectionLabel}>Statements</span>
        <Btn
          size='sm'
          onClick={handleAddStatement}
          disabled={statements.length >= MAX_STATEMENTS}>
          + Add statement
        </Btn>
      </div>

      <div className={styles.itemList}>
        {statements.map((s, idx) => (
          <div key={s.id} className={styles.itemRow}>
            <span>{idx + 1}.</span>
            <input
              type='text'
              className={styles.textInput}
              value={s.text ?? ""}
              placeholder={`Statement ${(idx + 1).toString()}`}
              onChange={(e) => {
                if (s.id) handleStatementTextChange(s.id, e.target.value);
              }}
              onBlur={flush}
            />
            <button
              type='button'
              className={styles.iconBtn}
              aria-label={`Remove statement ${(idx + 1).toString()}`}
              disabled={statements.length <= MIN_STATEMENTS}
              onClick={() => {
                if (s.id) handleRemoveStatement(s.id);
              }}>
              −
            </button>
          </div>
        ))}
      </div>

      {/* TODO: Get more specs — when `scored` is true, each statement needs a
          correctRating in [scaleMin, scaleMax]. Need the UI shape for that
          (slider? number? per-statement). */}
    </SlideContentWrapper>
  );
};

export { ScalesSlideContent };
