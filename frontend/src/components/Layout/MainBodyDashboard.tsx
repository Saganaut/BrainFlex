import type { ReactNode } from "react";
import styles from "./Layout.module.css";

interface MainBodyDashboardProps {
  children: ReactNode;
}

const MainBodyDashboard = ({ children }: MainBodyDashboardProps) => {
  return <div className={`${styles.mainBodyDashboard} `}>{children}</div>;
};

export { MainBodyDashboard };
