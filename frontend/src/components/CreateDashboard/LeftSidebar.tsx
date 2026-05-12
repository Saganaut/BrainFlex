import type { DragEndEvent } from "@dnd-kit/dom";
import { isSortable } from "@dnd-kit/dom/sortable";
import { DragDropProvider } from "@dnd-kit/react";
import { getRouteApi, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { Btn } from "../Common/Buttons/Btn";
import { SlideThumbnail } from "./SlideThumbnail";
import styles from "./CreateDashboard.module.css";

const routeApi = getRouteApi("/decks/$deckId/view");

const LeftSidebar = () => {
  const { deckId } = routeApi.useParams();
  console.log(
    `DeckId ${deckId} Will be necessary here to send re-organization requests to backend`,
  );
  const { questionId } = routeApi.useSearch();
  const navigate = useNavigate({ from: routeApi.id });

  const handleSelectQuestion = (id: string) => {
    // This updates the URL search params without losing the deckId param
    void navigate({
      search: (prev) => ({ ...prev, questionId: id }),
    });
  };

  const SLIDES_DATA = [
    { name: "slide 1", id: "u98afnafu", type: "section" },
    { name: "slide 2", id: "u98afnafaf3535efeafefau", type: "MCQ" },
    { name: "slide 5", id: "u98afnafa2343535fefau", type: "MCQ" },
    { name: "slide 36", id: "u98afnaf5353a234fefau", type: "MCQ" },
    { name: "slide 37", id: "u98a6a234fefau", type: "MCQ" },

    { name: "slide 4", id: "u98afna4ddfeafeaafau", type: "MCQ" },
    { name: "slide 4", id: "u98afnaf4afefau", type: "MCQ" },
  ];
  const [slidesData, setSlidesData] = useState(SLIDES_DATA);

  const handleDragEnd = (event: DragEndEvent) => {
    const { source } = event.operation;
    if (!isSortable(source)) return;
    const { initialIndex, index } = source;
    if (initialIndex === index) return;

    setSlidesData((prev) => {
      const newSlides = [...prev];
      const [movedItem] = newSlides.splice(source.initialIndex, 1);
      newSlides.splice(source.index, 0, movedItem);
      return newSlides;
    });
  };
  //TODO: New slide btn
  // When clicked sends signal to server, modal pops up, choose what to include
  // Once done issue a uuid, send a post request to server putting it in deck
  // Use uuid so it can be instant
  return (
    <div className={styles.leftSidebar}>
      <div>
        <Btn>New Slide</Btn>
      </div>
      <div className={styles.slideContainer}>
        <DragDropProvider
          onDragEnd={(event) => {
            handleDragEnd(event);
          }}>
          {slidesData.map((slide, index) => (
            <SlideThumbnail
              key={slide.id}
              index={index}
              currentQuestionId={questionId}
              {...slide}
              onClick={() => {
                handleSelectQuestion(slide.id);
              }}
            />
          ))}
        </DragDropProvider>
      </div>
    </div>
  );
};

export { LeftSidebar };
