/**
 * Author surface for a Matching question (MatchingQuestion).
 *
 * Players pair items between two columns. Each MatchingPair carries a stable
 * id and content for both sides; scoring matches by pair id (left and right
 * share an id, so the player is correct iff they paired pair[X]'s left side
 * with pair[X]'s right side). The runtime shuffles the right column at
 * presentation time — the editor renders pairs in authoring order so authors
 * can see the truth side-by-side.
 *
 * Both scoring modes are exposed: ALL_OR_NOTHING (binary) and PARTIAL
 * (pro-rated by the count of correct pairings).
 */
import { useState } from "react";
import { MinusIcon } from "@heroicons/react/24/outline";
import { SlideContentWrapper } from "./SlideContentWrapper";
import { RichTextInput } from "@/components/Common/Input/RichTextInput/RichTextInput";
import { Input } from "@/components/Common/Input/Input/Input";
import { NumberInput } from "@/components/Common/Input/NumberInput/NumberInput";
import { RadioGroup } from "@/components/Common/Input/RadioGroup/RadioGroup";
import { Btn } from "@/components/Common/Buttons/Btn";
import { IconBtn } from "@/components/Common/Buttons/IconBtn";
import { useElementEditor } from "./useElementEditor";
import type {
  MatchingPair,
  MatchingQuestion,
} from "@/store/BrainFlexApi";
import styles from "./SlideContentTypes.module.css";
import matchingStyles from "./MatchingSlideContent.module.css";

const isMatching = (e: { kind: string }): e is MatchingQuestion =>
  e.kind === "MatchingQuestion";

const MIN_PAIRS = 2;
const MAX_PAIRS = 10;
const DEFAULT_PAIR_COUNT = 4;
type MatchingScoringMode = NonNullable<MatchingQuestion["scoring"]>;

const seedPairs = (): MatchingPair[] =>
  Array.from({ length: DEFAULT_PAIR_COUNT }, () => ({
    id: crypto.randomUUID(),
    leftLabel: "",
    rightLabel: "",
  }));

const MatchingSlideContent = () => {
  const { element, schedule, flush, commit, syncedFromId, markSynced } =
    useElementEditor<MatchingQuestion>(isMatching);

  const [prompt, setPrompt] = useState(element?.prompt ?? "");
  const [pairs, setPairs] = useState<MatchingPair[]>(() =>
    element?.pairs?.length ? element.pairs : seedPairs(),
  );
  const [scoring, setScoring] = useState<MatchingScoringMode>(
    element?.scoring ?? "ALL_OR_NOTHING",
  );
  const [pointValue, setPointValue] = useState<number>(
    element?.pointValue ?? 100,
  );

  if (element && syncedFromId !== element.id) {
    markSynced(element.id);
    setPrompt(element.prompt ?? "");
    setPairs(element.pairs?.length ? element.pairs : seedPairs());
    setScoring(element.scoring ?? "ALL_OR_NOTHING");
    setPointValue(element.pointValue ?? 100);
  }

  if (!element) {
    return (
      <SlideContentWrapper title='Matching'>
        <p>Select a slide to edit.</p>
      </SlideContentWrapper>
    );
  }

  const buildPatch = (
    overrides: Partial<MatchingQuestion>,
  ): MatchingQuestion => ({
    ...element,
    prompt,
    pairs,
    scoring,
    pointValue,
    ...overrides,
  });

  const handleAddPair = () => {
    if (pairs.length >= MAX_PAIRS) return;
    flush();
    const next: MatchingPair[] = [
      ...pairs,
      { id: crypto.randomUUID(), leftLabel: "", rightLabel: "" },
    ];
    setPairs(next);
    commit(buildPatch({ pairs: next }));
  };

  const handleRemovePair = (id: string) => {
    if (pairs.length <= MIN_PAIRS) return;
    flush();
    const next = pairs.filter((p) => p.id !== id);
    setPairs(next);
    commit(buildPatch({ pairs: next }));
  };

  const handlePairChange = (
    id: string,
    side: "leftLabel" | "rightLabel",
    value: string,
  ) => {
    const next = pairs.map((p) => (p.id === id ? { ...p, [side]: value } : p));
    setPairs(next);
    schedule(buildPatch({ pairs: next }));
  };

  return (
    <SlideContentWrapper
      title='Matching'
      description='Players pair items across two columns. Authoring order is the answer key — the right column is shuffled visually at game time.'>
      <RichTextInput
        label='Question'
        id={`match-prompt-${element.id ?? ""}`}
        placeholder='Match each river to its continent.'
        value={prompt}
        onChange={(html) => {
          setPrompt(html);
          schedule(buildPatch({ prompt: html }));
        }}
        onBlur={flush}
      />

      <div className={styles.fieldRow}>
        <NumberInput
          label='Points'
          id={`match-points-${element.id ?? ""}`}
          min={0}
          value={pointValue}
          onChange={(next) => {
            setPointValue(next);
            schedule(buildPatch({ pointValue: next }));
          }}
          onBlur={flush}
        />
        <RadioGroup
          name={`match-scoring-${element.id ?? ""}`}
          legend='Scoring'
          options={[
            { value: "ALL_OR_NOTHING", label: "All or nothing" },
            { value: "PARTIAL", label: "Partial credit" },
          ]}
          value={scoring}
          onChange={(next) => {
            const mode = next as MatchingScoringMode;
            setScoring(mode);
            commit(buildPatch({ scoring: mode }));
          }}
        />
      </div>

      <div className={styles.sectionHeader}>
        <span className={styles.sectionLabel}>Pairs (left ↔ right)</span>
        <Btn
          size='sm'
          onClick={handleAddPair}
          disabled={pairs.length >= MAX_PAIRS}>
          + Add pair
        </Btn>
      </div>

      <div className={styles.itemList}>
        {pairs.map((pair, idx) => (
          <div
            key={pair.id}
            className={`${styles.itemRow} ${matchingStyles.pairRow}`}>
            <span className={styles.itemRowIndex}>{idx + 1}</span>
            <div className={matchingStyles.pairFields}>
              <Input
                type='text'
                fullWidth
                value={pair.leftLabel ?? ""}
                placeholder={`Left ${(idx + 1).toString()}`}
                onChange={(e) => {
                  if (pair.id) {
                    handlePairChange(pair.id, "leftLabel", e.target.value);
                  }
                }}
                onBlur={flush}
              />
              <span className={matchingStyles.pairArrow} aria-hidden='true'>
                ↔
              </span>
              <Input
                type='text'
                fullWidth
                value={pair.rightLabel ?? ""}
                placeholder={`Right ${(idx + 1).toString()}`}
                onChange={(e) => {
                  if (pair.id) {
                    handlePairChange(pair.id, "rightLabel", e.target.value);
                  }
                }}
                onBlur={flush}
              />
            </div>
            <IconBtn
              variant='bordered'
              size='xs'
              icon={<MinusIcon />}
              aria-label={`Remove pair ${(idx + 1).toString()}`}
              disabled={pairs.length <= MIN_PAIRS}
              onClick={() => {
                if (pair.id) handleRemovePair(pair.id);
              }}
            />
          </div>
        ))}
      </div>

      {/* TODO: Get more specs — per-side image picker (Lorem Picsum
          placeholder + media library), drag-to-reorder, best-answer modifier. */}
    </SlideContentWrapper>
  );
};

export { MatchingSlideContent };
