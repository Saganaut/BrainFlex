// Generic card with header/body/footer slots. Variant and size map to className
// modifiers in Cards.module.css. Becomes clickable when onClick is provided.
import { type ReactNode } from "react";
import type { BtnVariant, BtnSize } from "../Buttons/BtnTypes";
import styles from "./Cards.module.css";

interface CardProps {
  header: ReactNode;
  body: ReactNode;
  footer?: ReactNode;
  variant?: BtnVariant;
  size?: BtnSize;
  onClick?: () => void;
}

const Card = ({
  header,
  body,
  footer,
  variant = "default",
  size = "md",
  onClick,
}: CardProps) => {
  return (
    <div
      onClick={onClick}
      className={[
        styles.card,
        variant !== "default" && styles[variant],
        styles[size],
        onClick != null && styles.isClickable,
      ]
        .filter(Boolean)
        .join(" ")}>
      <div className={styles.header}>{header}</div>
      <div className={styles.body}>{body}</div>
      <div className={styles.footer}>{footer}</div>
    </div>
  );
};

export { Card };
