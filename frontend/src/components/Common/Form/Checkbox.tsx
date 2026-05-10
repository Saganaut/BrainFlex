// Styled checkbox with custom appearance, part of the Common/Form design system components.
import styles from "./Form.module.css";

interface CheckboxProps {
  label: string;
  id: string;
  checked: boolean;
  onChange?: (checked: boolean) => void;
  disabled?: boolean;
}

const Checkbox = ({ label, id, checked, onChange, disabled }: CheckboxProps) => {
  return (
    <label htmlFor={id} className={styles.checkboxWrapper}>
      <input
        id={id}
        type="checkbox"
        checked={checked}
        onChange={onChange ? (e) => { onChange(e.target.checked); } : undefined}
        disabled={disabled}
        className={styles.checkboxInput}
      />
      <span className={styles.checkboxLabel}>{label}</span>
    </label>
  );
};

export { Checkbox };
