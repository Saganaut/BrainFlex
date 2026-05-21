// Compact "Scan to join" trigger: shows a QR thumbnail inline and opens the
// full-size DisplayQR in a modal on click. Useful in tight spots (lobby
// header, host sidebar) where there isn't room for the full DisplayQR card
// but players still need a tappable way to surface the scannable code.
import { useState, type ReactNode } from "react";
import { Modal } from "@/components/Common/Modal/Modal";
import { DisplayQR } from "../DisplayQR/DisplayQR";
import styles from "./ScanToJoinBtn.module.css";

interface ScanToJoinBtnProps {
  value: string;
  label?: string;
  thumbSize?: number;
  modalSize?: number;
  caption?: string;
  render?: (value: string, size: number) => ReactNode;
  className?: string;
}

const DEFAULT_THUMB_SIZE = 48;
const DEFAULT_MODAL_SIZE = 280;

const ScanToJoinBtn = ({
  value,
  label = "Scan to join",
  thumbSize = DEFAULT_THUMB_SIZE,
  modalSize = DEFAULT_MODAL_SIZE,
  caption,
  render,
  className,
}: ScanToJoinBtnProps) => {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <>
      <button
        type='button'
        className={[styles.btn, className].filter(Boolean).join(" ")}
        onClick={() => {
          setIsOpen(true);
        }}
        aria-label={`${label} (open full QR code)`}>
        <span
          className={styles.thumb}
          style={{ width: thumbSize, height: thumbSize }}
          aria-hidden='true'>
          {render != null ? (
            render(value, thumbSize)
          ) : (
            <span className={styles.thumbPlaceholder} />
          )}
        </span>
        <span className={styles.label}>{label}</span>
      </button>

      {isOpen && (
        <Modal
          title={label}
          onClose={() => {
            setIsOpen(false);
          }}>
          <div className={styles.modalBody}>
            <DisplayQR
              value={value}
              label={label}
              caption={caption}
              size={modalSize}
              render={render}
            />
          </div>
        </Modal>
      )}
    </>
  );
};

export { ScanToJoinBtn };
export type { ScanToJoinBtnProps };
