// Theme settings section: preset themes, custom themes, and theme editor.
// Activation, deletion, and the editor-modal launcher all flow through
// useThemePicker so the deck-editor sidebar can reuse the exact same behavior.
import { useState } from "react";
import { useThemePicker } from "../../hooks/useThemePicker";
import { ThemeCard } from "./ThemeCard";
import styles from "./ThemeSection.module.css";
import accountStyles from "./AccountPage.module.css";
import { Btn } from "@/components/Common/Buttons/Btn";

const ThemeSection = () => {
  const {
    presets,
    themes,
    activeThemeId,
    customPresetActive,
    userId,
    activatePreset,
    activateCustom,
    deleteCustom,
    openEditor,
  } = useThemePicker();

  const [deleteError, setDeleteError] = useState<string | null>(null);

  const handleDelete = async (theme: Parameters<typeof deleteCustom>[0]) => {
    setDeleteError(null);
    try {
      await deleteCustom(theme);
    } catch {
      setDeleteError("Failed to delete theme.");
    }
  };

  return (
    <section className={accountStyles.section}>
      <div className={styles.sectionHeader}>
        <h2 className={accountStyles.sectionTitle}>Theme Settings</h2>
        <Btn
          type='button'
          onClick={() => {
            openEditor();
          }}>
          + New theme
        </Btn>
      </div>

      <div>
        <p className={styles.subsectionTitle}>Presets</p>
        <div className={styles.themeGrid}>
          {presets.map((preset) => (
            <div
              key={preset.label}
              className={`${styles.card} ${!activeThemeId && styles.cardActive}`}>
              <div className={styles.cardSwatch} aria-hidden='true'>
                <div
                  className={styles.swatchPrimary}
                  style={{
                    background: `oklch(55% 0.2 ${preset.huePrimary}deg)`,
                  }}
                />
                <div
                  className={styles.swatchAccent}
                  style={{
                    background: `oklch(65% 0.22 ${preset.hueAccent}deg)`,
                  }}
                />
              </div>
              <div className={styles.cardBody}>
                <p className={styles.cardName}>{preset.label}</p>
                <div className={styles.cardMeta}>
                  {!activeThemeId &&
                    !customPresetActive &&
                    preset.label === "Brand" && (
                      <span className={`${styles.badge} ${styles.badgeActive}`}>
                        Active
                      </span>
                    )}
                </div>
              </div>
              <div className={styles.cardActions}>
                <Btn
                  type='button'
                  className={`${styles.cardActionBtn} ${styles.cardActionBtnPrimary}`}
                  onClick={() => {
                    void activatePreset(preset);
                  }}>
                  Activate
                </Btn>
              </div>
            </div>
          ))}
        </div>
      </div>

      {themes.length > 0 && (
        <div>
          <p className={styles.subsectionTitle}>Custom Themes</p>
          <div className={styles.themeGrid}>
            {themes.map((theme) => (
              <ThemeCard
                key={theme.id}
                theme={theme}
                isActive={theme.id === activeThemeId}
                isOwned={theme.ownerId === userId}
                isOrgShared={
                  theme.organizationId != null &&
                  theme.organizationId !== "" &&
                  theme.ownerId !== userId
                }
                onActivate={(t) => {
                  void activateCustom(t);
                }}
                onEdit={
                  theme.ownerId === userId
                    ? (t) => {
                        openEditor(t);
                      }
                    : undefined
                }
                onDelete={
                  theme.ownerId === userId
                    ? (t) => {
                        void handleDelete(t);
                      }
                    : undefined
                }
              />
            ))}
          </div>
        </div>
      )}

      {deleteError && <p className={accountStyles.error}>{deleteError}</p>}
    </section>
  );
};

export { ThemeSection };
