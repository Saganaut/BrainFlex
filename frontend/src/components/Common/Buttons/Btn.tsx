// Primary button. The `variant` prop is the single axis controlling color +
// fill / outline / ghost — see BtnTypes.ts and STYLE-RULES.md "Named button
// + icon-button variants". Each variant maps to a nested rule under .btn in
// Buttons.module.css.
import type { ReactNode } from "react";
import type { BtnVariant, BtnSize, BtnShape } from "./BtnTypes";
import styles from "./Buttons.module.css";

interface BtnProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: BtnVariant;
  size?: BtnSize;
  shape?: BtnShape;
  icon?: ReactNode;
  iconPosition?: "left" | "right";
  isLoading?: boolean;
  children?: ReactNode;
}

const Btn = ({
  variant = "primary",
  size = "md",
  shape = "default",
  disabled = false,
  type = "button",
  icon,
  iconPosition = "left",
  isLoading,
  className,
  children,
  ...rest
}: BtnProps) => {
  return (
    <button
      type={type}
      disabled={disabled || isLoading}
      data-icon-position={icon ? iconPosition : undefined}
      {...rest}
      className={[
        styles.btn,
        styles[variant],
        styles[size],
        shape !== "default" && styles[shape],
        className,
      ]
        .filter(Boolean)
        .join(" ")}>
      {icon != null && <span>{icon}</span>}
      {children}
    </button>
  );
};

export { Btn };
