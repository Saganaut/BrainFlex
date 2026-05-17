// Text input wrapped with label + helper/error message slot. Variant maps to a
// className on the <input>; `errorMessage` (when set) forces variant="error" so
// the input border tint and helper text tint together. `fullWidth` lets the
// input fill its container instead of the default 300px (used inside tight
// editor cells like MCQ option cards).
import React from "react";
import type { BtnVariant } from "../../Buttons/BtnTypes";
import shared from "../Input.module.css";
import styles from "./Input.module.css";

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  variant?: BtnVariant;
  infoMessage?: string;
  errorMessage?: string;
  label?: string;
  labelPosition?: "labelAbove" | "labelInFront";
  checked?: boolean;
  fullWidth?: boolean;
  ref?: React.RefObject<HTMLInputElement | null>;
}

/** We can optionally display some information below the input field, if an error message is relevant it will temporary replace the info **/
const Input = ({
  value,
  ref,
  onChange,
  onBlur,
  maxLength,
  type,
  id,
  name,
  variant = "default",
  infoMessage,
  label,
  labelPosition = "labelAbove",
  errorMessage,
  placeholder,
  checked = false,
  disabled,
  fullWidth = false,
  min,
  max,
  step,
}: InputProps) => {
  const inputVariant: BtnVariant = errorMessage != null ? "error" : variant;
  return (
    <div
      className={[
        shared.inputContainer,
        shared[labelPosition],
        fullWidth ? shared.fullWidth : "",
      ]
        .filter(Boolean)
        .join(" ")}>
      {label && <label htmlFor={id}>{label}</label>}
      <div
        className={[styles.input, fullWidth ? styles.fullWidth : ""]
          .filter(Boolean)
          .join(" ")}>
        <input
          type={type}
          ref={ref}
          id={id}
          name={name}
          value={value}
          onChange={onChange}
          onBlur={onBlur}
          maxLength={maxLength}
          placeholder={placeholder}
          disabled={disabled}
          min={min}
          max={max}
          step={step}
          className={
            inputVariant !== "default" && inputVariant !== "brand"
              ? styles[inputVariant]
              : undefined
          }
        />

        {(errorMessage != null || infoMessage != null) && (
          <span
            className={[
              shared.inputInfoMessage,
              styles.message,
              errorMessage && shared.errorMessage,
            ]
              .filter(Boolean)
              .join(" ")}>
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
