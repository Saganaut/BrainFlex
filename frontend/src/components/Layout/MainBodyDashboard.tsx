import type { ReactNode } from "react";
import styles from "./Layout.module.css";

interface MainBodyDashboardProps {
  children: ReactNode;
  id: string;
}

const MainBodyDashboard = ({ id, children }: MainBodyDashboardProps) => {
  return (
    <div id={id} className={`${styles.mainBodyDashboard} `}>
      {children}
    </div>
  );
};

export { MainBodyDashboard };
