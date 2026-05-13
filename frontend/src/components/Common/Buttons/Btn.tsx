// Primary button. Variant / size / mode flow through className composition,
// not data-* attributes — each modifier class lives in Buttons.module.css and
// overrides a subset of the component's CSS vars (--padding, --color, etc.).
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
        variant !== "default" && styles[variant],
        styles[size],
        mode && styles[mode],
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
