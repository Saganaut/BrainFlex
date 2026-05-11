/**
 * Overlay shown after each round ends — reveals the correct answer, the
 * player's own outcome, and the per-player tally. Turn-based hosts see a
 * Next Round button; simultaneous-mode rounds auto-advance.
 *
 * Pulls the "correct answer" text out of the un-redacted element on the
 * result payload via a small helper so it can render every kind that ships
 * with an answer key.
 */
import styles from "./RoundResult.module.css";
import type { RoundResultPayload } from "../../../store/gameSlice";
import type { AnswerPayload, DeckElement } from "../../../types/elements";
import { Btn } from "@/components/Common/Buttons/Btn";

interface RoundResultProps {
  result: RoundResultPayload;
  currentUserId?: string;
  isHost: boolean;
  isTurnBased: boolean;
  onNextRound: () => void;
}

const RoundResult = ({
  result,
  currentUserId,
  isHost,
  isTurnBased,
  onNextRound,
}: RoundResultProps) => {
  const myResult = result.playerResults.find((r) => r.userId === currentUserId);
  const sorted = [...result.playerResults].sort(
    (a, b) => b.totalScore - a.totalScore,
  );

  const correctText = correctAnswerText(result.element);
  const renderPlayerSubmission = (payload: AnswerPayload | null | undefined) =>
    payload ? humanReadableAnswer(result.element, payload) : null;

  return (
    <div className={styles.overlay}>
      <div className={styles.panel}>
        <h2 className={styles.title}>Round {result.round + 1}</h2>

        {correctText && (
          <div className={styles.answer}>
            <span className={styles.answerLabel}>Correct answer</span>
            <span className={styles.answerText}>{correctText}</span>
          </div>
        )}

        {myResult && (
          <div
            className={`${styles.myResult} ${myResult.wasCorrect ? styles.myCorrect : styles.myWrong}`}>
            {renderPlayerSubmission(myResult.payload) && (
              <div className={styles.mySubmitted}>
                Your answer:{" "}
                <strong>{renderPlayerSubmission(myResult.payload)}</strong>
              </div>
            )}
            {myResult.wasCorrect
              ? `Correct! +${myResult.pointsAwarded} pts`
              : "Incorrect"}
          </div>
        )}

        <ol className={styles.results}>
          {sorted.map((r) => {
            const text = renderPlayerSubmission(r.payload);
            return (
              <li
                key={r.userId}
                className={`${styles.resultRow} ${r.userId === currentUserId ? styles.me : ""}`}>
                <span className={styles.playerName}>
                  {r.userName}
                  {text && (
                    <span className={styles.answerHint}> — “{text}”</span>
                  )}
                </span>
                <span
                  className={`${styles.badge} ${r.wasCorrect ? styles.badgeCorrect : styles.badgeWrong}`}>
                  {r.wasCorrect ? `+${r.pointsAwarded}` : "x"}
                </span>
                <span className={styles.total}>{r.totalScore}</span>
              </li>
            );
          })}
        </ol>

        {isHost && isTurnBased ? (
          <Btn onClick={onNextRound} className={styles.nextBtn}>
            Next Round
          </Btn>
        ) : (
          <p className={styles.autoAdvance}>Next round starting soon…</p>
        )}
      </div>
    </div>
  );
};

/** Resolves the canonical correct answer for display per element kind. */
const correctAnswerText = (element: DeckElement): string | null => {
  switch (element.kind) {
    case "McqQuestion":
    case "ImageChoiceQuestion": {
      const correct = element.options?.find(
        (o) => o.id === element.correctOptionId,
      );
      return correct?.text ?? null;
    }
    case "TextQuestion":
      return element.correctAnswer ?? null;
    case "NumberQuestion":
      return element.correctValue !== undefined
        ? `${element.correctValue}${element.unitLabel ?? ""}`
        : null;
    default:
      return null;
  }
};

/** Renders a human-readable string for what the player submitted. */
const humanReadableAnswer = (
  element: DeckElement,
  payload: AnswerPayload,
): string | null => {
  switch (payload.kind) {
    case "TimeoutAnswer":
      return "(timed out)";
    case "TextAnswer":
      return payload.text ?? null;
    case "NumberAnswer":
      return payload.value !== undefined ? String(payload.value) : null;
    case "McqAnswer":
    case "ImageChoiceAnswer": {
      if (element.kind === "McqQuestion" || element.kind === "ImageChoiceQuestion") {
        const opt = element.options?.find((o) => o.id === payload.optionId);
        return opt?.text ?? null;
      }
      return payload.optionId ?? null;
    }
    default:
      return null;
  }
};

export { RoundResult };
