// Styled radio group, part of the Common/Form design system components.
import styles from "./Form.module.css";

interface RadioOption {
  label: string;
  value: string;
}

interface RadioProps {
  legend: string;
  name: string;
  options: RadioOption[];
  value: string;
  onChange: (value: string) => void;
  disabled?: boolean;
}

const Radio = ({ legend, name, options, value, onChange, disabled }: RadioProps) => {
  return (
    <fieldset className={styles.radioGroup}>
      <legend className={styles.radioLegend}>{legend}</legend>
      {options.map((option) => (
        <label
          key={option.value}
          htmlFor={`${name}-${option.value}`}
          className={styles.radioOption}
        >
          <input
            id={`${name}-${option.value}`}
            type="radio"
            name={name}
            value={option.value}
            checked={value === option.value}
            onChange={() => { onChange(option.value); }}
            disabled={disabled}
            className={styles.radioInput}
          />
          <span className={styles.radioLabel}>{option.label}</span>
        </label>
      ))}
    </fieldset>
  );
};

export { Radio };
