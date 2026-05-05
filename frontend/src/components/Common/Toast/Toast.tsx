import { useEffect } from "react";
import type { ToastItem } from "./ToastTypes";
import styles from "./Toast.module.css";

interface ToastProps extends ToastItem {
  onDismiss: (id: string) => void;
}

const Toast = ({
  id,
  message,
  variant = "info",
  duration = 30,
  onDismiss,
}: ToastProps) => {
  useEffect(() => {
    if (duration === 0) return;
    const timer = setTimeout(() => onDismiss(id), duration);
    return () => clearTimeout(timer);
  }, [id, duration, onDismiss]);

  return (
    <div
      className={`${styles.toast} ${styles[variant]}`}
      role='alert'
      aria-live='polite'
      aria-atomic='true'>
      <span className={styles.message}>{message}</span>
      <button
        type='button'
        className={styles.dismiss}
        aria-label='Dismiss notification'
        onClick={() => onDismiss(id)}>
        ×
      </button>
    </div>
  );
};

const ToastContainer = ({ children }: { children: React.ReactNode }) => (
  <div className={styles.toastContainer} aria-label='Notifications'>
    {children}
  </div>
);

export { Toast, ToastContainer };
