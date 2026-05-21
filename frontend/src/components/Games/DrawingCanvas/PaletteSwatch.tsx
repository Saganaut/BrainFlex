/**
 * One color cell in the DrawingCanvas palette radiogroup. Renders a circular
 * filled button with selection ring; the radio semantics live here so the
 * parent only has to map over its palette array.
 */
import styles from "./DrawingCanvas.module.css";
import { resolvePaletteColor } from "./drawingUtils";

interface PaletteSwatchProps {
  swatch: string;
  selected: boolean;
  disabled: boolean;
  onSelect: () => void;
}

const PaletteSwatch = ({
  swatch,
  selected,
  disabled,
  onSelect,
}: PaletteSwatchProps) => (
  <button
    type='button'
    className={`${styles.swatch} ${selected ? styles.swatchSelected : ""}`}
    style={{ background: resolvePaletteColor(swatch) }}
    role='radio'
    aria-checked={selected}
    aria-label={`Color ${swatch}`}
    disabled={disabled}
    onClick={onSelect}
  />
);

export { PaletteSwatch };
