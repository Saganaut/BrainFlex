// Icon-only button: "close" renders an XMarkIcon, "avatar"/"default" renders the passed icon
import React, { type ReactNode } from "react";
import { XMarkIcon } from "@heroicons/react/24/outline";
import type { BtnShape, BtnSize, BtnVariant } from "./BtnTypes";
import styles from "./Buttons.module.css";

interface IconBtnProps extends Omit<
  React.ButtonHTMLAttributes<HTMLButtonElement>,
  "type"
> {
  type: "close" | "default" | "avatar";
  icon?: ReactNode;
  size?: BtnSize;
  variant?: BtnVariant;
  shape?: BtnShape;
  bordered?: boolean;
  backgroundColor?: boolean;
}

const IconBtn: React.FC<IconBtnProps> = ({
  type,
  icon,
  size = "md",
  disabled = false,
  onClick,
  bordered = false,
  shape = "default",
  variant = "primary",
  backgroundColor = false,
  className,
  ...rest
}) => {
  const isDisabled = disabled ? "isDisabled" : "";
  return (
    <button
      type='button'
      disabled={disabled}
      onClick={onClick}
      {...rest}
      className={[
        styles.iconBtn,
        styles[type],
        styles[size],
        bordered && styles.bordered,
        backgroundColor && styles.withBackground,
        styles[shape],
        styles[variant],
        styles[isDisabled],
        className,
      ].join(" ")}>
      {type === "close" ? <XMarkIcon /> : icon}
    </button>
  );
};

export { IconBtn };
