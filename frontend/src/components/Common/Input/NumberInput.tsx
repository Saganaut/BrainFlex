// Numeric input: same chrome as Input, but the value/onChange API is typed as
// `number` so callers don't repeat the parse-fallback dance. min / max / step
// flow through to the native control. Mirrors the labelled-container layout
// of Input so the two read identically in a form.
import React from "react";
import styles from "./Input.module.css";

interface NumberInputProps
  extends Omit<
    React.InputHTMLAttributes<HTMLInputElement>,
    "value" | "onChange" | "type"
  > {
  value: number;
  onChange: (value: number) => void;
  label?: string;
  labelPosition?: "labelAbove" | "labelInFront";
  infoMessage?: string;
  errorMessage?: string;
  fullWidth?: boolean;
}

const NumberInput: React.FC<NumberInputProps> = ({
  value,
  onChange,
  onBlur,
  id,
  name,
  label,
  labelPosition = "labelAbove",
  infoMessage,
  errorMessage,
  fullWidth = false,
  disabled,
  min,
  max,
  step,
  placeholder,
}) => {
  return (
    <div
      className={[
        styles.inputContainer,
        styles[labelPosition],
        fullWidth ? styles.fullWidth : "",
      ]
        .filter(Boolean)
        .join(" ")}>
      {label && <label htmlFor={id}>{label}</label>}
      <div className={styles.input}>
        <input
          type='number'
          id={id}
          name={name}
          value={Number.isFinite(value) ? value : 0}
          onChange={(e) => {
            const next = Number(e.target.value);
            onChange(Number.isFinite(next) ? next : 0);
          }}
          onBlur={onBlur}
          disabled={disabled}
          min={min}
          max={max}
          step={step}
          placeholder={placeholder}
          className={errorMessage != null ? styles.error : undefined}
        />
        {(errorMessage != null || infoMessage != null) && (
          <span
            className={[
              styles.inputInfoMessage,
              errorMessage ? styles.errorMessage : "",
            ]
              .filter(Boolean)
              .join(" ")}>
            {errorMessage ?? infoMessage}
          </span>
        )}
      </div>
    </div>
  );
};

export { NumberInput };
