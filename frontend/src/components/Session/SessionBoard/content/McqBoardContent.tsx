// MCQ presentation surface for the board. A single component covers every
// moment, switched by `mode`:
//   - prompt      → option cards; tappable when `interactive` (participant on
//                   their own device), otherwise read-only (projected / host).
//   - liveResults → cards with the response tally filling in; still tappable for
//                   a participant who hasn't answered yet.
//   - results     → cards with the final distribution + the correct answer(s)
//                   highlighted.
//
// `distribution` (optionId → response count) is the seam for real data: it
// arrives over the round-result WebSocket broadcast once the board is wired off
// mock data. Until then the results modes still render honestly — correct-answer
// highlighting is real (it's on the element), and the bars simply read empty.
import { useState } from "react";
import type { McqQuestion } from "@/types/elements";
import type { BoardQuestionMode } from "../resolveBoardStage";
import { useFlushOnClosing } from "./useFlushOnClosing";
import styles from "./McqBoardContent.module.css";

interface McqBoardContentProps {
  question: McqQuestion;
  mode: BoardQuestionMode;
  interactive: boolean;
  /** optionId → number of responses. Absent until wired to the live broadcast. */
  distribution?: Record<string, number>;
}

const McqBoardContent = ({
  question,
  mode,
  interactive,
  distribution,
}: McqBoardContentProps) => {
  const options = question.options ?? [];
  const correctIds = new Set(question.correctOptionIds ?? []);
  const maxSelections =
    question.allowMultipleSelect === true
      ? (question.maxSelections ?? options.length)
      : 1;

  // Local selection only — the board runs on mock data today, so this stands
  // in for a submitted answer. The seam to the real `sendAnswer` is this state.
  const [selected, setSelected] = useState<Set<string>>(() => new Set());

  // Chunk 25 — when the host ends the submit phase, flush this device's current
  // selection as the answer. The submit path itself is still mock (see the
  // `selected` seam above), so this is wired but inert until answers go live;
  // it consumes the closing signal regardless so the one-shot doesn't dangle.
  useFlushOnClosing(question.id, () => {
    if (!interactive || selected.size === 0) return;
    // TODO(answers-live): sendAnswer(question.id, { kind: "McqAnswer", optionIds: [...selected] })
  });

  const showResults = mode === "results" || mode === "liveResults";
  const revealCorrect = mode === "results";
  const canSelect = interactive && mode !== "results";

  const totalResponses = Object.values(distribution ?? {}).reduce(
    (sum, n) => sum + n,
    0,
  );

  const toggle = (id: string) => {
    if (!canSelect) return;
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
        return next;
      }
      // Single-select replaces; multi-select respects the cap.
      if (maxSelections === 1) return new Set([id]);
      if (next.size >= maxSelections) return prev;
      next.add(id);
      return next;
    });
  };

  // Even two-column grid, matching the editor's option layout.
  const columns = options.length ? Math.max(Math.ceil(options.length / 2), 2) : 2;

  return (
    <div
      className={styles.mcqBoardContent}
      style={{ "--cols": columns } as React.CSSProperties}>
      {options.map((option) => {
        const id = option.id ?? "";
        const isSelected = selected.has(id);
        const isCorrect = revealCorrect && correctIds.has(id);
        const count = distribution?.[id] ?? 0;
        const pct =
          totalResponses > 0 ? Math.round((count / totalResponses) * 100) : 0;

        const classes = [
          styles.option,
          isSelected ? styles.selected : "",
          isCorrect ? styles.correct : "",
          canSelect ? styles.selectable : "",
        ]
          .filter(Boolean)
          .join(" ");

        return (
          <button
            key={id}
            type='button'
            className={classes}
            disabled={!canSelect}
            aria-pressed={canSelect ? isSelected : undefined}
            onClick={() => {
              toggle(id);
            }}
            style={
              { "--option-accent": option.color ?? "var(--bg-brand)" } as React.CSSProperties
            }>
            {showResults && (
              <span
                className={styles.bar}
                style={{ width: `${pct.toString()}%` }}
                aria-hidden='true'
              />
            )}
            <span className={styles.label}>{option.text}</span>
            {showResults && (
              <span className={styles.pct}>{pct.toString()}%</span>
            )}
          </button>
        );
      })}
    </div>
  );
};

export { McqBoardContent };
