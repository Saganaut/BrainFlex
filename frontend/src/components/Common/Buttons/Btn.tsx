import type { ReactNode } from "react";
import type { BtnVariant, BtnSize, BtnShape } from "./BtnTypes";
import styles from "./Buttons.module.css";

interface BtnProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: BtnVariant;
  size?: BtnSize;
  icon?: ReactNode;
  iconPosition?: "left" | "right";
  isLoading?: boolean;
  children?: ReactNode;
  shape?: BtnShape;
}
const Btn = ({
  variant = "primary",
  size = "md",
  disabled = false,
  iconPosition = "left",
  type = "button",
  shape = "default",
  onClick,
  icon,
  children,
}: BtnProps) => {
  const isDisabled = disabled ? "isDisabled" : "";
  const withIcon = icon ? "withIcon" : "";
  return (
    <button
      type={type}
      disabled={disabled}
      onClick={onClick}
      className={`${[
        styles.btn,
        styles[iconPosition],
        styles[shape],
        styles[isDisabled],
        styles[size],
        styles[variant],
        styles[type],
        styles[withIcon],
      ].join(" ")}`}>
      {icon != null && <span>{icon}</span>}
      {children}
    </button>
  );
};

export { Btn };
