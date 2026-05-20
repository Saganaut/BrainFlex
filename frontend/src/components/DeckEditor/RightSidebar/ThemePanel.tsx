// Theme picker for the deck-editor right sidebar. A compact vertical list of
// presets + the user's custom themes — clicking a row activates it, "+ New
// theme" opens the shared ThemeEditor modal. All behavior comes from
// useThemePicker so this stays in sync with the AccountPage theme section.
import type { ThemeResponse } from "@/store/BrainFlexApi";
import { useThemePicker } from "@/hooks/useThemePicker";
import { Btn } from "@/components/Common/Buttons/Btn";
import styles from "./ThemePanel.module.css";

interface ThemeRowProps {
  name: string;
  huePrimary: number;
  hueAccent: number;
  isActive: boolean;
  isOrg?: boolean;
  onActivate: () => void;
}

const ThemeRow = ({
  name,
  huePrimary,
  hueAccent,
  isActive,
  isOrg,
  onActivate,
}: ThemeRowProps) => (
  <button
    type='button'
    className={`${styles.row} ${isActive ? styles.rowActive : ""}`}
    onClick={onActivate}
    aria-pressed={isActive}>
    <span className={styles.swatch} aria-hidden='true'>
      <span
        className={styles.swatchPrimary}
        style={{ background: `oklch(55% 0.2 ${String(huePrimary)}deg)` }}
      />
      <span
        className={styles.swatchAccent}
        style={{ background: `oklch(65% 0.22 ${String(hueAccent)}deg)` }}
      />
    </span>
    <span className={styles.rowLabel}>{name}</span>
    {isOrg && <span className={styles.orgBadge}>Org</span>}
    {isActive && <span className={styles.activeBadge}>Active</span>}
  </button>
);

const ThemePanel = () => {
  const {
    presets,
    themes,
    activeThemeId,
    customPresetActive,
    userId,
    activatePreset,
    activateCustom,
    openEditor,
  } = useThemePicker();

  const handleActivateCustom = (theme: ThemeResponse) => {
    void activateCustom(theme);
  };

  return (
    <div className={styles.panel}>
      <section className={styles.section}>
        <h4 className={styles.heading}>Presets</h4>
        <div className={styles.list}>
          {presets.map((preset) => (
            <ThemeRow
              key={preset.label}
              name={preset.label}
              huePrimary={preset.huePrimary}
              hueAccent={preset.hueAccent}
              // Brand counts as "active" only when there's no active custom
              // theme and the user hasn't tweaked hues into a custom override.
              isActive={
                !activeThemeId &&
                !customPresetActive &&
                preset.label === "Brand"
              }
              onActivate={() => {
                void activatePreset(preset);
              }}
            />
          ))}
        </div>
      </section>

      {themes.length > 0 && (
        <section className={styles.section}>
          <h4 className={styles.heading}>Your themes</h4>
          <div className={styles.list}>
            {themes.map((theme) => (
              <ThemeRow
                key={theme.id}
                name={theme.name ?? "Untitled"}
                huePrimary={theme.huePrimary ?? 260}
                hueAccent={theme.hueAccent ?? 25}
                isActive={theme.id === activeThemeId}
                isOrg={
                  theme.organizationId != null &&
                  theme.organizationId !== "" &&
                  theme.ownerId !== userId
                }
                onActivate={() => {
                  handleActivateCustom(theme);
                }}
              />
            ))}
          </div>
        </section>
      )}

      <Btn
        type='button'
        className={styles.newBtn}
        onClick={() => {
          openEditor();
        }}>
        + New theme
      </Btn>
    </div>
  );
};

export { ThemePanel };
