// Shared type unions for Btn / IconBtn props.
// Variant and Size mirror the data-variant / data-size modifier sets defined in
// frontend/src/tokens.css — keep these in sync if new modifiers are added.

export type BtnSize = "xs" | "sm" | "md" | "lg";
export type BtnVariant =
  | "default"
  | "error"
  | "success"
  | "warning"
  | "info"
  | "brand";
export type BtnMode = "outline" | "ghost";
export type BtnShape = "default" | "round" | "pill";
