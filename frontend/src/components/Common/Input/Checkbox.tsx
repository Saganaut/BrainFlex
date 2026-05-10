// Common checkbox input component used in forms throughout the app
import React from "react";
import styles from "./Form.module.css";

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
  return (
    <div
      className={[
        styles.checkboxContainer,
        styles[labelPosition],
      ].join(" ")}>
      <div className={styles.checkbox}>
        <input
          type="checkbox"
          id={id}
          checked={checked}
          onChange={onChange}
          disabled={disabled}
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
      </div>
      {label && <label htmlFor={id}>{label}</label>}
    </div>
  );
};

export { Checkbox };
