import { type ReactNode } from "react";
import styles from "./Cards.module.css";

type CardSize = "sm" | "md" | "lg";

interface CardProps {
  header: ReactNode;
  body: ReactNode;
  footer?: ReactNode;
  variant?: string;
  size?: CardSize;
  orientation?: "portrait" | "landscape";
  onClick?: () => void;
}

const Card = ({
  header,
  body,
  footer,
  variant = "primary",
  size = "md",
  orientation = "portrait",
  onClick,
}: CardProps) => {
  return (
    <div
      onClick={onClick}
      className={`${[styles.card, styles[orientation], styles[size], styles[variant]].join(" ")}`}>
      <div className={styles.header}>{header}</div>
      <div className={styles.body}>{body}</div>

      <div className={styles.footer}>{footer}</div>
    </div>
  );
};

export { Card };
