/** Used to wrap the main body (everything but the navbar) of main page components that should not exceed 100dvh **/

import React, { type ReactNode } from "react";
import styles from "./Containers.module.css";
import { useFullScreen } from "@/context/useFullScreen";

interface DashboardContainerProps {
  children: ReactNode;
}

const DashboardContainer: React.FC<DashboardContainerProps> = ({
  children,
}) => {
  const { isFullScreen } = useFullScreen();

  return (
    <div
      className={`${styles.dashboardContainer} ${isFullScreen ? styles.isFullScreen : ""}`}>
      {children}
    </div>
  );
};

export { DashboardContainer };
