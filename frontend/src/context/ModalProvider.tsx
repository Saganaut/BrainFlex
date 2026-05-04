import { createContext, useCallback, useContext, useState, type ReactNode } from "react";
import { Modal } from "../components/Common/Modal";

interface ModalConfig {
  content: ReactNode;
  title?: string;
}

interface ModalContextValue {
  openModal: (config: ModalConfig) => void;
  closeModal: () => void;
}

const ModalContext = createContext<ModalContextValue | null>(null);

export function ModalProvider({ children }: { children: ReactNode }) {
  const [config, setConfig] = useState<ModalConfig | null>(null);

  const openModal = useCallback((cfg: ModalConfig) => setConfig(cfg), []);
  const closeModal = useCallback(() => setConfig(null), []);

  return (
    <ModalContext.Provider value={{ openModal, closeModal }}>
      {children}
      {config && (
        <Modal title={config.title} onClose={closeModal}>
          {config.content}
        </Modal>
      )}
    </ModalContext.Provider>
  );
}

export function useModal(): ModalContextValue {
  const ctx = useContext(ModalContext);
  if (!ctx) throw new Error("useModal must be used within a ModalProvider");
  return ctx;
}
