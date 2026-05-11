import { Btn } from "../Common/Buttons/Btn";

import styles from "./CreateDashboard.module.css";

import { getRouteApi, useNavigate } from "@tanstack/react-router";
import { DeckSettingsMenu } from "./DeckSettingsMenu";
import { SlideThumbnail } from "./SlideThumbnail";
import { DragDropProvider, type DragEndEvent } from "@dnd-kit/react";
import { useState } from "react";
import { Sortable } from "@dnd-kit/dom/sortable";

const routeApi = getRouteApi("/decks/$deckId/view");

const CreateDashboard = () => {
  const { deckId } = routeApi.useParams();
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
    { name: "slide 2", id: "u98afnafafefeafefau", type: "MCQ" },
    { name: "slide 3", id: "u98afnafa234fefau", type: "MCQ" },

    { name: "slide 4", id: "u98afna4ddfeafeaafau", type: "MCQ" },
    { name: "slide 4", id: "u98afnaf4afefau", type: "MCQ" },
  ];
  const [slidesData, setSlidesData] = useState(SLIDES_DATA);
  const handleDragEnd = (event: DragEndEvent) => {
    const { source, target } = event.operation;

    // Ensure we actually dropped it over something
    if (source == null) return;
    if (target && source.index !== source.initialIndex) {
      setSlidesData((prev) => {
        const newSlides = [...prev];
        const [movedItem] = newSlides.splice(source.initialIndex, 1);
        newSlides.splice(source.index, 0, movedItem);
        return newSlides;
      });
    }
  };

  return (
    <div className={styles.createDashboard}>
      {/* Dashboard nav and control bar 
        Deck
        
      */}

      <div className={styles.navbar}>
        <Btn>Back</Btn>

        <h2>Navbar Deck Id {deckId}</h2>
        <DeckSettingsMenu />
      </div>
      {/* Canvas */}

      <div className={styles.mainCanvas}>
        {/* Sidebar right - displays all questions/slides*/}
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
                  {...slide}
                  onClick={() => {
                    handleSelectQuestion(slide.id);
                  }}
                />
              ))}
            </DragDropProvider>
          </div>
        </div>
        {/* Editable slide/*/}

        <div className={styles.slideCanvas}>
          {questionId}
          Slides
        </div>
        {/* Sidebar Left */}

        <div className={styles.rightSidebar}>Left sidebar</div>
      </div>
    </div>
  );
};

export { CreateDashboard };
