// Text input wrapped with label + helper/error message slot.
// `errorMessage` (when set) forces data-variant="error" on the <input> so border
// styling and helper text tint together. See frontend/STYLES.md.
import React from "react";
import type { BtnVariant } from "../Buttons/BtnTypes";
import styles from "./Input.module.css";
interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  variant?: BtnVariant;
  infoMessage?: string;
  errorMessage?: string;
  label?: string;
  labelPosition?: "labelAbove" | "labelInFront";
  checked?: boolean;
  ref?: React.RefObject<HTMLInputElement | null>;
}

/** We can optionally display some information below the input field, if an error message is relevant it will temporary replace the info **/
const Input: React.FC<InputProps> = ({
  value,
  ref,
  onChange,
  maxLength,
  id,
  variant = "default",
  infoMessage,
  label,
  labelPosition = "labelAbove",
  errorMessage,
  placeholder,
  checked = false,
}) => {
  const inputVariant: BtnVariant = errorMessage != null ? "error" : variant;
  return (
    <div className={[styles.inputContainer, styles[labelPosition]].join(" ")}>
      {label && <label htmlFor={id}>{label}</label>}
      <div className={styles.input}>
        <input
          ref={ref}
          id={id}
          value={value}
          onChange={onChange}
          maxLength={maxLength}
          placeholder={placeholder}
          data-variant={inputVariant}
        />

        {(errorMessage != null || infoMessage != null) && (
          <span
            className={[
              styles.inputInfoMessage,
              errorMessage && styles.errorMessage,
            ].join(" ")}>
            {errorMessage ?? infoMessage}
          </span>
        )}

        {checked && (
          <span className={styles.checked}>
            <svg
              xmlns='http://www.w3.org/2000/svg'
              fill='none'
              viewBox='0 0 24 24'
              strokeWidth={1.5}>
              <path
                strokeLinecap='round'
                strokeLinejoin='round'
                d='m4.5 12.75 6 6 9-13.5'
              />
            </svg>
          </span>
        )}
      </div>
    </div>
  );
};

export { Input };
