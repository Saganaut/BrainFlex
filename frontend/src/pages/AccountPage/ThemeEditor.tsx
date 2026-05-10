// Form to create or edit a custom theme.
// Handles name, hue sliders, mode, image uploads, and org scope.
import type { ChangeEvent } from "react";
import { useRef, useState } from "react";
import type {
  CreateThemeRequest,
  ThemeResponse,
  UpdateThemeRequest,
} from "../../store/BrainFlexApi";
import {
  useCreateThemeMutation,
  useUpdateThemeMutation,
  useUploadBackgroundMutation,
  useUploadLogoMutation,
} from "../../store/BrainFlexApi";
import styles from "./ThemeSection.module.css";
import { Btn } from "@/components/Common/Buttons/Btn";

// Converts a hex color string to its oklch hue angle (0–360°) via Oklab matrices.
function hexToOklchHue(hex: string): number {
  const r = parseInt(hex.slice(1, 3), 16) / 255;
  const g = parseInt(hex.slice(3, 5), 16) / 255;
  const b = parseInt(hex.slice(5, 7), 16) / 255;
  const toLinear = (v: number) =>
    v <= 0.04045 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4;
  const rl = toLinear(r);
  const gl = toLinear(g);
  const bl = toLinear(b);
  const lC = Math.cbrt(
    0.4122214708 * rl + 0.5363325363 * gl + 0.0514459929 * bl,
  );
  const mC = Math.cbrt(
    0.2119034982 * rl + 0.6806995451 * gl + 0.1073969566 * bl,
  );
  const sC = Math.cbrt(
    0.0883024619 * rl + 0.2817188376 * gl + 0.6299787005 * bl,
  );
  const a = 1.977998495 * lC - 2.428592205 * mC + 0.45059371 * sC;
  const bOk = 0.025904037 * lC + 0.782771766 * mC - 0.808675766 * sC;
  const hue = (Math.atan2(bOk, a) * 180) / Math.PI;
  return hue < 0 ? hue + 360 : hue;
}

interface HueSliderProps {
  label: string;
  hue: number;
  onChange: (hue: number) => void;
}

const HueSlider = ({ label, hue, onChange }: HueSliderProps) => {
  const handleColorPicker = (e: ChangeEvent<HTMLInputElement>) => {
    onChange(Math.round(hexToOklchHue(e.target.value)));
  };

  return (
    <div className={styles.hueRow}>
      <span className={styles.hueLabel}>{label}</span>
      <label
        className={styles.hueSwatch}
        style={{ background: `oklch(65% 0.2 ${hue}deg)` }}
        title={`Pick ${label.toLowerCase()} color`}
        aria-label={`Pick ${label.toLowerCase()} color`}>
        <input
          type='color'
          className={styles.colorPickerInput}
          onChange={handleColorPicker}
        />
      </label>
      <input
        type='range'
        min={0}
        max={360}
        value={hue}
        onChange={(e) => {
          onChange(Number(e.target.value));
        }}
        className={styles.hueSlider}
        aria-label={`${label} hue angle`}
      />
      <input
        type='number'
        min={0}
        max={360}
        value={hue}
        onChange={(e) => {
          const n = Number(e.target.value);
          if (Number.isFinite(n)) onChange(n);
        }}
        className={styles.hueNumber}
        aria-label={`${label} hue in degrees`}
      />
    </div>
  );
};

type Mode = "light" | "dark" | "system";

interface ThemeEditorProps {
  /** Provide an existing theme to edit it; omit to create a new one. */
  existing?: ThemeResponse;
  /** The current user's organizationId, if any. */
  organizationId?: string;
  onSaved: (theme: ThemeResponse) => void;
  onCancel: () => void;
}

