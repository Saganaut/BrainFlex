// Text input fused with an action button, used for search or submit-inline patterns
import React from "react";
import type { ReactNode } from "react";
import styles from "./Input.module.css";
import { Btn } from "../Buttons/Btn";

interface InputWithButtonProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  labelPosition?: "labelAbove" | "labelInFront";
  buttonLabel?: ReactNode;
  onButtonClick?: () => void;
  errorMessage?: string;
  infoMessage?: string;
}

const InputWithButton: React.FC<InputWithButtonProps> = ({
  id,
  value,
  onChange,
  maxLength,
  placeholder,
  disabled,
  label,
  labelPosition = "labelAbove",
  buttonLabel = "Submit",
  onButtonClick,
  errorMessage,
  infoMessage,
}) => {
  return (
    <div className={[styles.inputContainer, styles[labelPosition]].join(" ")}>
      {label && <label htmlFor={id}>{label}</label>}
      <div className={styles.inputWithButton}>
        <input
          id={id}
          value={value}
          onChange={onChange}
          maxLength={maxLength}
          placeholder={placeholder}
          disabled={disabled}
        />
        <Btn type='button' onClick={onButtonClick} disabled={disabled}>
          {buttonLabel}
        </Btn>
        {(errorMessage != null || infoMessage != null) && (
          <span
            className={[
              styles.inputInfoMessage,
              errorMessage && styles.errorMessage,
            ].join(" ")}>
            {errorMessage ?? infoMessage}
          </span>
        )}
      </div>
    </div>
  );
};

export { InputWithButton };
