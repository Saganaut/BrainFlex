import { useEffect, useRef, type ReactNode } from "react";
import style from "./Modal.module.css";
interface ModalProps {
  children: ReactNode;
  title?: string;
  onClose: () => void;
}

const Modal = ({ children, title, onClose }: ModalProps) => {
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
      className={style.modal}>
      <div className={style.header}>
        {title && (
          <h2 id='modal-title' className={style.title}>
            {title}
          </h2>
        )}
        <button type='button' aria-label='Close modal' onClick={onClose}>
          ×
        </button>
      </div>
      <div className={style.content}>{children}</div>
    </dialog>
  );
};

export { Modal };
