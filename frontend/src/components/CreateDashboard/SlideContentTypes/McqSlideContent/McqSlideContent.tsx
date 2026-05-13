/**
 * Author surface for an MCQ slide.
 *
 * Edits are kept in local state so typing stays responsive, then committed to
 * the server through a debounced `updateElement` mutation. The rich-text
 * prompt and option text inputs schedule a commit on every keystroke and
 * flush on blur. Structural changes (add / remove option, toggle correct)
 * flush any pending text edits first, then commit immediately.
 *
 * MCQ rules enforced here:
 *   - 2–6 options; freshly-created MCQs start with 4 blanks (via the picker).
 *   - Any subset of options may be marked "correct" — including none. A
 *     question with zero correct answers is not scoreable in a showcase, so
 *     we surface a small warning at the bottom of the editor.
 */
import { useState } from "react";
import { getRouteApi } from "@tanstack/react-router";
import { MinusIcon } from "@heroicons/react/24/outline";
import { SlideContentWrapper } from "../SlideContentWrapper";
import {
  useGetDeckQuery,
  useUpdateElementMutation,
  type McqOption,
  type McqQuestion,
} from "@/store/BrainFlexApi";
import { RichTextInput } from "@/components/Common/Input/RichTextInput";
import { Btn } from "@/components/Common/Buttons/Btn";
import { IconBtn } from "@/components/Common/Buttons/IconBtn";
import { useDebouncedCommit } from "@/hooks/useDebouncedCommit";
import styles from "./McqSlideContent.module.css";

const routeApi = getRouteApi("/decks/$deckId/view");

const MIN_OPTIONS = 2;
const MAX_OPTIONS = 6;

