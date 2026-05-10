// Theme settings section: preset themes, custom themes, and theme editor.
// Handles activating themes (applies hues immediately) and CRUD for custom themes.
import { useState } from "react";
import type { ThemeResponse } from "../../store/BrainFlexApi";
import {
  useDeleteThemeMutation,
  useGetCurrentUserQuery,
  useListThemesQuery,
  useUpdateProfileMutation,
} from "../../store/BrainFlexApi";
import { useTheme } from "../../hooks/useTheme";
import { useCurrentUser } from "../../hooks/useCurrentUser";
import { themePresets } from "../DesignSystemPage/data";
import { ThemeCard } from "./ThemeCard";
import { ThemeEditor } from "./ThemeEditor";
import styles from "./ThemeSection.module.css";
import accountStyles from "./AccountPage.module.css";
import { Btn } from "@/components/Common/Buttons/Btn";

const ThemeSection = () => {
  const userState = useCurrentUser();
  const { refetch: refetchUser } = useGetCurrentUserQuery();
  const registeredUser =
    userState.state === "registered" ? userState.user : null;

  const { data: themes = [], refetch: refetchThemes } = useListThemesQuery(
    undefined,
    { skip: !registeredUser },
  );

  const [updateProfile] = useUpdateProfileMutation();
  const [deleteTheme] = useDeleteThemeMutation();

  const { setTheme: applyMode, setHuePrimary, setHueAccent } = useTheme();

  const [editing, setEditing] = useState<ThemeResponse | null | "new">(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const activeThemeId = registeredUser?.activeThemeId;
  const userId = registeredUser?.id;
  const organizationId = registeredUser?.organizationId ?? undefined;

  const handleActivatePreset = async (
    huePrimary: number,
    hueAccent: number,
  ) => {
    setHuePrimary(huePrimary);
    setHueAccent(hueAccent);
    // Clear any custom active theme when switching to a preset
    await updateProfile({
      updateProfileRequest: { activeThemeId: "" },
    }).unwrap();
    await refetchUser();
  };

  const handleActivateCustom = async (theme: ThemeResponse) => {
    if (!theme.id) return;
    setHuePrimary(theme.huePrimary ?? 260);
    setHueAccent(theme.hueAccent ?? 25);
    if (theme.mode === "light" || theme.mode === "dark") {
      applyMode(theme.mode);
    }
    await updateProfile({
      updateProfileRequest: { activeThemeId: theme.id },
    }).unwrap();
    await refetchUser();
  };

  const handleDelete = async (theme: ThemeResponse) => {
    if (!theme.id) return;
    setDeleteError(null);
    try {
      await deleteTheme({ id: theme.id }).unwrap();
      // If the deleted theme was active, clear it
      if (activeThemeId === theme.id) {
        await updateProfile({
          updateProfileRequest: { activeThemeId: "" },
        }).unwrap();
        await refetchUser();
      }
      await refetchThemes();
    } catch {
      setDeleteError("Failed to delete theme.");
    }
  };

  const handleSaved = async () => {
    setEditing(null);
    await refetchThemes();
  };

  return (
    <section className={accountStyles.section}>
      <div className={styles.sectionHeader}>
        <h2 className={accountStyles.sectionTitle}>Theme Settings</h2>
        <Btn
          type='button'
          onClick={() => {
            setEditing("new");
          }}>
          + New theme
        </Btn>
      </div>

      {editing != null && (
        <ThemeEditor
          existing={editing === "new" ? undefined : editing}
          organizationId={organizationId}
          onSaved={() => {
            void handleSaved();
          }}
          onCancel={() => {
            setEditing(null);
          }}
        />
      )}

      <div>
        <p className={styles.subsectionTitle}>Presets</p>
        <div className={styles.themeGrid}>
          {themePresets.map((preset) => (
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
                  {!activeThemeId && preset.label === "Default" && (
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
                    void handleActivatePreset(
                      preset.huePrimary,
                      preset.hueAccent,
                    );
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
                  void handleActivateCustom(t);
                }}
                onEdit={
                  theme.ownerId === userId
                    ? (t) => {
                        setEditing(t);
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
