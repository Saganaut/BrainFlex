/**
 * Renders the 2x2 grid of answer buttons for a multiple-choice question.
 * Highlights correct/incorrect choices after the round result arrives;
 * disabled once the local player has answered or time has expired.
 */
import styles from "./AnswerOptions.module.css";

type Props = {
  options: string[];
  selectedOption: number | null;
  correctOption?: number;
  onSelect: (index: number) => void;
  disabled: boolean;
};

const LABELS = ["A", "B", "C", "D"];

export function AnswerOptions({
  options,
  selectedOption,
  correctOption,
  onSelect,
  disabled,
}: Props) {
  return (
    <div className={styles.grid}>
      {options.map((text, i) => {
        const isSelected = selectedOption === i;
        const isCorrect = correctOption !== undefined && i === correctOption;
        const isWrong = correctOption !== undefined && isSelected && !isCorrect;

        return (
          <button
            key={i}
            type="button"
            disabled={disabled}
            onClick={() => onSelect(i)}
            className={[
              styles.option,
              isSelected ? styles.selected : "",
              isCorrect ? styles.correct : "",
              isWrong ? styles.wrong : "",
            ]
              .filter(Boolean)
              .join(" ")}
          >
            <span className={styles.label}>{LABELS[i]}</span>
            <span className={styles.text}>{text}</span>
          </button>
        );
      })}
    </div>
  );
}
