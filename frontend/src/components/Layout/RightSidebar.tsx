import { useFullScreen } from "@/context/useFullScreen";
import type { ReactNode } from "react";
import styles from "./Layout.module.css";

interface LeftSidebarProps {
  children: ReactNode;
}

const RightSidebar = ({ children }: LeftSidebarProps) => {
  const { isFullScreen } = useFullScreen();
  return (
    <div
      className={`${styles.rightSidebar} ${isFullScreen ? styles.isCollapsed : " "}`}>
      {children}
    </div>
  );
};

export { RightSidebar };
