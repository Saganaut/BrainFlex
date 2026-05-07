import React from "react";
import styles from "./Form.module.css";
interface RadioProps {
  label: string;
}

const Radio: React.FC<RadioProps> = ({ label }) => {
  return <div className={styles.radioContainer}> {label}</div>;
};

export { Radio };
