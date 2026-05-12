/**
 * Author surface for an image-choice question. Same shape as MCQ except that
 * each option carries an `imageUrl` (the option is the image; the text is the
 * label/caption). Backend still uses a single `correctOptionId`.
 *
 * Image upload requires the media library, which isn't built yet — for now,
 * every option shows a deterministic Lorem Picsum placeholder seeded on the
 * option id, and the editor exposes a plain URL field so the author can
 * paste a custom one if they have it.
 */
import { useState } from "react";
import { SlideContentWrapper } from "./SlideContentWrapper";
import { RichTextInput } from "@/components/Common/Input/RichTextInput";
import { useElementEditor } from "./useElementEditor";
import { Btn } from "@/components/Common/Buttons/Btn";
import type {
  ImageChoiceQuestion,
  McqOption,
} from "@/store/BrainFlexApi";
import styles from "./SlideContentTypes.module.css";

const isImageChoice = (e: { kind: string }): e is ImageChoiceQuestion =>
  e.kind === "ImageChoiceQuestion";

const MIN_OPTIONS = 2;
const MAX_OPTIONS = 6;

/** Lorem Picsum placeholder seeded on the option id so each card stays stable
 *  across re-renders without needing real uploaded images. */
const placeholderImageUrl = (seed: string) =>
  `https://picsum.photos/seed/${encodeURIComponent(seed)}/240/240`;

const ImageChoiceSlideContent = () => {
  const { element, schedule, flush, commit, syncedFromId, markSynced } =
    useElementEditor<ImageChoiceQuestion>(isImageChoice);

  const [prompt, setPrompt] = useState(element?.prompt ?? "");
  const [options, setOptions] = useState<McqOption[]>(element?.options ?? []);
  const [correctOptionId, setCorrectOptionId] = useState<string | undefined>(
    element?.correctOptionId,
  );

  if (element && syncedFromId !== element.id) {
    markSynced(element.id);
    setPrompt(element.prompt ?? "");
    setOptions(element.options ?? []);
    setCorrectOptionId(element.correctOptionId);
  }

  if (!element) {
    return (
      <SlideContentWrapper>
        <p>Select a slide to edit.</p>
      </SlideContentWrapper>
    );
  }

  const buildPatch = (overrides: {
    prompt?: string;
    options?: McqOption[];
    correctOptionId?: string;
  }): ImageChoiceQuestion => ({
    ...element,
    prompt: overrides.prompt ?? prompt,
    options: overrides.options ?? options,
    correctOptionId: overrides.correctOptionId ?? correctOptionId,
  });

  const handleAddOption = () => {
    if (options.length >= MAX_OPTIONS) return;
    flush();
    const next: McqOption[] = [
      ...options,
      { id: crypto.randomUUID(), text: "", imageUrl: "" },
    ];
    setOptions(next);
    commit(buildPatch({ options: next }));
  };

  const handleRemoveOption = (id: string) => {
    if (options.length <= MIN_OPTIONS) return;
    flush();
    const next = options.filter((o) => o.id !== id);
    const nextCorrect = correctOptionId === id ? undefined : correctOptionId;
    setOptions(next);
    if (nextCorrect !== correctOptionId) setCorrectOptionId(nextCorrect);
    commit(buildPatch({ options: next, correctOptionId: nextCorrect }));
  };

  const handleOptionField = (
    id: string,
    field: "text" | "imageUrl",
    value: string,
  ) => {
    const next = options.map((o) =>
      o.id === id ? { ...o, [field]: value } : o,
    );
    setOptions(next);
    schedule(buildPatch({ options: next }));
  };

  const handleSetCorrect = (id: string) => {
    flush();
    setCorrectOptionId(id);
    commit(buildPatch({ correctOptionId: id }));
  };

  const hasCorrect =
    correctOptionId !== undefined &&
    options.some((o) => o.id === correctOptionId);

  return (
    <SlideContentWrapper>
      <RichTextInput
        label='Question'
        id={`img-prompt-${element.id ?? ""}`}
        placeholder='Type your question…'
        value={prompt}
        onChange={(html) => {
          setPrompt(html);
          schedule(buildPatch({ prompt: html }));
        }}
        onBlur={flush}
      />

      <div className={styles.sectionHeader}>
        <span className={styles.sectionLabel}>Image options</span>
        <Btn
          size='sm'
          onClick={handleAddOption}
          disabled={options.length >= MAX_OPTIONS}>
          + Add option
        </Btn>
      </div>

      <div className={styles.itemList}>
        {options.map((option, idx) => {
          const id = option.id ?? `__no-id-${idx.toString()}`;
          const isCorrect = correctOptionId === option.id;
          const imgSrc =
            option.imageUrl && option.imageUrl.trim() !== ""
              ? option.imageUrl
              : placeholderImageUrl(id);
          return (
            <div key={id} className={styles.itemRow}>
              <img
                src={imgSrc}
                alt=''
                className={styles.optionImagePlaceholder}
                style={{ width: 80, height: 80 }}
              />
              <div style={{ flex: 1, display: "flex", flexDirection: "column", gap: "var(--space-1)" }}>
                <input
                  type='text'
                  className={styles.textInput}
                  value={option.text ?? ""}
                  placeholder={`Caption ${(idx + 1).toString()}`}
                  onChange={(e) => {
                    if (option.id) handleOptionField(option.id, "text", e.target.value);
                  }}
                  onBlur={flush}
                />
                <input
                  type='text'
                  className={styles.textInput}
                  value={option.imageUrl ?? ""}
                  placeholder='Image URL (or leave blank for placeholder)'
                  onChange={(e) => {
                    if (option.id) handleOptionField(option.id, "imageUrl", e.target.value);
                  }}
                  onBlur={flush}
                />
                <label className={styles.checkboxLabel}>
                  <input
                    type='radio'
                    name={`img-correct-${element.id ?? ""}`}
                    checked={isCorrect}
                    onChange={() => {
                      if (option.id) handleSetCorrect(option.id);
                    }}
                  />
                  Correct
                </label>
              </div>
              <button
                type='button'
                className={styles.iconBtn}
                aria-label={`Remove option ${(idx + 1).toString()}`}
                disabled={options.length <= MIN_OPTIONS}
                onClick={() => {
                  if (option.id) handleRemoveOption(option.id);
                }}>
                −
              </button>
            </div>
          );
        })}
      </div>

      {!hasCorrect && (
        <p className={styles.warning} role='alert'>
          Not setting a correct answer means this slide is not scoreable in a
          game showcase.
        </p>
      )}

      {/* TODO: Get more specs — replace the URL field with the media library
          picker (drag-and-drop, crop, alt text). Multi-correct on
          ImageChoice would mirror the MCQ change once spec'd. */}
    </SlideContentWrapper>
  );
};

export { ImageChoiceSlideContent };
