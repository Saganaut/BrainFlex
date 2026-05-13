// Shared type unions for Btn / IconBtn props. Each value here corresponds to a
// className in Buttons.module.css (e.g. variant="error" → styles.error,
// size="lg" → styles.lg). Keep these in sync if new modifier classes are added.

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
