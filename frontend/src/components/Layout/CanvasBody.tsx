import React, { type ReactNode } from "react";
import styles from "./Layout.module.css";

interface CanvasBodyProps {
  id: string;
  children: ReactNode;
}

const CanvasBody: React.FC<CanvasBodyProps> = ({ id, children }) => {
  return (
    <div id={id} className={styles.canvasBody}>
      {children}
    </div>
  );
};

export { CanvasBody };