const McqSlideContent = () => {
  const { deckId } = routeApi.useParams();
  const { questionId } = routeApi.useSearch();

  const { element } = useGetDeckQuery(
    { id: deckId },
    {
      selectFromResult: ({ data }) => ({
        element: data?.elements?.find((e) => e.id === questionId) as
          | McqQuestion
          | undefined,
      }),
    },
  );

  const [updateElement] = useUpdateElementMutation();

  // ---- commit pipeline ---------------------------------------------------
  // The debounced commit always sends a fresh, complete McqQuestion built
  // from the latest local state — we never mutate `element` directly.
  const commitMcq = (patch: McqQuestion) => {
    if (!element?.id) return;
    void updateElement({
      id: deckId,
      elementId: element.id,
      body: patch,
    })
      .unwrap()
      .catch((err: unknown) => {
        console.error("Failed to update MCQ", err);
      });
  };
  const { schedule, flush } = useDebouncedCommit<McqQuestion>(commitMcq, 500);

  // ---- local-state mirror -------------------------------------------------
  const [prompt, setPrompt] = useState<string>(element?.prompt ?? "");
  const [options, setOptions] = useState<McqOption[]>(element?.options ?? []);
  const [correctOptionIds, setCorrectOptionIds] = useState<string[]>(
    element?.correctOptionIds ?? [],
  );

  // Resync when we switch to a different slide. React's "derive state during
  // render" pattern: setState during render is fine when the new value
  // differs, and it avoids the set-state-in-effect anti-pattern.
  const [syncedFromId, setSyncedFromId] = useState<string | undefined>(
    element?.id,
  );
  if (element && syncedFromId !== element.id) {
    setSyncedFromId(element.id);
    setPrompt(element.prompt ?? "");
    setOptions(element.options ?? []);
    setCorrectOptionIds(element.correctOptionIds ?? []);
  }

  if (!element) {
    return (
      <SlideContentWrapper>
        <p>Select a slide to edit.</p>
      </SlideContentWrapper>
    );
  }

  // Build a complete McqQuestion from the latest local state for committing.
  const buildPatch = (overrides: {
    prompt?: string;
    options?: McqOption[];
    correctOptionIds?: string[];
  }): McqQuestion => ({
    ...element,
    prompt: overrides.prompt ?? prompt,
    options: overrides.options ?? options,
    correctOptionIds: overrides.correctOptionIds ?? correctOptionIds,
  });

  // ---- handlers -----------------------------------------------------------
  const handlePromptChange = (html: string) => {
    setPrompt(html);
    schedule(buildPatch({ prompt: html }));
  };

  const handleOptionTextChange = (id: string, text: string) => {
    const next = options.map((o) => (o.id === id ? { ...o, text } : o));
    setOptions(next);
    schedule(buildPatch({ options: next }));
  };

  const handleAddOption = () => {
    if (options.length >= MAX_OPTIONS) return;
    flush();
    const newOption: McqOption = {
      id: crypto.randomUUID(),
      text: "",
    };
    const next = [...options, newOption];
    setOptions(next);
    commitMcq(buildPatch({ options: next }));
  };

  const handleRemoveOption = (id: string) => {
    if (options.length <= MIN_OPTIONS) return;
    flush();
    const next = options.filter((o) => o.id !== id);
    const nextCorrect = correctOptionIds.filter((cid) => cid !== id);
    setOptions(next);
    if (nextCorrect.length !== correctOptionIds.length) {
      setCorrectOptionIds(nextCorrect);
    }
    commitMcq(buildPatch({ options: next, correctOptionIds: nextCorrect }));
  };

  const handleToggleCorrect = (id: string) => {
    flush();
    const next = correctOptionIds.includes(id)
      ? correctOptionIds.filter((cid) => cid !== id)
      : [...correctOptionIds, id];
    setCorrectOptionIds(next);
    commitMcq(buildPatch({ correctOptionIds: next }));
  };

  const hasCorrectAnswer = correctOptionIds.length > 0;

  return (
    <SlideContentWrapper>
      <div className={styles.slideInnerHeader}>
        <RichTextInput
          label='Question'
          id={`mcq-prompt-${element.id ?? ""}`}
          placeholder='Type your question…'
          value={prompt}
          onChange={handlePromptChange}
          onBlur={flush}
        />
      </div>
      <div className={styles.slideInnerBody}>
        <div className={styles.optionsHeader}>
          <span className={styles.optionsLabel}>Options</span>
          <Btn
            size='sm'
            onClick={handleAddOption}
            disabled={options.length >= MAX_OPTIONS}>
            + Add option
          </Btn>
        </div>

        <div className={styles.optionsRow}>
          {options.map((option, idx) => {
            const optionId = option.id ?? `__no-id-${idx.toString()}`;
            const isCorrect = option.id
              ? correctOptionIds.includes(option.id)
              : false;
            return (
              <div
                key={optionId}
                className={`${styles.optionCard} ${isCorrect ? styles.optionCardCorrect : ""}`}>
                <div className={styles.optionTopRow}>
                  <span className={styles.optionIndex}>{idx + 1}</span>
                  <IconBtn
                    type='default'
                    size='xs'
                    bordered
                    icon={<MinusIcon />}
                    aria-label={`Remove option ${(idx + 1).toString()}`}
                    disabled={options.length <= MIN_OPTIONS}
                    onClick={() => {
                      if (option.id) handleRemoveOption(option.id);
                    }}
                  />
                </div>
                <input
                  type='text'
                  className={styles.optionInput}
                  placeholder={`Option ${(idx + 1).toString()}`}
                  value={option.text ?? ""}
                  onChange={(e) => {
                    if (option.id)
                      handleOptionTextChange(option.id, e.target.value);
                  }}
                  onBlur={flush}
                />
                <label className={styles.optionCorrectLabel}>
                  <input
                    type='checkbox'
                    checked={isCorrect}
                    onChange={() => {
                      if (option.id) handleToggleCorrect(option.id);
                    }}
                  />
                  Correct
                </label>
              </div>
            );
          })}
        </div>
      </div>
      <div className={styles.slideInnerFooter}>
        {!hasCorrectAnswer && (
          <p className={styles.warning} role='alert'>
            Not setting a correct answer means this slide is not scoreable in a
            game showcase.
          </p>
        )}
      </div>
    </SlideContentWrapper>
  );
};

export { McqSlideContent };
