import { useEffect, useRef, type ReactNode } from "react";

interface ModalProps {
  children: ReactNode;
  title?: string;
  onClose: () => void;
}

export function Modal({ children, title, onClose }: ModalProps) {
  const dialogRef = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    dialogRef.current?.showModal();
  }, []);

  function handleCancel(e: React.SyntheticEvent) {
    e.preventDefault();
    onClose();
  }

  // Clicks on the <dialog> element itself (not its children) are backdrop clicks
  function handleClick(e: React.MouseEvent<HTMLDialogElement>) {
    if (e.target === dialogRef.current) onClose();
  }

  return (
    <dialog
      ref={dialogRef}
      onCancel={handleCancel}
      onClick={handleClick}
      aria-labelledby={title ? "modal-title" : undefined}
      aria-modal="true"
    >
      <div>
        {title && <h2 id="modal-title">{title}</h2>}
        <button type="button" aria-label="Close modal" onClick={onClose}>
          ×
        </button>
      </div>
      <div>{children}</div>
    </dialog>
  );
}
