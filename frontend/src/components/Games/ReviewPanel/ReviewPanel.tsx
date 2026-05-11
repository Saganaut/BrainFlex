/**
 * Post-showcase review: paginates through each round and shows the question, the
 * correct answer, the aggregate distribution (BarChart for MCQ / FrequencyList for
 * TEXT_INPUT), and an optional per-player breakdown.
 *
 * Driven by GET /api/showcases/{roomCode}/review which is only valid once the
 * showcase is FINISHED. Hides point columns when the host disabled scoring (Pulse).
 */
import { useState } from "react";
import type { RoundReview, ShowcaseReviewDto } from "../../../store/BrainFlexApi";
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

const LABELS = ["A", "B", "C", "D"];

const buildMcqBars = (round: RoundReview): BarChartItem[] => {
  const options = round.options ?? [];
  const distribution = round.mcqDistribution ?? {};
  return options.map((text, idx) => ({
    label: `${LABELS[idx] ?? String(idx + 1)} — ${text}`,
    value: distribution[String(idx)] ?? 0,
    highlight: idx === round.correctOptionIndex,
  }));
};

const buildTextItems = (round: RoundReview): FrequencyListItem[] =>
  (round.textSubmissions ?? []).map((sub) => ({
    text: sub.text ?? "",
    count: sub.count ?? 0,
    correct: sub.isCorrect === true,
  }));

const ReviewPanel = ({ review }: ReviewPanelProps) => {
  const rounds = review.rounds ?? [];
  const [activeIndex, setActiveIndex] = useState(0);
  const [showDetails, setShowDetails] = useState(false);

  if (rounds.length === 0) {
    return <p className={styles.empty}>No rounds to review.</p>;
  }

  const round = rounds[Math.min(activeIndex, rounds.length - 1)];
  const isSlide = round.kind === "SLIDE";
  const isMcq = round.questionType !== "TEXT_INPUT";
  const totalAnswered = (round.playerAnswers ?? []).length;
  const scoringEnabled = review.scoringEnabled !== false;

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
        {isSlide && round.title && (
          <h3 className={styles.questionText}>{round.title}</h3>
        )}
        {!isSlide && (
          <h3 className={styles.questionText}>{round.questionText}</h3>
        )}
        {isSlide && (
          <p className={styles.correctAnswer}>{round.questionText}</p>
        )}
        {!isSlide && round.correctAnswerText && (
          <p className={styles.correctAnswer}>
            <span className={styles.correctLabel}>Correct answer:</span>{" "}
            <strong>{round.correctAnswerText}</strong>
          </p>
        )}
      </div>

      {!isSlide &&
        (isMcq ? (
          <BarChart
            items={buildMcqBars(round)}
            total={totalAnswered}
            caption='Distribution'
          />
        ) : (
          <FrequencyList
            items={buildTextItems(round)}
            total={totalAnswered}
            caption='Submissions'
            emptyMessage='No one submitted an answer.'
          />
        ))}

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
            {(round.playerAnswers ?? []).map((pa) => {
              const ans = renderPlayerAnswer(round, pa);
              return (
                <tr key={pa.userId} className={pa.wasCorrect ? styles.rowCorrect : styles.rowWrong}>
                  <td>{pa.userName}</td>
                  <td>{ans}</td>
                  <td>{pa.wasCorrect ? "✓" : "✗"}</td>
                  {scoringEnabled && <td>{pa.pointsAwarded ?? 0}</td>}
                </tr>
              );
            })}
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

const renderPlayerAnswer = (
  round: RoundReview,
  pa: { selectedOption?: number; textAnswer?: string },
): string => {
  if (round.questionType === "TEXT_INPUT") {
    return pa.textAnswer && pa.textAnswer.length > 0 ? pa.textAnswer : "(timed out)";
  }
  const opt = pa.selectedOption ?? -1;
  if (opt < 0) return "(timed out)";
  const options = round.options ?? [];
  return `${LABELS[opt] ?? String(opt + 1)} — ${options[opt] ?? ""}`;
};

export { ReviewPanel };
