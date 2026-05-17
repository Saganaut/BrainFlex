import React, { type ReactNode } from "react";
import styles from "./Layout.module.css";
import { useFullScreen } from "@/context/useFullScreen";

interface CanvasBodyProps {
  children: ReactNode;
}

const CanvasBody: React.FC<CanvasBodyProps> = ({ children }) => {
  const { isFullScreen } = useFullScreen();
  return (
    <div
      className={`${styles.canvasBody} ${isFullScreen ? styles.isCollapsed : " "}`}>
      {children}
    </div>
  );
};

export { CanvasBody };
