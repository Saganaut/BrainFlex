// Common checkbox input component used in forms throughout the app
import React, { useId } from "react";
import styles from "./Input.module.css";

interface CheckboxProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: React.ReactNode;
  labelPosition?: "labelBefore" | "labelAfter";
  errorMessage?: string;
  infoMessage?: string;
}

const Checkbox: React.FC<CheckboxProps> = ({
  id,
  label,
  labelPosition = "labelAfter",
  checked,
  onChange,
  disabled,
  errorMessage,
  infoMessage,
}) => {
  const generatedId = useId();
  const inputId = id ?? generatedId;

  return (
    <div
      className={[
        styles.checkboxContainer,
        labelPosition === "labelBefore" ? styles.labelBefore : "",
      ]
        .filter(Boolean)
        .join(" ")}>
      <input
        type='checkbox'
        id={inputId}
        className={styles.checkboxInput}
        checked={checked}
        onChange={onChange}
        disabled={disabled}
      />
      <label htmlFor={inputId} className={styles.checkboxWrap}>
        <span className={styles.checkboxControl} />
        {label && <span className={styles.checkboxLabelText}>{label}</span>}
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

export { Checkbox };
