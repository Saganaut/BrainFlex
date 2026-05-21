// Host-facing card that renders a scannable QR code for the join URL so a
// player on a phone can tap-to-join without typing. Pairs with
// DisplayJoinCode. The actual QR pixels are produced by a generator function
// passed in via `render` — keeps this file free of QR-library dependencies
// so callers can plug in whichever generator the app standardizes on (an
// <img> against a service, qrcode.react, etc.) without changing the
// surrounding chrome.
import type { ReactNode } from "react";
import styles from "./DisplayQR.module.css";

interface DisplayQRProps {
  value: string;
  label?: string;
  caption?: string;
  size?: number;
  render?: (value: string, size: number) => ReactNode;
  className?: string;
}

const DEFAULT_SIZE = 220;

const DisplayQR = ({
  value,
  label = "Scan to join",
  caption,
  size = DEFAULT_SIZE,
  render,
  className,
}: DisplayQRProps) => {
  return (
    <div
      className={[styles.card, className].filter(Boolean).join(" ")}
      aria-label={`${label}: ${value}`}>
      <span className={styles.label}>{label}</span>
      <div
        className={styles.qrFrame}
        style={{ width: size, height: size }}
        role='img'
        aria-label={`QR code for ${value}`}>
        {render != null ? render(value, size) : <QrPlaceholder value={value} />}
      </div>
      {caption != null && <span className={styles.caption}>{caption}</span>}
    </div>
  );
};

const QrPlaceholder = ({ value }: { value: string }) => (
  <span className={styles.placeholder}>{value}</span>
);

export { DisplayQR };
export type { DisplayQRProps };
