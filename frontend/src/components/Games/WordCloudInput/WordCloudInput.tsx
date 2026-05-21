/**
 * Player input for a Word Cloud round.
 *
 * The author's question caps each player at N words (`maxSubmissionsPerPlayer`)
 * and a per-word length limit (`maxWordLength`). The player drafts a batch of
 * words client-side — adding via Enter or the Add button, removing via the
 * chip's X — and submits the whole batch with the Submit button. The server
 * normalizes the batch (lower-cases unless caseSensitive, drops banned words,
 * caps at the limit) and broadcasts the rolled-up cloud on /wordCloud.
 *
 * Once submitted the chips lock and the input disappears; the live cloud is
 * rendered by the parent renderer (the input view stays compact next to it).
 */
import { useState } from "react";
import { XMarkIcon } from "@heroicons/react/24/outline";
import { Btn } from "@/components/Common/Buttons/Btn";
import { IconBtn } from "@/components/Common/Buttons/IconBtn";
import { Input } from "@/components/Common/Input/Input/Input";
import type {
  WordCloudAnswer,
  WordCloudQuestion,
} from "@/types/elements";
import styles from "./WordCloudInput.module.css";

interface WordCloudInputProps {
  element: WordCloudQuestion;
  submittedWords: string[] | null;
  onSubmit: (payload: WordCloudAnswer) => void;
  disabled: boolean;
}

const WordCloudInput = ({
  element,
  submittedWords,
  onSubmit,
  disabled,
}: WordCloudInputProps) => {
  const [draftWord, setDraftWord] = useState("");
  const [draftBatch, setDraftBatch] = useState<string[]>([]);

  const maxPerPlayer = element.maxSubmissionsPerPlayer ?? 3;
  const maxWordLen = element.maxWordLength ?? 30;
  const locked = submittedWords !== null || disabled;
  const atCap = draftBatch.length >= maxPerPlayer;

  const wordsToShow = submittedWords ?? draftBatch;

  const addWord = () => {
    const trimmed = draftWord.trim();
    if (!trimmed) return;
    if (atCap) return;
    setDraftBatch((prev) => [...prev, trimmed.slice(0, maxWordLen)]);
    setDraftWord("");
  };

  const removeWord = (idx: number) => {
    setDraftBatch((prev) => prev.filter((_, i) => i !== idx));
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      e.preventDefault();
      addWord();
    }
  };

  const handleSubmit = (e: React.SubmitEvent) => {
    e.preventDefault();
    if (locked) return;
    // Include any unsubmitted trailing word, so a player who hit Submit without
    // pressing Enter still ships what they were typing.
    const trailing = draftWord.trim();
    const final = trailing
      ? [...draftBatch, trailing.slice(0, maxWordLen)]
      : draftBatch;
    if (final.length === 0) return;
    onSubmit({ kind: "WordCloudAnswer", words: final });
  };

  return (
    <form className={styles.wrapper} onSubmit={handleSubmit}>
      <label htmlFor='word-cloud-input' className={styles.label}>
        Type up to {maxPerPlayer} {maxPerPlayer === 1 ? "word" : "words"}
      </label>

      <div className={styles.row}>
        <Input
          id='word-cloud-input'
          className={styles.field}
          value={draftWord}
          onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
            setDraftWord(e.target.value.slice(0, maxWordLen));
          }}
          onKeyDown={handleKeyDown}
          placeholder='A word or phrase…'
          autoComplete='off'
          autoFocus
          disabled={locked || atCap}
          maxLength={maxWordLen}
        />
        <Btn
          type='button'
          onClick={addWord}
          disabled={locked || atCap || draftWord.trim().length === 0}>
          Add
        </Btn>
      </div>

      {wordsToShow.length > 0 && (
        <ul className={styles.chips} aria-label='Words in your submission'>
          {wordsToShow.map((w, idx) => (
            <li key={`${w}-${String(idx)}`} className={styles.chip}>
              <span>{w}</span>
              {!locked && (
                <IconBtn
                  variant='secondary'
                  fill='ghost'
                  size='xs'
                  className={styles.chipRemove}
                  icon={<XMarkIcon />}
                  aria-label={`Remove ${w}`}
                  onClick={() => {
                    removeWord(idx);
                  }}
                />
              )}
            </li>
          ))}
        </ul>
      )}

      {!locked && (
        <Btn
          type='submit'
          disabled={
            draftBatch.length === 0 && draftWord.trim().length === 0
          }>
          Submit {wordsToShow.length > 0 ? `(${String(wordsToShow.length)})` : ""}
        </Btn>
      )}

      {submittedWords !== null && (
        <p className={styles.waiting}>
          Locked in. Waiting for the round to end…
        </p>
      )}
    </form>
  );
};

export { WordCloudInput };
