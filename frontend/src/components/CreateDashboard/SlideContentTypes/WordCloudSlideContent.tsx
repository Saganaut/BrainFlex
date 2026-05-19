/**
 * Author surface for a Word Cloud survey (WordCloudQuestion).
 *
 * Players submit short words/phrases that the room aggregates into a live
 * frequency cloud. Survey-only — never scored. The author controls how many
 * submissions each player can make, the per-word length cap, case-folding,
 * and a host-supplied banned-words list (comma-separated input). The
 * profanity-filter flag is exposed as a checkbox; the v1 backend honors it
 * only to the extent that it owns a small built-in list.
 */
import { useState } from "react";
import { SlideContentWrapper } from "./SlideContentWrapper";
import { RichTextInput } from "@/components/Common/Input/RichTextInput/RichTextInput";
import { Input } from "@/components/Common/Input/Input/Input";
import { NumberInput } from "@/components/Common/Input/NumberInput/NumberInput";
import { Checkbox } from "@/components/Common/Input/Checkbox/Checkbox";
import { useElementEditor } from "./useElementEditor";
import type { WordCloudQuestion } from "@/store/BrainFlexApi";
import styles from "./SlideContentTypes.module.css";

const isWordCloud = (e: { kind: string }): e is WordCloudQuestion =>
  e.kind === "WordCloudQuestion";

const bannedToInput = (banned: string[] | undefined) =>
  (banned ?? []).join(", ");
const inputToBanned = (raw: string) =>
  raw
    .split(",")
    .map((s) => s.trim())
    .filter((s) => s !== "");

const WordCloudSlideContent = () => {
  const { element, schedule, flush, syncedFromId, markSynced } =
    useElementEditor<WordCloudQuestion>(isWordCloud);

  const [prompt, setPrompt] = useState(element?.prompt ?? "");
  const [maxSubmissions, setMaxSubmissions] = useState<number>(
    element?.maxSubmissionsPerPlayer ?? 3,
  );
  const [maxWordLength, setMaxWordLength] = useState<number>(
    element?.maxWordLength ?? 30,
  );
  const [caseSensitive, setCaseSensitive] = useState<boolean>(
    element?.caseSensitive ?? false,
  );
  const [profanityFilter, setProfanityFilter] = useState<boolean>(
    element?.profanityFilter ?? true,
  );
  const [bannedText, setBannedText] = useState<string>(() =>
    bannedToInput(element?.bannedWords),
  );

  if (element && syncedFromId !== element.id) {
    markSynced(element.id);
    setPrompt(element.prompt ?? "");
    setMaxSubmissions(element.maxSubmissionsPerPlayer ?? 3);
    setMaxWordLength(element.maxWordLength ?? 30);
    setCaseSensitive(element.caseSensitive ?? false);
    setProfanityFilter(element.profanityFilter ?? true);
    setBannedText(bannedToInput(element.bannedWords));
  }

  if (!element) {
    return (
      <SlideContentWrapper title='Word Cloud'>
        <p>Select a slide to edit.</p>
      </SlideContentWrapper>
    );
  }

  const buildPatch = (
    overrides: Partial<WordCloudQuestion>,
  ): WordCloudQuestion => ({
    ...element,
    prompt,
    maxSubmissionsPerPlayer: maxSubmissions,
    maxWordLength,
    caseSensitive,
    profanityFilter,
    bannedWords: inputToBanned(bannedText),
    ...overrides,
  });

  return (
    <SlideContentWrapper
      title='Word Cloud'
      description='Open-ended survey. Each player submits short words that the room aggregates into a live cloud — never scored.'>
      <RichTextInput
        label='Question'
        id={`wc-prompt-${element.id ?? ""}`}
        placeholder='What word describes Mondays?'
        value={prompt}
        onChange={(html) => {
          setPrompt(html);
          schedule(buildPatch({ prompt: html }));
        }}
        onBlur={flush}
      />

      <div className={styles.fieldRow}>
        <NumberInput
          label='Submissions per player'
          id={`wc-max-${element.id ?? ""}`}
          min={1}
          value={maxSubmissions}
          onChange={(next) => {
            setMaxSubmissions(next);
            schedule(buildPatch({ maxSubmissionsPerPlayer: next }));
          }}
          onBlur={flush}
        />

        <NumberInput
          label='Max word length'
          id={`wc-len-${element.id ?? ""}`}
          min={1}
          value={maxWordLength}
          onChange={(next) => {
            setMaxWordLength(next);
            schedule(buildPatch({ maxWordLength: next }));
          }}
          onBlur={flush}
        />
      </div>

      <div className={styles.fieldRow}>
        <Checkbox
          label='Case sensitive (treat "Monday" and "monday" as different)'
          id={`wc-case-${element.id ?? ""}`}
          checked={caseSensitive}
          onChange={(e) => {
            const next = e.target.checked;
            setCaseSensitive(next);
            schedule(buildPatch({ caseSensitive: next }));
          }}
        />

        <Checkbox
          label='Profanity filter'
          id={`wc-prof-${element.id ?? ""}`}
          checked={profanityFilter}
          onChange={(e) => {
            const next = e.target.checked;
            setProfanityFilter(next);
            schedule(buildPatch({ profanityFilter: next }));
          }}
        />
      </div>

      <Input
        label='Banned words (comma-separated)'
        id={`wc-banned-${element.id ?? ""}`}
        type='text'
        value={bannedText}
        placeholder='e.g. spam, lol'
        onChange={(e) => {
          const next = e.target.value;
          setBannedText(next);
          schedule(buildPatch({ bannedWords: inputToBanned(next) }));
        }}
        onBlur={flush}
      />
    </SlideContentWrapper>
  );
};

export { WordCloudSlideContent };
