import { useFullScreen } from "@/context/useFullScreen";
import type { ReactNode } from "react";
import styles from "./Layout.module.css";

interface LeftSidebarProps {
  children: ReactNode;
  id: string;
}

const RightSidebar = ({ children, id }: LeftSidebarProps) => {
  const { isFullScreen } = useFullScreen();
  return (
    <div
      id={id}
      className={`${styles.leftSidebar} ${isFullScreen ? styles.collapsed : " "}`}>
      {children}
    </div>
  );
};

export { RightSidebar };
