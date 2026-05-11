/**
 * Stub renderer for element kinds whose dedicated UI isn't built yet
 * (RANKING, SCALES, Q&A, GRID, PLACE_ON_IMAGE).
 *
 * Shows the question's prompt and a "Skip" button that posts a TimeoutAnswer
 * so the showcase round can still complete cleanly. Replace per-kind as the
 * dedicated renderers land.
 */
import { Btn } from "@/components/Common/Buttons/Btn";
import type { DeckElement } from "../../../types/elements";
import styles from "./PlaceholderAnswer.module.css";

export interface PlaceholderAnswerProps {
  element: DeckElement;
  disabled: boolean;
  onSkip: () => void;
}

const PRETTY_KIND: Record<string, string> = {
  RankingQuestion: "Ranking",
  ScalesQuestion: "Scales",
  QAndAQuestion: "Q&A",
  GridQuestion: "Grid",
  PlaceOnImageQuestion: "Place on image",
};

const PlaceholderAnswer = ({
  element,
  disabled,
  onSkip,
}: PlaceholderAnswerProps) => (
  <div className={styles.wrapper}>
    <p className={styles.label}>
      {PRETTY_KIND[element.kind] ?? element.kind} — interactive renderer coming soon
    </p>
    <p className={styles.hint}>
      The deck is exercising every element type to verify the backend round-trip.
      Tap Skip to advance.
    </p>
    <Btn type='button' onClick={onSkip} disabled={disabled}>
      {disabled ? "Submitted" : "Skip"}
    </Btn>
  </div>
);

export { PlaceholderAnswer };
