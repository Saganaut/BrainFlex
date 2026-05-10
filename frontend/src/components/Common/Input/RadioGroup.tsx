// RadioGroup — wraps Radio items into a named fieldset where exactly one option must be selected
import React from "react";
import { Radio } from "./Radio";
import styles from "./Input.module.css";

interface RadioGroupOption {
  value: string;
  label: string;
}

interface RadioGroupProps {
  name: string;
  legend?: string;
  options: RadioGroupOption[];
  value: string;
  onChange: (value: string) => void;
  disabled?: boolean;
  errorMessage?: string;
  infoMessage?: string;
}

const RadioGroup: React.FC<RadioGroupProps> = ({
  name,
  legend,
  options,
  value,
  onChange,
  disabled,
  errorMessage,
  infoMessage,
}) => {
  return (
    <fieldset className={styles.radioGroupContainer}>
      {legend && <legend className={styles.radioGroupLegend}>{legend}</legend>}
      <div className={styles.radioGroupOptions}>
        {options.map((option) => (
          <Radio
            key={option.value}
            id={`${name}-${option.value}`}
            name={name}
            value={option.value}
            label={option.label}
            checked={value === option.value}
            onChange={() => { onChange(option.value); }}
            disabled={disabled}
          />
        ))}
      </div>
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
    </fieldset>
  );
};

export { RadioGroup };
