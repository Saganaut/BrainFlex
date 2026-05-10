// Common textarea component matching Input structure for multi-line text entry
import React from "react";
import styles from "./Form.module.css";

interface TextAreaProps
  extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
  variant?: "default";
  infoMessage?: string;
  errorMessage?: string;
  label?: string;
  labelPosition?: "labelAbove" | "labelInFront";
}

const TextArea: React.FC<TextAreaProps> = ({
  value,
  onChange,
  maxLength,
  id,
  rows = 4,
  variant = "default",
  infoMessage,
  label,
  labelPosition = "labelAbove",
  errorMessage,
}) => {
  console.log(variant);
  return (
    <div className={[styles.inputContainer, styles[labelPosition]].join(" ")}>
      {label && <label htmlFor={id}>{label}</label>}
      <div className={styles.textarea}>
        <textarea
          id={id}
          value={value}
          onChange={onChange}
          maxLength={maxLength}
          rows={rows}
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
    </div>
  );
};

export { TextArea };
