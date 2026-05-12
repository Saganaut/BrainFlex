// Primary button component. Variants and sizes are wired through data-* attributes
// (data-variant / data-size / data-mode) so they compose with any element that
// follows the canonical CSS-var manifest. See frontend/STYLES.md for the convention.
import type { ReactNode } from "react";
import type { BtnVariant, BtnSize, BtnShape, BtnMode } from "./BtnTypes";
import styles from "./Buttons.module.css";

interface BtnProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: BtnVariant;
  size?: BtnSize;
  mode?: BtnMode;
  shape?: BtnShape;
  icon?: ReactNode;
  iconPosition?: "left" | "right";
  isLoading?: boolean;
  children?: ReactNode;
}

const Btn = ({
  variant = "default",
  size = "md",
  mode,
  shape = "default",
  disabled = false,
  type = "button",
  icon,
  iconPosition = "left",
  onClick,
  children,
}: BtnProps) => {
  return (
    <button
      type={type}
      disabled={disabled}
      onClick={onClick}
      data-variant={variant}
      data-size={size}
      data-mode={mode}
      data-icon-position={icon ? iconPosition : undefined}
      className={[styles.btn, shape !== "default" && styles[shape]]
        .filter(Boolean)
        .join(" ")}>
      {icon != null && <span>{icon}</span>}
      {children}
    </button>
  );
};

export { Btn };