const ThemeEditor = ({
  existing,
  organizationId,
  onSaved,
  onCancel,
}: ThemeEditorProps) => {
  const [name, setName] = useState(existing?.name ?? "");
  const [huePrimary, setHuePrimary] = useState(existing?.huePrimary ?? 260);
  const [hueAccent, setHueAccent] = useState(existing?.hueAccent ?? 25);
  const [mode, setMode] = useState<Mode>(
    (existing?.mode as Mode | undefined) ?? "system",
  );
  const [shareWithOrg, setShareWithOrg] = useState(
    existing?.organizationId != null && existing.organizationId !== "",
  );

  const [bgPreview, setBgPreview] = useState<string | null>(
    existing?.backgroundImageUrl ?? null,
  );
  const [logoPreview, setLogoPreview] = useState<string | null>(
    existing?.logoImageUrl ?? null,
  );
  const [bgFile, setBgFile] = useState<File | null>(null);
  const [logoFile, setLogoFile] = useState<File | null>(null);

  const bgInputRef = useRef<HTMLInputElement>(null);
  const logoInputRef = useRef<HTMLInputElement>(null);

  const [error, setError] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  const [createTheme] = useCreateThemeMutation();
  const [updateTheme] = useUpdateThemeMutation();
  const [uploadBackground] = useUploadBackgroundMutation();
  const [uploadLogo] = useUploadLogoMutation();

  const handleBgChange = (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (file.size > 5 * 1024 * 1024) {
      setError("Background image exceeds 5 MB.");
      return;
    }
    setBgFile(file);
    setBgPreview(URL.createObjectURL(file));
  };

  const handleLogoChange = (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (file.size > 2 * 1024 * 1024) {
      setError("Logo image exceeds 2 MB.");
      return;
    }
    setLogoFile(file);
    setLogoPreview(URL.createObjectURL(file));
  };

  const handleSave = async () => {
    if (!name.trim()) {
      setError("Theme name is required.");
      return;
    }
    setError(null);
    setIsSaving(true);

    try {
      let saved: ThemeResponse;
      const orgId = shareWithOrg && organizationId ? organizationId : "";

      if (existing?.id) {
        const req: UpdateThemeRequest = {
          name: name.trim(),
          huePrimary,
          hueAccent,
          mode,
          organizationId: orgId,
        };
        saved = await updateTheme({
          id: existing.id,
          updateThemeRequest: req,
        }).unwrap();
      } else {
        const req: CreateThemeRequest = {
          name: name.trim(),
          huePrimary,
          hueAccent,
          mode,
          organizationId: orgId,
        };
        saved = await createTheme({ createThemeRequest: req }).unwrap();
      }

      if (bgFile && saved.id) {
        const fd = new FormData();
        fd.append("image", bgFile);
        saved = await uploadBackground({
          id: saved.id,
          body: fd as unknown as { image: Blob },
        }).unwrap();
      }

      if (logoFile && saved.id) {
        const fd = new FormData();
        fd.append("image", logoFile);
        saved = await uploadLogo({
          id: saved.id,
          body: fd as unknown as { image: Blob },
        }).unwrap();
      }

      onSaved(saved);
    } catch (err: unknown) {
      const msg =
        err && typeof err === "object" && "data" in err
          ? String(err.data)
          : null;
      setError(msg ?? "Failed to save theme. Please try again.");
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className={styles.editorForm}>
      <div className={styles.fieldGroup}>
        <label className={styles.fieldLabel} htmlFor='theme-name'>
          Name
        </label>
        <input
          id='theme-name'
          type='text'
          className={styles.fieldInput}
          value={name}
          onChange={(e) => {
            setName(e.target.value);
          }}
          placeholder='My Theme'
          maxLength={64}
        />
      </div>

      <div className={styles.fieldGroup}>
        <span className={styles.fieldLabel}>Colors</span>
        <HueSlider label='Primary' hue={huePrimary} onChange={setHuePrimary} />
        <HueSlider label='Accent' hue={hueAccent} onChange={setHueAccent} />
      </div>

      <div className={styles.fieldGroup}>
        <span className={styles.fieldLabel}>Mode</span>
        <div className={styles.modeRow}>
          {(["light", "dark", "system"] as const).map((m) => (
            <Btn
              key={m}
              type='button'
              className={`${styles.modeBtn} ${mode === m ? styles.modeBtnActive : ""}`}
              onClick={() => {
                setMode(m);
              }}>
              {m.charAt(0).toUpperCase() + m.slice(1)}
            </Btn>
          ))}
        </div>
      </div>

      <div className={styles.fieldGroup}>
        <span className={styles.fieldLabel}>Background Image</span>
        <div className={styles.uploadRow}>
          {bgPreview ? (
            <img
              src={bgPreview}
              alt='Background preview'
              className={styles.uploadThumb}
            />
          ) : (
            <div className={styles.uploadThumbPlaceholder} aria-hidden='true'>
              None
            </div>
          )}
          <div>
            <input
              ref={bgInputRef}
              type='file'
              accept='image/jpeg,image/png,image/webp'
              onChange={handleBgChange}
              style={{ display: "none" }}
              aria-label='Upload background image'
            />
            <Btn
              type='button'
              onClick={() => {
                bgInputRef.current?.click();
              }}>
              {bgPreview ? "Replace" : "Upload"}
            </Btn>
            <p className={styles.uploadHint}>
              JPEG, PNG, or WebP · max 5 MB · max 2000 px
            </p>
          </div>
        </div>
      </div>

      <div className={styles.fieldGroup}>
        <span className={styles.fieldLabel}>Logo Image</span>
        <div className={styles.uploadRow}>
          {logoPreview ? (
            <img
              src={logoPreview}
              alt='Logo preview'
              className={styles.uploadThumb}
            />
          ) : (
            <div className={styles.uploadThumbPlaceholder} aria-hidden='true'>
              None
            </div>
          )}
          <div>
            <input
              ref={logoInputRef}
              type='file'
              accept='image/jpeg,image/png,image/webp,image/gif'
              onChange={handleLogoChange}
              style={{ display: "none" }}
              aria-label='Upload logo image'
            />
            <Btn
              type='button'
              onClick={() => {
                logoInputRef.current?.click();
              }}>
              {logoPreview ? "Replace" : "Upload"}
            </Btn>
            <p className={styles.uploadHint}>
              JPEG, PNG, WebP, or GIF · max 2 MB · resized to 400×400
            </p>
          </div>
        </div>
      </div>

      {organizationId && (
        <label className={styles.scopeToggle}>
          <input
            type='checkbox'
            checked={shareWithOrg}
            onChange={(e) => {
              setShareWithOrg(e.target.checked);
            }}
          />
          Share with organization
        </label>
      )}

      {error && <p className={styles.editorError}>{error}</p>}

      <div className={styles.editorActions}>
        <Btn type='button' onClick={onCancel} disabled={isSaving}>
          Cancel
        </Btn>
        <Btn
          type='button'
          onClick={() => {
            void handleSave();
          }}
          disabled={isSaving}>
          {isSaving ? "Saving..." : "Save theme"}
        </Btn>
      </div>
    </div>
  );
};

export { ThemeEditor };
