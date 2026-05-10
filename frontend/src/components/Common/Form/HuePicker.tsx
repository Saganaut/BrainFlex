// Reusable oklch hue picker: color swatch that opens a custom popover,
// a rainbow range slider, and a numeric degree input.
// Used wherever a 0–360° hue angle needs to be picked (ThemePicker, ThemeEditor).
import {
  useRef,
  useState,
  useEffect,
  type CSSProperties,
  type ChangeEvent,
  type MouseEvent as ReactMouseEvent,
} from "react";
import styles from "./Form.module.css";

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
  const areaRef = useRef<HTMLDivElement>(null);
  const isDragging = useRef(false);

  const getHueFromEvent = (e: ReactMouseEvent) => {
    if (!areaRef.current) return value;
    const rect = areaRef.current.getBoundingClientRect();
    return Math.round(
      Math.max(0, Math.min(360, ((e.clientX - rect.left) / rect.width) * 360)),
    );
  };

  return (
    <div
      className={styles.colorPickerPopover}
      role='dialog'
      aria-label='Color picker'>
      {/* 2D hue×lightness area — x maps to hue, click/drag to pick */}
      <div
        ref={areaRef}
        className={styles.colorArea}
        onMouseDown={(e) => {
          isDragging.current = true;
          onChange(getHueFromEvent(e));
        }}
        onMouseMove={(e) => {
          if (isDragging.current) onChange(getHueFromEvent(e));
        }}
        onMouseUp={() => {
          isDragging.current = false;
        }}
        onMouseLeave={() => {
          isDragging.current = false;
        }}>
        <div
          className={styles.colorAreaMarker}
          style={{ left: `${(value / 360) * 100}%` }}
        />
      </div>
      {/* Quick-pick named hue swatches */}
      <div className={styles.quickPicks}>
        {quickPickHues.map(({ hue, label }) => (
          <button
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
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!isOpen) return;
    const handlePointerDown = (e: PointerEvent) => {
      if (!containerRef.current?.contains(e.target as Node)) setIsOpen(false);
    };
    document.addEventListener("pointerdown", handlePointerDown);
    return () => {
      document.removeEventListener("pointerdown", handlePointerDown);
    };
  }, [isOpen]);

  const handleNumberInput = (e: ChangeEvent<HTMLInputElement>) => {
    const n = Number(e.target.value);
    if (Number.isFinite(n)) onChange(Math.max(0, Math.min(360, n)));
  };

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
        <button
          type='button'
          className={styles.huePickerSwatch}
          style={{ background: `oklch(65% 0.2 ${value}deg)` }}
          onClick={() => {
            setIsOpen((o) => !o);
          }}
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
