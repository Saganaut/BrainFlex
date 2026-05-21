/**
 * One thickness cell in the DrawingCanvas thickness radiogroup. Renders a
 * square button with a preview dot whose diameter mirrors the stroke weight
 * (capped at 28px so the toolbar stays compact). The dot's size and color
 * are passed to CSS as `--dot-size` / `--dot-color` so the module owns layout
 * and only the runtime-derived values cross the React/CSS boundary.
 */
import type { CSSProperties } from "react";
import styles from "./DrawingCanvas.module.css";
import { resolvePaletteColor } from "./drawingUtils";

interface ThicknessOptionProps {
  thickness: number;
  color: string;
  selected: boolean;
  disabled: boolean;
  onSelect: () => void;
}

const ThicknessOption = ({
  thickness,
  color,
  selected,
  disabled,
  onSelect,
}: ThicknessOptionProps) => {
  const dotPx = Math.min(28, thickness / 2 + 4);
  const dotVars = {
    "--dot-size": `${String(dotPx)}px`,
    "--dot-color": resolvePaletteColor(color),
  } as CSSProperties;
  return (
    <button
      type='button'
      className={`${styles.thickness} ${selected ? styles.thicknessSelected : ""}`}
      role='radio'
      aria-checked={selected}
      aria-label={`Thickness ${String(thickness)}`}
      disabled={disabled}
      onClick={onSelect}>
      <span className={styles.thicknessDot} style={dotVars} />
    </button>
  );
};

export { ThicknessOption };
