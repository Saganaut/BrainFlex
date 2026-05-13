// Icon-only button. Variant + size flow through className composition (see
// Buttons.module.css for the modifier classes). Component-specific concepts —
// close / avatar / round / pill / bordered / withBackground — stay as module
// classes too. "close" renders an XMarkIcon; "avatar" and "default" render the
// passed icon.
import React, { type ReactNode } from "react";
import { XMarkIcon } from "@heroicons/react/24/outline";
import type { BtnShape, BtnSize, BtnVariant } from "./BtnTypes";
import styles from "./Buttons.module.css";

interface IconBtnProps
  extends Omit<React.ButtonHTMLAttributes<HTMLButtonElement>, "type"> {
  type: "close" | "default" | "avatar";
  icon?: ReactNode;
  size?: BtnSize;
  variant?: BtnVariant;
  shape?: BtnShape;
  bordered?: boolean;
  backgroundColor?: boolean;
}

const IconBtn = ({
  type,
  icon,
  size = "md",
  variant = "default",
  shape = "default",
  bordered = false,
  backgroundColor = false,
  disabled = false,
  onClick,
  className,
  ...rest
}: IconBtnProps) => {
  return (
    <button
      type='button'
      disabled={disabled}
      onClick={onClick}
      {...rest}
      className={[
        styles.iconBtn,
        variant !== "default" && styles[variant],
        styles[size],
        type !== "default" && styles[type],
        shape !== "default" && styles[shape],
        bordered && styles.bordered,
        backgroundColor && styles.withBackground,
        className,
      ]
        .filter(Boolean)
        .join(" ")}>
      {type === "close" ? <XMarkIcon /> : icon}
    </button>
  );
};

export { IconBtn };
