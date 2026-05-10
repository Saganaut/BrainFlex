// Common radio input component used in form option groups throughout the app
import React, { useId } from "react";
import styles from "./Input.module.css";

interface RadioProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  labelPosition?: "labelBefore" | "labelAfter";
  errorMessage?: string;
  infoMessage?: string;
}

const Radio: React.FC<RadioProps> = ({
  id,
  label,
  labelPosition = "labelAfter",
  checked,
  onChange,
  name,
  value,
  disabled,
  errorMessage,
  infoMessage,
}) => {
  const generatedId = useId();
  const inputId = id ?? generatedId;

  return (
    <div
      className={[
        styles.radioContainer,
        labelPosition === "labelBefore" ? styles.labelBefore : "",
      ]
        .filter(Boolean)
        .join(" ")}>
      <input
        type='radio'
        id={inputId}
        className={styles.radioInput}
        name={name}
        value={value}
        checked={checked}
        onChange={onChange}
        disabled={disabled}
      />
      <label htmlFor={inputId} className={styles.radioWrap}>
        <span className={styles.radioControl} />
        {label && <span className={styles.radioLabelText}>{label}</span>}
      </label>
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
  );
};

export { Radio };
