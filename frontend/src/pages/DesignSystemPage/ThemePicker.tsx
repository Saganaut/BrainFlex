// Color scheme customizer for design system exploration.
// Edits --hue-primary and --hue-accent on :root, so changes ripple
// instantly through every semantic token on the page.
import type { ChangeEvent } from "react";
import { useTheme } from "../../hooks/useTheme";
import { Btn } from "../../components/Common/Buttons/Btn";
import { themePresets } from "./data";
import styles from "./DesignSystem.module.css";

// Converts a hex color string to its oklch hue angle (0–360°).
// Uses the Oklab color space matrices for perceptual accuracy.
function hexToOklchHue(hex: string): number {
  const r = parseInt(hex.slice(1, 3), 16) / 255;
  const g = parseInt(hex.slice(3, 5), 16) / 255;
  const b = parseInt(hex.slice(5, 7), 16) / 255;

  const toLinear = (v: number) =>
    v <= 0.04045 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4;

  const rl = toLinear(r);
  const gl = toLinear(g);
  const bl = toLinear(b);

  const lCube = Math.cbrt(
    0.4122214708 * rl + 0.5363325363 * gl + 0.0514459929 * bl,
  );
  const mCube = Math.cbrt(
    0.2119034982 * rl + 0.6806995451 * gl + 0.1073969566 * bl,
  );
  const sCube = Math.cbrt(
    0.0883024619 * rl + 0.2817188376 * gl + 0.6299787005 * bl,
  );

  const a = 1.977998495 * lCube - 2.428592205 * mCube + 0.450593710 * sCube;
  const bOk = 0.025904037 * lCube + 0.782771766 * mCube - 0.808675766 * sCube;

  const hue = (Math.atan2(bOk, a) * 180) / Math.PI;
  return hue < 0 ? hue + 360 : hue;
}

interface HueSliderProps {
  label: string;
  hue: number;
  onChange: (hue: number) => void;
}

function HueSlider({ label, hue, onChange }: HueSliderProps) {
  const handleColorPicker = (e: ChangeEvent<HTMLInputElement>) => {
    onChange(Math.round(hexToOklchHue(e.target.value)));
  };

  const handleNumberInput = (e: ChangeEvent<HTMLInputElement>) => {
    const n = Number(e.target.value);
    if (Number.isFinite(n)) onChange(n);
  };

  return (
    <div className={styles.hueRow}>
      <span className={styles.hueLabel}>{label}</span>
      {/* Swatch doubles as the color picker trigger */}
      <label
        className={styles.hueSwatch}
        style={{ background: `oklch(65% 0.2 ${hue}deg)` }}
        title={`Click to pick ${label.toLowerCase()} color`}
        aria-label={`Pick ${label.toLowerCase()} color`}
      >
        <input
          type="color"
          className={styles.colorPickerInput}
          onChange={handleColorPicker}
        />
      </label>
      <input
        type="range"
        min={0}
        max={360}
        value={hue}
        onChange={(e) => { onChange(Number(e.target.value)); }}
        className={styles.hueSlider}
        aria-label={`${label} hue angle`}
      />
      <input
        type="number"
        min={0}
        max={360}
        value={hue}
        onChange={handleNumberInput}
        className={styles.hueNumber}
        aria-label={`${label} hue in degrees`}
      />
      <span className={styles.hueDegree}>deg</span>
    </div>
  );
}

export function ThemePicker() {
  const { huePrimary, hueAccent, setHuePrimary, setHueAccent, resetHues } =
    useTheme();

  return (
    <section>
      <div className={styles.sectionTitle}>Color Scheme</div>
      <p className={styles.sectionDescription}>
        Drag the sliders or click a swatch to pick a color. Changes apply
        instantly to every semantic token on this page and are saved to
        localStorage.
      </p>
      <div className={styles.hueControls}>
        <HueSlider label="Primary" hue={huePrimary} onChange={setHuePrimary} />
        <HueSlider label="Accent" hue={hueAccent} onChange={setHueAccent} />
      </div>
      <div className={styles.presetsRow}>
        <span className={styles.presetsLabel}>Presets</span>
        <div className={styles.presetButtons}>
          {themePresets.map((preset) => (
            <Btn
              key={preset.label}
              size="sm"
              onClick={() => {
                setHuePrimary(preset.huePrimary);
                setHueAccent(preset.hueAccent);
              }}
            >
              <span
                className={styles.presetDot}
                style={{
                  background: `oklch(65% 0.2 ${preset.huePrimary}deg)`,
                }}
              />
              {preset.label}
            </Btn>
          ))}
        </div>
        <Btn size="sm" onClick={resetHues}>
          Reset
        </Btn>
      </div>
    </section>
  );
}
