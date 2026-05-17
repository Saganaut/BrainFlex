/**
 * Numeric input for NumberQuestion rounds. Accepts an integer or decimal value
 * (decimalPlaces hint from the element). After submission the input locks; on
 * reveal the correct value is shown alongside the player's guess.
 */
import { useState } from "react";
import { Btn } from "@/components/Common/Buttons/Btn";
import { Input } from "@/components/Common/Input/Input/Input";
import type { NumberQuestion } from "../../../types/elements";
import styles from "./NumberAnswerInput.module.css";

export interface NumberAnswerInputProps {
  element: NumberQuestion;
  submittedValue: number | null;
  revealedCorrectValue?: number;
  onSubmit: (value: number) => void;
  disabled: boolean;
}

const NumberAnswerInput = ({
  element,
  submittedValue,
  revealedCorrectValue,
  onSubmit,
  disabled,
}: NumberAnswerInputProps) => {
  const [draft, setDraft] = useState("");

  const handleSubmit = (e: React.SubmitEvent) => {
    e.preventDefault();
    const parsed = Number(draft);
    if (Number.isNaN(parsed)) return;
    onSubmit(parsed);
  };

  const locked = submittedValue !== null || disabled;
  const revealed = revealedCorrectValue !== undefined;
  const unit = element.unitLabel ?? "";

  return (
    <form className={styles.wrapper} onSubmit={handleSubmit}>
      <label htmlFor='number-input' className={styles.label}>
        Your guess
      </label>
      <div className={styles.row}>
        <Input
          id='number-input'
          type='number'
          inputMode='decimal'
          step={
            (element.decimalPlaces ?? 0) > 0
              ? Math.pow(10, -(element.decimalPlaces ?? 0)).toString()
              : "1"
          }
          className={styles.field}
          value={submittedValue !== null ? String(submittedValue) : draft}
          onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
            setDraft(e.target.value);
          }}
          placeholder='123'
          autoComplete='off'
          autoFocus
          disabled={locked}
        />
        {unit && <span className={styles.unit}>{unit}</span>}
        <Btn type='submit' disabled={locked || draft.trim().length === 0}>
          Submit
        </Btn>
      </div>

      {submittedValue !== null && !revealed && (
        <p className={styles.waiting}>
          Locked in <strong>{submittedValue}</strong>
          {unit}. Waiting for the round to end…
        </p>
      )}

      {revealed && (
        <div className={styles.reveal}>
          Correct value:{" "}
          <strong>
            {revealedCorrectValue}
            {unit}
          </strong>
        </div>
      )}
    </form>
  );
};

export { NumberAnswerInput };
