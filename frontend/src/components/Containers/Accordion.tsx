import React, { useState, type ReactNode } from "react";
import styles from "./Containers.module.css";
import { ChevronDownIcon } from "@heroicons/react/24/solid";

interface AccordionProps {
  titleBar: string;
  children: ReactNode;
}

const Accordion: React.FC<AccordionProps> = ({ titleBar, children }) => {
  const [isCollapsed, setIsCollapsed] = useState(true);

  return (
    <div className={styles.accordion}>
      <div
        className={`${styles.accordionTitleSection} ${isCollapsed ? styles.isCollapsed : ""}`}
        onClick={() => {
          setIsCollapsed(!isCollapsed);
        }}>
        <h4> {titleBar}</h4> <ChevronDownIcon />
      </div>

      <div
        className={`${styles.collapsableSection} ${isCollapsed ? styles.isCollapsed : ""}`}>
        {" "}
        {children}
      </div>
    </div>
  );
};

export { Accordion };
