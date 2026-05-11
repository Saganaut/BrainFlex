/**
 * Post-showcase review. Paginates through every element played and renders
 * per-kind summaries:
 *   SLIDE     — title + body (no answers to aggregate)
 *   MCQ / IMAGE_CHOICE — horizontal bar chart of option-id counts
 *   TEXT      — frequency list of unique submissions
 *   NUMBER    — list of submitted values sorted by frequency
 *   other     — just the totals + a per-player breakdown
 *
 * Aggregation is computed client-side from the per-player payloads so we don't
 * need a backend-side per-kind aggregator (each round-result already carries
 * every submission). Per-player details are collapsible.
 */
import { useState } from "react";
import type {
  PlayerRoundDetail,
  RoundReview,
  ShowcaseReviewDto,
} from "../../../store/BrainFlexApi";
import type { AnswerPayload, DeckElement } from "../../../types/elements";
import { BarChart, type BarChartItem } from "@/components/Common/Charts/BarChart/BarChart";
import {
  FrequencyList,
  type FrequencyListItem,
} from "@/components/Common/Charts/FrequencyList/FrequencyList";
import { Btn } from "@/components/Common/Buttons/Btn";
import styles from "./ReviewPanel.module.css";

export interface ReviewPanelProps {
  review: ShowcaseReviewDto;
}

