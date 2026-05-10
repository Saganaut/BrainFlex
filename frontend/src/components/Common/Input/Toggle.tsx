// Toggle switch built on a visually-hidden checkbox; CSS :has() drives all visual state
import React, { useId } from "react";
import styles from "./Input.module.css";

interface ToggleProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  labelPosition?: "labelBefore" | "labelAfter";
  errorMessage?: string;
  infoMessage?: string;
}

const Toggle: React.FC<ToggleProps> = ({
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
        styles.toggleContainer,
        labelPosition === "labelBefore" ? styles.labelBefore : "",
      ]
        .filter(Boolean)
        .join(" ")}>
      <input
        type='checkbox'
        id={inputId}
        className={styles.toggleInput}
        checked={checked}
        onChange={onChange}
        disabled={disabled}
      />
      <label htmlFor={inputId} className={styles.toggleWrap}>
        <span className={styles.toggleTrack}>
          <span className={styles.toggleThumb} />
        </span>
        {label && <span className={styles.toggleLabelText}>{label}</span>}
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

export { Toggle };
