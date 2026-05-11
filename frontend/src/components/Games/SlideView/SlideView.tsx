/**
 * Renders a non-interactive deck element (slide) during a showcase round.
 * Used for title cards, section dividers, intermissions, callouts, etc. — analogous
 * to a PowerPoint slide between questions.
 *
 * The server auto-advances slides on their display timer (always, even when noTimer
 * is on for questions), so there's nothing for the player to do here. We still show
 * a countdown so participants know how long the slide will stay up.
 */
import styles from "./SlideView.module.css";
import type { QuestionData } from "../../../store/gameSlice";

export interface SlideViewProps {
  slide: QuestionData;
  round: number;
  totalRounds: number;
  // Seconds left on the display timer (-1 hides the countdown badge).
  timeRemaining?: number;
}

const SlideView = ({ slide, round, totalRounds, timeRemaining }: SlideViewProps) => {
  return (
    <article className={styles.card} aria-label='Slide'>
      <div className={styles.meta}>
        <span className={styles.position}>
          Slide {round + 1} / {totalRounds}
        </span>
        {typeof timeRemaining === "number" && timeRemaining >= 0 && (
          <span className={styles.timer}>
            advancing in {timeRemaining}s
          </span>
        )}
      </div>

      {slide.imageUrl && (
        <img src={slide.imageUrl} alt='' className={styles.image} />
      )}

      {slide.title && <h2 className={styles.title}>{slide.title}</h2>}
      <p className={styles.body}>{slide.questionText}</p>
    </article>
  );
};

export { SlideView };