const ReviewPanel = ({ review }: ReviewPanelProps) => {
  const rounds = review.rounds ?? [];
  const [activeIndex, setActiveIndex] = useState(0);
  const [showDetails, setShowDetails] = useState(false);

  if (rounds.length === 0) {
    return <p className={styles.empty}>No rounds to review.</p>;
  }
  const round = rounds[Math.min(activeIndex, rounds.length - 1)];
  const element: DeckElement | undefined = round.element;
  const scoringEnabled = review.scoringEnabled !== false;
  const isSlide = element?.kind === "Slide";
  const aggregateView = element ? renderAggregate(round, element) : null;

  return (
    <div className={styles.panel}>
      <div className={styles.pager} role='tablist' aria-label='Round'>
        {rounds.map((_, i) => (
          <button
            // eslint-disable-next-line react-x/no-array-index-key -- round index is the identity
            key={i}
            type='button'
            role='tab'
            aria-selected={i === activeIndex}
            className={`${styles.pagerBtn} ${i === activeIndex ? styles.pagerActive : ""}`}
            onClick={() => {
              setActiveIndex(i);
            }}>
            {i + 1}
          </button>
        ))}
      </div>

      <div className={styles.questionHeader}>
        <span className={styles.roundLabel}>
          {isSlide ? "Slide" : "Round"} {activeIndex + 1} / {rounds.length}
        </span>
        {element && <h3 className={styles.questionText}>{titleFor(element)}</h3>}
        {element && bodyFor(element) && (
          <p className={styles.correctAnswer}>{bodyFor(element)}</p>
        )}
      </div>

      {!isSlide && aggregateView}

      {!isSlide && (round.timedOutCount ?? 0) > 0 && (
        <p className={styles.timedOut}>
          {round.timedOutCount} player(s) didn&apos;t answer in time.
        </p>
      )}

      {!isSlide && (
        <Btn
          size='sm'
          type='button'
          onClick={() => {
            setShowDetails((prev) => !prev);
          }}>
          {showDetails ? "Hide" : "Show"} per-player details
        </Btn>
      )}

      {!isSlide && showDetails && (
        <table className={styles.details}>
          <thead>
            <tr>
              <th scope='col'>Player</th>
              <th scope='col'>Answer</th>
              <th scope='col'>Result</th>
              {scoringEnabled && <th scope='col'>Points</th>}
            </tr>
          </thead>
          <tbody>
            {(round.playerAnswers ?? []).map((pa) => (
              <tr
                key={pa.userId}
                className={pa.wasCorrect ? styles.rowCorrect : styles.rowWrong}>
                <td>{pa.userName}</td>
                <td>{renderSubmission(element, pa.payload)}</td>
                <td>{pa.wasCorrect ? "✓" : "✗"}</td>
                {scoringEnabled && <td>{pa.pointsAwarded ?? 0}</td>}
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <div className={styles.navBtns}>
        <Btn
          size='sm'
          type='button'
          disabled={activeIndex === 0}
          onClick={() => {
            setActiveIndex((i) => Math.max(0, i - 1));
          }}>
          ← Previous
        </Btn>
        <Btn
          size='sm'
          type='button'
          disabled={activeIndex >= rounds.length - 1}
          onClick={() => {
            setActiveIndex((i) => Math.min(rounds.length - 1, i + 1));
          }}>
          Next →
        </Btn>
      </div>
    </div>
  );
};

// ─── per-kind rendering ───────────────────────────────────────────────────────

const titleFor = (e: DeckElement): string => {
  if (e.kind === "Slide") return e.title ?? "(untitled slide)";
  return ("prompt" in e ? (e.prompt ?? "") : "");
};

const bodyFor = (e: DeckElement): string | undefined => {
  if (e.kind === "Slide") return e.body ?? undefined;
  return undefined;
};

const renderAggregate = (round: RoundReview, element: DeckElement) => {
  const answers: PlayerRoundDetail[] = round.playerAnswers ?? [];
  switch (element.kind) {
    case "McqQuestion":
    case "ImageChoiceQuestion": {
      const options = element.options ?? [];
      const counts = new Map<string, number>();
      for (const a of answers) {
        const p = a.payload;
        if ((p?.kind === "McqAnswer" || p?.kind === "ImageChoiceAnswer") && p.optionId) {
          counts.set(p.optionId, (counts.get(p.optionId) ?? 0) + 1);
        }
      }
      const bars: BarChartItem[] = options.map((o) => ({
        label: o.text ?? "",
        value: counts.get(o.id ?? "") ?? 0,
        highlight: o.id === element.correctOptionId,
      }));
      return <BarChart items={bars} total={answers.length} caption='Distribution' />;
    }
    case "TextQuestion": {
      const counts = new Map<string, number>();
      for (const a of answers) {
        const p = a.payload;
        if (p?.kind === "TextAnswer" && p.text) {
          const key = p.text.trim();
          counts.set(key, (counts.get(key) ?? 0) + 1);
        }
      }
      const items: FrequencyListItem[] = [...counts.entries()]
        .sort((a, b) => b[1] - a[1])
        .map(([text, count]) => ({
          text,
          count,
          correct:
            text.toLowerCase() === element.correctAnswer?.toLowerCase(),
        }));
      return (
        <FrequencyList
          items={items}
          total={answers.length}
          caption='Submissions'
          emptyMessage='No one submitted.'
        />
      );
    }
    case "NumberQuestion": {
      const counts = new Map<string, number>();
      for (const a of answers) {
        const p = a.payload;
        if (p?.kind === "NumberAnswer" && p.value !== undefined) {
          const key = String(p.value);
          counts.set(key, (counts.get(key) ?? 0) + 1);
        }
      }
      const items: FrequencyListItem[] = [...counts.entries()]
        .sort((a, b) => Number(a[0]) - Number(b[0]))
        .map(([text, count]) => ({
          text: text + (element.unitLabel ?? ""),
          count,
          correct: Number(text) === element.correctValue,
        }));
      return (
        <FrequencyList
          items={items}
          total={answers.length}
          caption='Guesses'
          emptyMessage='No one guessed.'
        />
      );
    }
    default:
      return (
        <p className={styles.timedOut}>
          Aggregate view for {element.kind} coming soon.
        </p>
      );
  }
};

const renderSubmission = (
  element: DeckElement | undefined,
  payload: AnswerPayload | undefined,
): string => {
  if (!payload) return "(no answer)";
  switch (payload.kind) {
    case "TimeoutAnswer":
      return "(timed out)";
    case "TextAnswer":
      return payload.text ?? "(empty)";
    case "NumberAnswer":
      return payload.value !== undefined ? String(payload.value) : "(empty)";
    case "McqAnswer":
    case "ImageChoiceAnswer": {
      if (element && (element.kind === "McqQuestion" || element.kind === "ImageChoiceQuestion")) {
        const opt = element.options?.find((o) => o.id === payload.optionId);
        return opt?.text ?? payload.optionId ?? "(unknown)";
      }
      return payload.optionId ?? "(unknown)";
    }
    default:
      return `(${payload.kind})`;
  }
};

export { ReviewPanel };
