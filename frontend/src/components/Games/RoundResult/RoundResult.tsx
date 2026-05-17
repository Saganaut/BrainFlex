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
import type { BestAnswerOutcome } from "../../../types/bestAnswer";
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

        {result.bestAnswer && (
          <BestAnswerReveal
            outcome={result.bestAnswer}
            element={result.element}
          />
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

/**
 * Best Answer REVEAL block. Ranks submissions by vote count, crowns the
 * winner(s) (multiple on a tie — all listed), and renders each de-anonymized
 * submission so the room can see who said what.
 */
interface BestAnswerRevealProps {
  outcome: BestAnswerOutcome;
  element: DeckElement;
}

const BestAnswerReveal = ({ outcome, element }: BestAnswerRevealProps) => {
  const ranked = [...outcome.tallies].sort((a, b) => b.voteCount - a.voteCount);
  const winnerSet = new Set(outcome.winnerUserIds);

  return (
    <div className={styles.bestAnswer}>
      <div className={styles.bestAnswerHeader}>
        <span className={styles.bestAnswerLabel}>Best Answer</span>
        {outcome.winnerUserIds.length > 0 ? (
          <span className={styles.bestAnswerBonus}>
            +{outcome.bonusAwarded} bonus
          </span>
        ) : (
          <span className={styles.bestAnswerNoVotes}>No votes cast</span>
        )}
      </div>
      <ol className={styles.tallyList}>
        {ranked.map((t) => {
          const isWinner = winnerSet.has(t.userId);
          return (
            <li
              key={t.submissionId}
              className={`${styles.tallyRow} ${isWinner ? styles.tallyWinner : ""}`}>
              <span className={styles.tallyAuthor}>
                {isWinner && <span className={styles.crown}>★ </span>}
                {t.userName}
              </span>
              <span className={styles.tallyText}>
                {humanReadableAnswer(element, t.payload) ?? "(no submission)"}
              </span>
              <span className={styles.tallyCount}>
                {t.voteCount} {t.voteCount === 1 ? "vote" : "votes"}
              </span>
            </li>
          );
        })}
      </ol>
    </div>
  );
};

/** Resolves the canonical correct answer for display per element kind. */
const correctAnswerText = (element: DeckElement): string | null => {
  switch (element.kind) {
    case "McqQuestion": {
      // Multiple correct answers possible — join their option texts.
      const correctIds = new Set(element.correctOptionIds ?? []);
      const correctTexts = (element.options ?? [])
        .filter((o) => o.id && correctIds.has(o.id))
        .map((o) => o.text ?? "")
        .filter(Boolean);
      return correctTexts.length > 0 ? correctTexts.join(", ") : null;
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
    case "McqAnswer": {
      const oid = payload.optionIds?.[0];
      if (element.kind === "McqQuestion") {
        const opt = element.options?.find((o) => o.id === oid);
        return opt?.text ?? null;
      }
      return oid ?? null;
    }
    default:
      return null;
  }
};

export { RoundResult };
