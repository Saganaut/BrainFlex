// Form to create or edit a custom theme.
// Handles name, hue sliders, mode, image uploads, and org scope. The form is
// rendered inside the shared Modal (see useModal) — callers open and close the
// dialog and supply the org list the author can pick from.
import { useState } from "react";
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
import { Alert } from "@/components/Common/Alert/Alert";
import { Btn } from "@/components/Common/Buttons/Btn";
import { Dropdown } from "@/components/Common/Input/Dropdown/Dropdown";
import { FileUpload } from "@/components/Common/Input/FileUpload/FileUpload";
import { ColorPicker } from "@/components/Common/Input/ColorPicker/ColorPicker";
import { Input } from "@/components/Common/Input/Input/Input";
import { RadioGroup } from "@/components/Common/Input/RadioGroup/RadioGroup";
import { extractErrorMessage } from "@/utils/utils";

// Sentinel option value for "no org" / personal theme. Backend treats an empty
// organizationId as personal, so we just map this back to "" on save.
const PERSONAL_SCOPE = "__personal__";

const MODE_OPTIONS = [
  { value: "light", label: "Light" },
  { value: "dark", label: "Dark" },
  { value: "system", label: "System" },
];

type Mode = "light" | "dark" | "system";

interface OrgOption {
  id: string;
  name: string;
}

interface ThemeEditorProps {
  /** Provide an existing theme to edit it; omit to create a new one. */
  existing?: ThemeResponse;
  /** Organizations the user belongs to. Empty list = personal-only. */
  organizations?: OrgOption[];
  onSaved: (theme: ThemeResponse) => void;
  onCancel: () => void;
}

const ThemeEditor = ({
  existing,
  organizations = [],
  onSaved,
  onCancel,
}: ThemeEditorProps) => {
  const [name, setName] = useState(existing?.name ?? "");
  const [huePrimary, setHuePrimary] = useState(existing?.huePrimary ?? 260);
  const [hueAccent, setHueAccent] = useState(existing?.hueAccent ?? 25);
  const [mode, setMode] = useState<Mode>(
    (existing?.mode as Mode | undefined) ?? "system",
  );

  // Pick "Personal" by default; pre-select an existing theme's org if it has one.
  const [scopeId, setScopeId] = useState<string>(() => {
    const existingOrg = existing?.organizationId;
    if (existingOrg && existingOrg !== "") return existingOrg;
    return PERSONAL_SCOPE;
  });

  const [bgPreview, setBgPreview] = useState<string | null>(
    existing?.backgroundImageUrl ?? null,
  );
  const [logoPreview, setLogoPreview] = useState<string | null>(
    existing?.logoImageUrl ?? null,
  );
  const [bgFile, setBgFile] = useState<File | null>(null);
  const [logoFile, setLogoFile] = useState<File | null>(null);

  const [error, setError] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  const [createTheme] = useCreateThemeMutation();
  const [updateTheme] = useUpdateThemeMutation();
  const [uploadBackground] = useUploadBackgroundMutation();
  const [uploadLogo] = useUploadLogoMutation();

  const scopeOptions = [
    { value: PERSONAL_SCOPE, label: "Personal (only you)" },
    ...organizations.map((o) => ({ value: o.id, label: o.name })),
  ];

  // FileUpload returns the full accumulated list each change; treat the most
  // recent entry as the chosen file so re-picking replaces the previous one.
  const handleBgFiles = (files: File[]) => {
    const file = files.at(-1);
    if (!file) return;
    if (file.size > 5 * 1024 * 1024) {
      setError("Background image exceeds 5 MB.");
      return;
    }
    setBgFile(file);
    setBgPreview(URL.createObjectURL(file));
  };

  const handleLogoFiles = (files: File[]) => {
    const file = files.at(-1);
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
      const orgId = scopeId === PERSONAL_SCOPE ? "" : scopeId;

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
      setError(extractErrorMessage(err, "Failed to save theme. Please try again."));
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className={styles.editorForm}>
      <Input
        id='theme-name'
        type='text'
        label='Name'
        value={name}
        onChange={(e) => {
          setName(e.target.value);
        }}
        placeholder='My Theme'
        maxLength={64}
        fullWidth
      />

      <div className={styles.fieldGroup}>
        <ColorPicker label='Primary' value={huePrimary} onChange={setHuePrimary} />
        <ColorPicker label='Accent' value={hueAccent} onChange={setHueAccent} />
      </div>

      <RadioGroup
        name='theme-mode'
        legend='Mode'
        options={MODE_OPTIONS}
        value={mode}
        onChange={(v) => {
          setMode(v as Mode);
        }}
      />

      <div className={styles.fieldGroup}>
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
          <FileUpload
            label='Background Image'
            accept='image/jpeg,image/png,image/webp'
            onChange={handleBgFiles}
            infoMessage='JPEG, PNG, or WebP · max 5 MB · max 2000 px (optional)'
          />
        </div>
      </div>

      <div className={styles.fieldGroup}>
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
          <FileUpload
            label='Logo Image'
            accept='image/jpeg,image/png,image/webp,image/gif'
            onChange={handleLogoFiles}
            infoMessage='JPEG, PNG, WebP, or GIF · max 2 MB · resized to 400×400 (optional)'
          />
        </div>
      </div>

      <Dropdown
        id='theme-scope'
        label='Scope'
        options={scopeOptions}
        value={[scopeId]}
        onChange={(values) => {
          if (values[0]) setScopeId(values[0]);
        }}
        infoMessage={
          organizations.length === 0
            ? "Join an organization to share themes with its members."
            : undefined
        }
      />

      {error != null && error !== "" && (
        <Alert severity='error'>{error}</Alert>
      )}

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
export type { OrgOption };
