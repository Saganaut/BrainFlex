import React, { type ReactNode } from "react";
import styles from "./Layout.module.css";
interface InnerDisplayProps {
  id?: string;
  children: ReactNode;
}

const InnerDisplay: React.FC<InnerDisplayProps> = ({ children, id }) => {
  return (
    <div id={id} className={styles.innerDisplay}>
      {children}
    </div>
  );
};

export { InnerDisplay };
