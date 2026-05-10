// Common radio input component used in form option groups throughout the app
import React from "react";
import styles from "./Form.module.css";

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
  return (
    <div className={[styles.radioContainer, styles[labelPosition]].join(" ")}>
      <div className={styles.radio}>
        <input
          type='radio'
          id={id}
          name={name}
          value={value}
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

export { Radio };
