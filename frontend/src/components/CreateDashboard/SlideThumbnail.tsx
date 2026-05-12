import React from "react";
import styles from "./CreateDashboard.module.css";
interface SlideThumbnailProps {
  name: string;
  id: string;
  type: string;
  index: number;
  currentQuestionId?: string;
}
import { useSortable } from "@dnd-kit/react/sortable";
import { DropdownMenu } from "../Menus/DropdownMenu";
import { getRouteApi, useNavigate } from "@tanstack/react-router";
const routeApi = getRouteApi("/decks/$deckId/view");

const SlideThumbnail: React.FC<SlideThumbnailProps> = ({
  name,
  id,
  type,
  index,
  currentQuestionId,
}) => {
  const navigate = useNavigate({ from: routeApi.id });

  const { ref } = useSortable({ id, index });
  console.log("index", index);

  const handleSelectQuestion = (id: string) => {
    void navigate({
      search: (prev) => ({ ...prev, questionId: id }),
    });
  };

  return (
    <div className={styles.slideThumbnailWrapper} ref={ref}>
      <DropdownMenu
        position={"top-right"}
        trigger={(toggle) => (
          <div
            onContextMenu={(e) => {
              e.preventDefault();
              toggle();
            }}
            onClick={void handleSelectQuestion}
            className={`${styles.slideThumbnail} ${currentQuestionId == id && styles.active}`}>
            <div>
              {index} {name} {id} {type}
            </div>
          </div>
        )}>
        Menu goes here - New slide - Clone slide - Delete slide - Add comment
      </DropdownMenu>
      <div className={styles.slideIndex}> {index + 1} </div>
    </div>
  );
};

export { SlideThumbnail };
