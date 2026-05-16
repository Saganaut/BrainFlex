import React, { type ReactNode } from "react";
import styles from "./Layout.module.css";

interface CanvasBodyProps {
  children: ReactNode;
}

const CanvasBody: React.FC<CanvasBodyProps> = ({ children }) => {
  return <div className={styles.canvasBody}>{children}</div>;
};

export { CanvasBody };
