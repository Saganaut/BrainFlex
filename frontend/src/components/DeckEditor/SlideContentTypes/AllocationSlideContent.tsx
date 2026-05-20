/**
 * Author surface for an Allocation survey (AllocationQuestion).
 *
 * Players distribute a fixed pool of points across N options to express
 * weighted preferences (Mentimeter "100 Points" pattern). Survey-only —
 * never scored. The editor exposes the prompt, an option list (reusing the
 * MCQ option shape so the schema stays cheap), the pool total, and the two
 * submission-validation toggles.
 *
 * The on-screen options list is the source of truth for backend persistence:
 * each entry carries an id, label, and color the player view uses to render
 * its sliders / number inputs.
 */
import { useState } from "react";
import { MinusIcon } from "@heroicons/react/24/outline";
import { SlideContentWrapper } from "./SlideContentWrapper";
import { RichTextInput } from "@/components/Common/Input/RichTextInput/RichTextInput";
import { Input } from "@/components/Common/Input/Input/Input";
import { NumberInput } from "@/components/Common/Input/NumberInput/NumberInput";
import { Checkbox } from "@/components/Common/Input/Checkbox/Checkbox";
import { Btn } from "@/components/Common/Buttons/Btn";
import { IconBtn } from "@/components/Common/Buttons/IconBtn";
import { useElementEditor } from "./useElementEditor";
import type {
  AllocationQuestion,
  McqOption,
} from "@/store/BrainFlexApi";
import styles from "./SlideContentTypes.module.css";

const isAllocation = (e: { kind: string }): e is AllocationQuestion =>
  e.kind === "AllocationQuestion";

const MIN_OPTIONS = 2;
const MAX_OPTIONS = 8;
const DEFAULT_OPTION_COUNT = 4;

const seedOptions = (): McqOption[] =>
  Array.from({ length: DEFAULT_OPTION_COUNT }, () => ({
    id: crypto.randomUUID(),
    text: "",
  }));

const AllocationSlideContent = () => {
  const { element, schedule, flush, commit, syncedFromId, markSynced } =
    useElementEditor<AllocationQuestion>(isAllocation);

  const [prompt, setPrompt] = useState(element?.prompt ?? "");
  const [options, setOptions] = useState<McqOption[]>(() =>
    element?.options?.length ? element.options : seedOptions(),
  );
  const [totalPoints, setTotalPoints] = useState<number>(
    element?.totalPointsToDistribute ?? 100,
  );
  const [allowZero, setAllowZero] = useState<boolean>(
    element?.allowZeroOnItem ?? true,
  );
  const [enforceTotal, setEnforceTotal] = useState<boolean>(
    element?.enforceExactTotal ?? true,
  );

  if (element && syncedFromId !== element.id) {
    markSynced(element.id);
    setPrompt(element.prompt ?? "");
    setOptions(element.options?.length ? element.options : seedOptions());
    setTotalPoints(element.totalPointsToDistribute ?? 100);
    setAllowZero(element.allowZeroOnItem ?? true);
    setEnforceTotal(element.enforceExactTotal ?? true);
  }

  if (!element) {
    return (
      <SlideContentWrapper title='Allocation'>
        <p>Select a slide to edit.</p>
      </SlideContentWrapper>
    );
  }

  const buildPatch = (
    overrides: Partial<AllocationQuestion>,
  ): AllocationQuestion => ({
    ...element,
    prompt,
    options,
    totalPointsToDistribute: totalPoints,
    allowZeroOnItem: allowZero,
    enforceExactTotal: enforceTotal,
    ...overrides,
  });

  const handleAddOption = () => {
    if (options.length >= MAX_OPTIONS) return;
    flush();
    const next = [...options, { id: crypto.randomUUID(), text: "" }];
    setOptions(next);
    commit(buildPatch({ options: next }));
  };

  const handleRemoveOption = (id: string) => {
    if (options.length <= MIN_OPTIONS) return;
    flush();
    const next = options.filter((o) => o.id !== id);
    setOptions(next);
    commit(buildPatch({ options: next }));
  };

  const handleOptionLabelChange = (id: string, text: string) => {
    const next = options.map((o) => (o.id === id ? { ...o, text } : o));
    setOptions(next);
    schedule(buildPatch({ options: next }));
  };

  return (
    <SlideContentWrapper
      title='Allocation'
      description='Players distribute a fixed pool of points across the options to show weighted preferences — never scored.'>
      <RichTextInput
        label='Question'
        id={`alloc-prompt-${element.id ?? ""}`}
        placeholder='How would you split your budget?'
        value={prompt}
        onChange={(html) => {
          setPrompt(html);
          schedule(buildPatch({ prompt: html }));
        }}
        onBlur={flush}
      />

      <div className={styles.fieldRow}>
        <NumberInput
          label='Total points to distribute'
          id={`alloc-total-${element.id ?? ""}`}
          min={1}
          value={totalPoints}
          onChange={(next) => {
            setTotalPoints(next);
            schedule(buildPatch({ totalPointsToDistribute: next }));
          }}
          onBlur={flush}
        />
      </div>

      <div className={styles.fieldRow}>
        <Checkbox
          label='Allow 0 points on an option'
          id={`alloc-zero-${element.id ?? ""}`}
          checked={allowZero}
          onChange={(e) => {
            const next = e.target.checked;
            setAllowZero(next);
            schedule(buildPatch({ allowZeroOnItem: next }));
          }}
        />
        <Checkbox
          label='Submissions must sum to the total'
          id={`alloc-exact-${element.id ?? ""}`}
          checked={enforceTotal}
          onChange={(e) => {
            const next = e.target.checked;
            setEnforceTotal(next);
            schedule(buildPatch({ enforceExactTotal: next }));
          }}
        />
      </div>

      <div className={styles.sectionHeader}>
        <span className={styles.sectionLabel}>Options</span>
        <Btn
          size='sm'
          onClick={handleAddOption}
          disabled={options.length >= MAX_OPTIONS}>
          + Add option
        </Btn>
      </div>

      <div className={styles.itemList}>
        {options.map((option, idx) => (
          <div key={option.id} className={styles.itemRow}>
            <span className={styles.itemRowIndex}>{idx + 1}</span>
            <div className={styles.itemRowField}>
              <Input
                type='text'
                fullWidth
                value={option.text ?? ""}
                placeholder={`Option ${(idx + 1).toString()}`}
                onChange={(e) => {
                  if (option.id) {
                    handleOptionLabelChange(option.id, e.target.value);
                  }
                }}
                onBlur={flush}
              />
            </div>
            <IconBtn
              fill='bordered'
              size='xs'
              icon={<MinusIcon />}
              aria-label={`Remove option ${(idx + 1).toString()}`}
              disabled={options.length <= MIN_OPTIONS}
              onClick={() => {
                if (option.id) handleRemoveOption(option.id);
              }}
            />
          </div>
        ))}
      </div>

      {/* TODO: Get more specs — per-option color picker / image picker
          (Lorem Picsum placeholder + media library) reuse from MCQ. */}
    </SlideContentWrapper>
  );
};

export { AllocationSlideContent };
