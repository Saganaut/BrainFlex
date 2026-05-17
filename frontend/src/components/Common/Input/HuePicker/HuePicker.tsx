// Reusable oklch hue picker: color swatch that opens a custom popover,
// a rainbow range slider, and a numeric degree input.
// Used wherever a 0–360° hue angle needs to be picked (ThemePicker, ThemeEditor).
import type { CSSProperties } from "react";
import styles from "./HuePicker.module.css";
import { useHuePicker, useColorAreaDrag } from "./useHuePicker";
import { Btn } from "../../Buttons/Btn";

// Named hues aligned with the design system's palette stops.
const quickPickHues = [
  { hue: 0, label: "Red" },
  { hue: 30, label: "Orange" },
  { hue: 60, label: "Amber" },
  { hue: 95, label: "Yellow" },
  { hue: 140, label: "Lime" },
  { hue: 170, label: "Green" },
  { hue: 200, label: "Teal" },
  { hue: 230, label: "Sky" },
  { hue: 260, label: "Blue" },
  { hue: 290, label: "Violet" },
  { hue: 320, label: "Fuchsia" },
  { hue: 350, label: "Rose" },
];

interface ColorPickerPopoverProps {
  value: number;
  onChange: (hue: number) => void;
}

const ColorPickerPopover = ({ value, onChange }: ColorPickerPopoverProps) => {
  const { areaRef, onMouseDown, onMouseMove, stopDrag } = useColorAreaDrag(
    value,
    onChange,
  );

  return (
    <div
      className={styles.colorPickerPopover}
      role='dialog'
      aria-label='Color picker'>
      {/* 2D hue×lightness area — x maps to hue, click/drag to pick */}
      <div
        ref={areaRef}
        className={styles.colorArea}
        onMouseDown={onMouseDown}
        onMouseMove={onMouseMove}
        onMouseUp={stopDrag}
        onMouseLeave={stopDrag}>
        <div
          className={styles.colorAreaMarker}
          style={{ left: `${(value / 360) * 100}%` }}
        />
      </div>
      {/* Quick-pick named hue swatches */}
      <div className={styles.quickPicks}>
        {quickPickHues.map(({ hue, label }) => (
          <Btn
            key={hue}
            type='button'
            className={styles.quickPickSwatch}
            style={{ background: `oklch(65% 0.2 ${hue}deg)` }}
            onClick={() => {
              onChange(hue);
            }}
            aria-label={label}
            title={label}
          />
        ))}
      </div>
    </div>
  );
};

interface HuePickerProps {
  label: string;
  value: number;
  onChange: (hue: number) => void;
}

const HuePicker = ({ label, value, onChange }: HuePickerProps) => {
  const { isOpen, containerRef, toggleOpen, handleNumberInput } =
    useHuePicker(onChange);

  return (
    <div
      ref={containerRef}
      className={styles.huePickerContainer}
      style={{ "--current-hue": `${value}deg` } as CSSProperties}>
      <div className={styles.huePickerHeader}>
        <span className={styles.huePickerLabel}>{label}</span>
        <div className={styles.huePickerValueRow}>
          <input
            type='number'
            min={0}
            max={360}
            value={value}
            onChange={handleNumberInput}
            className={styles.huePickerNumber}
            aria-label={`${label} hue in degrees`}
          />
          <span className={styles.huePickerDegree}>°</span>
        </div>
      </div>
      <div className={styles.huePickerControls}>
        <Btn
          type='button'
          className={styles.huePickerSwatch}
          style={{ background: `oklch(65% 0.2 ${value}deg)` }}
          onClick={toggleOpen}
          aria-label={`Pick ${label.toLowerCase()} color`}
          aria-expanded={isOpen}
          aria-haspopup='dialog'
        />
        <input
          type='range'
          min={0}
          max={360}
          value={value}
          onChange={(e) => {
            onChange(Number(e.target.value));
          }}
          className={styles.huePickerSlider}
          aria-label={`${label} hue angle`}
        />
      </div>
      {isOpen && <ColorPickerPopover value={value} onChange={onChange} />}
    </div>
  );
};

export { HuePicker };
