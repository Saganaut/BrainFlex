// Small status pill. Variant and size flow through data-* attributes shared
// with the rest of the design system (see frontend/STYLES.md).
import styles from "./Common.module.css";
import type { BtnVariant, BtnSize } from "./Buttons/BtnTypes";

interface BadgeProps {
  label: string;
  variant?: BtnVariant;
  size?: BtnSize;
}

const Badge = ({ label, size = "md", variant = "info" }: BadgeProps) => {
  return (
    <span
      className={styles.badge}
      data-variant={variant}
      data-size={size}>
      {label}
    </span>
  );
};

export { Badge };
