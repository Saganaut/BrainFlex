/**
 * Author surface for an open Q & A round.
 *
 * Players submit free-form questions/comments that the host can optionally
 * moderate and the room can optionally upvote. This kind is never scored,
 * so there is no "correct answer" field.
 */
import { useState } from "react";
import { SlideContentWrapper } from "./SlideContentWrapper";
import { RichTextInput } from "@/components/Common/Input/RichTextInput";
import { useElementEditor } from "./useElementEditor";
import type { QAndAQuestion } from "@/store/BrainFlexApi";
import styles from "./SlideContentTypes.module.css";

const isQAndA = (e: { kind: string }): e is QAndAQuestion =>
  e.kind === "QAndAQuestion";

const QAndASlideContent = () => {
  const { element, schedule, flush, syncedFromId, markSynced } =
    useElementEditor<QAndAQuestion>(isQAndA);

  const [prompt, setPrompt] = useState(element?.prompt ?? "");
  const [maxSubmissions, setMaxSubmissions] = useState<number>(
    element?.maxSubmissionsPerPlayer ?? 0,
  );
  const [allowVoting, setAllowVoting] = useState<boolean>(
    element?.allowVoting ?? false,
  );
  const [autoApprove, setAutoApprove] = useState<boolean>(
    element?.autoApprove ?? false,
  );

  if (element && syncedFromId !== element.id) {
    markSynced(element.id);
    setPrompt(element.prompt ?? "");
    setMaxSubmissions(element.maxSubmissionsPerPlayer ?? 0);
    setAllowVoting(element.allowVoting ?? false);
    setAutoApprove(element.autoApprove ?? false);
  }

  if (!element) {
    return (
      <SlideContentWrapper>
        <p>Select a slide to edit.</p>
      </SlideContentWrapper>
    );
  }

  const buildPatch = (overrides: Partial<QAndAQuestion>): QAndAQuestion => ({
    ...element,
    prompt,
    maxSubmissionsPerPlayer: maxSubmissions,
    allowVoting,
    autoApprove,
    ...overrides,
  });

  return (
    <SlideContentWrapper>
      <RichTextInput
        label='Question'
        id={`qa-prompt-${element.id ?? ""}`}
        placeholder='Type your question or prompt…'
        value={prompt}
        onChange={(html) => {
          setPrompt(html);
          schedule(buildPatch({ prompt: html }));
        }}
        onBlur={flush}
      />

      <div className={styles.fieldRow}>
        <label className={styles.fieldLabel}>
          Max submissions per player (0 = unlimited)
          <input
            type='number'
            min={0}
            className={styles.numberInput}
            value={maxSubmissions}
            onChange={(e) => {
              const next = Number(e.target.value) || 0;
              setMaxSubmissions(next);
              schedule(buildPatch({ maxSubmissionsPerPlayer: next }));
            }}
            onBlur={flush}
          />
        </label>

        <label className={styles.checkboxLabel}>
          <input
            type='checkbox'
            checked={allowVoting}
            onChange={(e) => {
              const next = e.target.checked;
              setAllowVoting(next);
              schedule(buildPatch({ allowVoting: next }));
            }}
            onBlur={flush}
          />
          Allow upvoting
        </label>

        <label className={styles.checkboxLabel}>
          <input
            type='checkbox'
            checked={autoApprove}
            onChange={(e) => {
              const next = e.target.checked;
              setAutoApprove(next);
              schedule(buildPatch({ autoApprove: next }));
            }}
            onBlur={flush}
          />
          Auto-approve (skip host moderation)
        </label>
      </div>
    </SlideContentWrapper>
  );
};

export { QAndASlideContent };
