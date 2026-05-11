import React from "react";
import styles from "./CreateDashboard.module.css";
interface SlideThumbnailProps {
  name: string;
  id: string;
  type: string;
  onClick: () => void;
  index: number;
}
import { useSortable } from "@dnd-kit/react/sortable";

const SlideThumbnail: React.FC<SlideThumbnailProps> = ({
  name,
  id,
  type,
  index,
  onClick,
}) => {
  const { ref } = useSortable({ id, index });
  console.log("index", index);
  return (
    <button ref={ref} onClick={onClick} className={styles.slideThumbnail}>
      <div>
        {index} {name} {id} {type}
      </div>
    </button>
  );
};

export { SlideThumbnail };
