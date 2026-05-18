// Shared type unions for Btn / IconBtn props. Each variant value corresponds
// to a nested className in Buttons.module.css (e.g. variant="error" →
// .btn.error). The variant is the single axis that controls color + fill /
// outline / ghost style. See STYLE-RULES.md "Named button + icon-button
// variants" for the full catalog and the /design-system page for live demos.

export type BtnSize = "xs" | "sm" | "md" | "lg";

export type BtnVariant =
  | "primary"
  | "ghost"
  | "bordered"
  | "filled"
  | "close"
  | "error"
  | "delete"
  | "success"
  | "warning"
  | "info"
  | "brand";

export type BtnShape = "default" | "round" | "pill" | "avatar";
