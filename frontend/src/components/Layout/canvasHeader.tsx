import { useFullScreen } from "@/context/useFullScreen";
import type { ReactNode } from "react";
import styles from "./Layout.module.css";

interface CanvasHeaderProps {
  children: ReactNode;
}

const CanvasHeader = ({ children }: CanvasHeaderProps) => {
  const { isFullScreen } = useFullScreen();
  return (
    <div
      className={`${styles.canvasHeader} ${isFullScreen ? styles.collapsed : " "}`}>
      {children}
    </div>
  );
};

export { CanvasHeader };
