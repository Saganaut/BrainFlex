// Styled single-line text input, part of the Common/Form design system components.
import type { ChangeEvent } from "react";
import styles from "./Form.module.css";

interface InputProps {
  label: string;
  id: string;
  value: string;
  onChange?: (value: string) => void;
  placeholder?: string;
  disabled?: boolean;
}

const Input = ({ label, id, value, onChange, placeholder, disabled }: InputProps) => {
  const handleChange = onChange
    ? (e: ChangeEvent<HTMLInputElement>) => { onChange(e.target.value); }
    : undefined;

  return (
    <div className={styles.fieldGroup}>
      <label htmlFor={id} className={styles.fieldLabel}>{label}</label>
      <input
        id={id}
        type="text"
        value={value}
        onChange={handleChange}
        placeholder={placeholder}
        disabled={disabled}
        className={styles.textInput}
      />
    </div>
  );
};

export { Input };
