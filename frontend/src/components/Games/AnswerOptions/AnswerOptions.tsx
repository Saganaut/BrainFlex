/**
 * Renders the 2x2 grid of answer buttons for a multiple-choice question.
 * Highlights correct/incorrect choices after the round result arrives;
 * disabled once the local player has answered or time has expired.
 */
import { Btn } from "@/components/Common/Buttons/Btn";
import styles from "./AnswerOptions.module.css";

interface AnswerOptionsProps {
  options: string[];
  selectedOption: number | null;
  correctOption?: number;
  onSelect: (index: number) => void;
  disabled: boolean;
}

const LABELS = ["A", "B", "C", "D"];

//TODO: Instead of using the text.substring as a key each option should have its own ID
const AnswerOptions = ({
  options,
  selectedOption,
  correctOption,
  onSelect,
  disabled,
}: AnswerOptionsProps) => {
  return (
    <div className={styles.grid}>
      {options.map((text, i) => {
        const isSelected = selectedOption === i;
        const isCorrect = correctOption !== undefined && i === correctOption;
        const isWrong = correctOption !== undefined && isSelected && !isCorrect;

        return (
          <Btn
            key={text.substring(0, 20)}
            type='button'
            disabled={disabled}
            onClick={() => {
              onSelect(i);
            }}
            className={[
              styles.option,
              isSelected ? styles.selected : "",
              isCorrect ? styles.correct : "",
              isWrong ? styles.wrong : "",
            ]
              .filter(Boolean)
              .join(" ")}>
            <span className={styles.label}>{LABELS[i]}</span>
            <span className={styles.text}>{text}</span>
          </Btn>
        );
      })}
    </div>
  );
};

export { AnswerOptions };
