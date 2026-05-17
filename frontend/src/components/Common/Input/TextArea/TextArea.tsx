// Common textarea component matching Input structure for multi-line text entry
import React from "react";
import shared from "../Input.module.css";
import styles from "./TextArea.module.css";

interface TextAreaProps
  extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
  variant?: "default";
  infoMessage?: string;
  errorMessage?: string;
  label?: string;
  labelPosition?: "labelAbove" | "labelInFront";
}

const TextArea = ({
  value,
  onChange,
  maxLength,
  id,
  rows = 4,
  infoMessage,
  label,
  labelPosition = "labelAbove",
  errorMessage,
}: TextAreaProps) => {
  return (
    <div
      className={[shared.inputContainer, shared[labelPosition]].join(" ")}>
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
              shared.inputInfoMessage,
              styles.message,
              errorMessage && shared.errorMessage,
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

export { TextArea };
