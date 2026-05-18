// Icon-only button. The `variant` prop is the single axis controlling color
// + fill / outline / ghost — see BtnTypes.ts and STYLE-RULES.md "Named
// button + icon-button variants". Each variant maps to a nested rule under
// .iconBtn in Buttons.module.css. variant="close" renders an XMarkIcon and
// ignores the `icon` prop. shape="avatar" gives the round photo treatment
// (zero padding, thicker border, image clipping).
import React, { type ReactNode } from "react";
import { XMarkIcon } from "@heroicons/react/24/outline";
import type { BtnShape, BtnSize, BtnVariant } from "./BtnTypes";
import styles from "./Buttons.module.css";

interface IconBtnProps extends Omit<
  React.ButtonHTMLAttributes<HTMLButtonElement>,
  "type"
> {
  variant?: BtnVariant;
  icon?: ReactNode;
  size?: BtnSize;
  shape?: BtnShape;
}

const IconBtn = ({
  variant = "primary",
  icon,
  size = "md",
  shape = "default",
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
        styles[variant],
        styles[size],
        shape !== "default" && styles[shape],
        className,
      ]
        .filter(Boolean)
        .join(" ")}>
      {variant === "close" ? <XMarkIcon /> : icon}
    </button>
  );
};

export { IconBtn };
