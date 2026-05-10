// Styled multi-line text input, part of the Common/Form design system components.
import type { ChangeEvent } from "react";
import styles from "./Form.module.css";

interface TextAreaProps {
  label: string;
  id: string;
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  rows?: number;
  disabled?: boolean;
}

const TextArea = ({ label, id, value, onChange, placeholder, rows = 4, disabled }: TextAreaProps) => {
  const handleChange = (e: ChangeEvent<HTMLTextAreaElement>) => { onChange(e.target.value); };

  return (
    <div className={styles.fieldGroup}>
      <label htmlFor={id} className={styles.fieldLabel}>{label}</label>
      <textarea
        id={id}
        value={value}
        onChange={handleChange}
        placeholder={placeholder}
        rows={rows}
        disabled={disabled}
        className={styles.textAreaInput}
      />
    </div>
  );
};

export { TextArea };
