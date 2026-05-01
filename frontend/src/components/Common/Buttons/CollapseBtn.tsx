import React from "react";
import styles from "./Buttons.module.css";
interface CollapseBtnProps {
  collapse: React.Dispatch<React.SetStateAction<boolean>>;
  isCollapsed: boolean;
}

const CollapseBtn: React.FC<CollapseBtnProps> = ({ collapse, isCollapsed }) => {
  return (
    <button
      onClick={() => {
        collapse(!isCollapsed);
      }}>
      <svg
        xmlns='http://www.w3.org/2000/svg'
        fill='none'
        viewBox='0 0 24 24'
        strokeWidth={1.5}
        stroke='white'
        className={`${isCollapsed ? styles.isCollapsed : ""}`}>
        <path
          strokeLinecap='round'
          strokeLinejoin='round'
          d='m19.5 8.25-7.5 7.5-7.5-7.5'
        />
      </svg>
    </button>
  );
};

export { CollapseBtn };
