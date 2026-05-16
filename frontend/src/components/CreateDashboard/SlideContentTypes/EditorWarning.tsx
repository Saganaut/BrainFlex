/**
 * Inline warning banner used inside slide-content editors when an author's
 * configuration is valid-to-save but problematic at showcase time (e.g. an
 * MCQ with no correct answer). Composes shared tokens — no per-editor copies
 * of the warning CSS.
 */
import type { ReactNode } from "react";
import { ExclamationTriangleIcon } from "@heroicons/react/24/outline";
import styles from "./SlideContentTypes.module.css";

interface EditorWarningProps {
  children: ReactNode;
}

const EditorWarning = ({ children }: EditorWarningProps) => (
  <p className={styles.warning} role='alert'>
    <span className={styles.warningIcon} aria-hidden='true'>
      <ExclamationTriangleIcon />
    </span>
    <span>{children}</span>
  </p>
);

export { EditorWarning };
