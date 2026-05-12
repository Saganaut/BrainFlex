// Dialog-element-based modal. Accepts an optional variant that tints the dialog
// surface via data-variant (see frontend/STYLES.md). Inner header / content
// keep their own surface tokens, so changing variant retints frame + edges only.
import { useEffect, useRef, type ReactNode } from "react";
import style from "./Modal.module.css";
import { IconBtn } from "../Buttons/IconBtn";
import type { BtnVariant } from "../Buttons/BtnTypes";
interface ModalProps {
  children: ReactNode;
  title?: string;
  variant?: BtnVariant;
  onClose: () => void;
}

const Modal = ({
  children,
  title,
  variant = "default",
  onClose,
}: ModalProps) => {
  const dialogRef = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    dialogRef.current?.showModal();
  }, []);

  function handleCancel(e: React.SyntheticEvent) {
    e.preventDefault();
    onClose();
  }

  function handleClick(e: React.MouseEvent<HTMLDialogElement>) {
    if (e.target === dialogRef.current) onClose();
  }

  return (
    <dialog
      ref={dialogRef}
      onCancel={handleCancel}
      onClick={handleClick}
      aria-labelledby={title ? "modal-title" : undefined}
      aria-modal='true'
      data-variant={variant}
      className={style.modal}>
      <div className={style.header}>
        {title && (
          <h2 id='modal-title' className={style.title}>
            {title}
          </h2>
        )}
        <IconBtn type='close' aria-label='Close modal' onClick={onClose} />
      </div>
      <div className={style.content}>{children}</div>
    </dialog>
  );
};

export { Modal };
