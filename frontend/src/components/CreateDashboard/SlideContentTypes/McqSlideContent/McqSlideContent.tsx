/**
 * Author surface for an MCQ slide.
 *
 * Composition:
 *   - Prompt editor (debounced rich text) — owns a local mirror so typing
 *     stays responsive.
 *   - "Options" header with the add-option button.
 *   - A CSS grid of `<McqOptionEditable />` cards. Each card is fully
 *     self-contained: it draws its own index pill, remove button,
 *     text/image/color fields, and correct-answer checkbox, and routes
 *     every write through `useMcqOptionEditor`.
 *
 * Question-level operations (prompt edits, addOption) route through
 * `useMcqQuestionEditor`. Option-scoped operations (text/image/color,
 * remove, toggleCorrect) live on each `<McqOptionEditable />` via
 * `useMcqOptionEditor`. There's no local mirror of `options` or
 * `correctOptionIds` in this parent — those read straight from the deck
 * cache so concurrent option edits never get stomped by a parent rebuild.
 *
 * MCQ rules enforced via the hooks:
 *   - `MIN_MCQ_OPTIONS`–`MAX_MCQ_OPTIONS` bound the option count.
 *   - Zero correct answers is allowed but flagged by `EditorWarning`
 *     because the slide isn't scoreable in that state.
 */
import { useState } from "react";
import { SlideContentWrapper } from "../SlideContentWrapper";
import { RichTextInput } from "@/components/Common/Input/RichTextInput/RichTextInput";
import { Btn } from "@/components/Common/Buttons/Btn";
import { useMcqQuestionEditor } from "../useElementEditor";
import { EditorWarning } from "../EditorWarning";
import { McqOptionEditable } from "./McqOptionEditable";
import styles from "./McqSlideContent.module.css";

const McqSlideContent = () => {
  const {
    question,
    schedulePrompt,
    flush,
    syncedFromId,
    markSynced,
    canAddOption,
    addOption,
  } = useMcqQuestionEditor();

  // Only the prompt needs a local mirror — typing should feel responsive
  // and the rich-text editor controls its own DOM. Options come straight
  // from the cache via each `<McqOptionEditable />`.
  const [prompt, setPrompt] = useState(question?.prompt ?? "");

  // Resync local mirror when the active question changes. "Derive state
  // during render" pattern — safe when the new value differs.
  if (question && syncedFromId !== question.id) {
    markSynced(question.id);
    setPrompt(question.prompt ?? "");
  }

  // Even-column grid: round up half the option count, never below 2.
  const optionCount = question?.options?.length ?? 0;
  const columns = optionCount ? Math.max(Math.ceil(optionCount / 2), 2) : 2;

  if (!question) {
    return (
      <SlideContentWrapper title='Multiple choice'>
        <p>Select a slide to edit.</p>
      </SlideContentWrapper>
    );
  }

  const options = question.options ?? [];
  const hasCorrectAnswer = (question.correctOptionIds?.length ?? 0) > 0;

  return (
    <SlideContentWrapper
      footer={
        !hasCorrectAnswer ? (
          <EditorWarning>
            Not setting a correct answer means this slide is not scoreable in a
            game showcase.
          </EditorWarning>
        ) : null
      }>
      <div className={styles.slideHeader}>
        <RichTextInput
          isBordered={false}
          id={`mcq-prompt-${question.id ?? ""}`}
          placeholder='Type your question…'
          value={prompt}
          onChange={(html) => {
            setPrompt(html);
            schedulePrompt(html);
          }}
          onBlur={flush}
        />
      </div>
      <div className={styles.optionsHeader}>
        <Btn size='sm' onClick={addOption} disabled={!canAddOption}>
          + Add option
        </Btn>
      </div>

      <div
        className={styles.optionsRow}
        style={{ "--cols": columns } as React.CSSProperties}>
        {options.map((option, idx) => (
          <McqOptionEditable
            key={option.id ?? `__no-id-${idx.toString()}`}
            option={option}
          />
        ))}
      </div>
    </SlideContentWrapper>
  );
};

export { McqSlideContent };
