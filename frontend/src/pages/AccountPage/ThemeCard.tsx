// Single theme card: color swatch preview, name, activate/edit/delete actions.
import { Btn } from "@components/Common/Buttons/Btn";
import type { ThemeResponse } from "../../store/BrainFlexApi";
import styles from "./ThemeSection.module.css";

interface ThemeCardProps {
  theme: ThemeResponse;
  isActive: boolean;
  isOwned: boolean;
  isOrgShared: boolean;
  onActivate: (theme: ThemeResponse) => void;
  onEdit?: (theme: ThemeResponse) => void;
  onDelete?: (theme: ThemeResponse) => void;
}

const ThemeCard = ({
  theme,
  isActive,
  isOwned,
  isOrgShared,
  onActivate,
  onEdit,
  onDelete,
}: ThemeCardProps) => {
  const primary = theme.huePrimary ?? 260;
  const accent = theme.hueAccent ?? 25;

  return (
    <div className={`${styles.card} ${isActive ? styles.cardActive : ""}`}>
      <div className={styles.cardSwatch} aria-hidden='true'>
        <div
          className={styles.swatchPrimary}
          style={{ background: `oklch(55% 0.2 ${primary}deg)` }}
        />
        <div
          className={styles.swatchAccent}
          style={{ background: `oklch(65% 0.22 ${accent}deg)` }}
        />
      </div>
      <div className={styles.cardBody}>
        <p className={styles.cardName} title={theme.name}>
          {theme.name ?? "Untitled"}
        </p>
        <div className={styles.cardMeta}>
          {isActive && (
            <span className={`${styles.badge} ${styles.badgeActive}`}>
              Active
            </span>
          )}
          {isOrgShared && (
            <span className={`${styles.badge} ${styles.badgeOrg}`}>Org</span>
          )}
        </div>
      </div>
      <div className={styles.cardActions}>
        {!isActive && (
          <Btn
            className={`${styles.cardActionBtn} ${styles.cardActionBtnPrimary}`}
            onClick={() => {
              onActivate(theme);
            }}>
            Activate
          </Btn>
        )}
        {isOwned && onEdit && (
          <Btn
            className={styles.cardActionBtn}
            onClick={() => {
              onEdit(theme);
            }}>
            Edit
          </Btn>
        )}
        {isOwned && onDelete && (
          <Btn
            className={`${styles.cardActionBtn} ${styles.cardActionBtnDanger}`}
            onClick={() => {
              onDelete(theme);
            }}>
            Delete
          </Btn>
        )}
      </div>
    </div>
  );
};

export { ThemeCard };
