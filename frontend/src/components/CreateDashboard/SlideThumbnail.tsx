import React from "react";
import styles from "./CreateDashboard.module.css";
interface SlideThumbnailProps {
  name: string;
  id: string;
  type: string;
  onClick: () => void;
  index: number;
  currentQuestionId?: string;
}
import { useSortable } from "@dnd-kit/react/sortable";

const SlideThumbnail: React.FC<SlideThumbnailProps> = ({
  name,
  id,
  type,
  index,
  currentQuestionId,
  onClick,
}) => {
  const { ref } = useSortable({ id, index });
  console.log("index", index);
  return (
    <div
      ref={ref}
      onClick={onClick}
      className={`${styles.slideThumbnail} ${currentQuestionId == id && styles.active}`}>
      <div>
        {index} {name} {id} {type}
      </div>
    </div>
  );
};

export { SlideThumbnail };
