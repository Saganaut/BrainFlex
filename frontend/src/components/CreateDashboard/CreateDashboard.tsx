import { Btn } from "../Common/Buttons/Btn";

import styles from "./CreateDashboard.module.css";

import { getRouteApi } from "@tanstack/react-router";
import { DeckSettingsMenu } from "./DeckSettingsMenu";
import { LeftSidebar } from "./LeftSidebar";
import { SlideDisplay } from "./SlideDisplay";

const routeApi = getRouteApi("/decks/$deckId/view");

const CreateDashboard = () => {
  const { deckId } = routeApi.useParams();
  // const { questionId } = routeApi.useSearch();

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
        {/* Sidebar Left - displays all questions/slides*/}
        <LeftSidebar />
        {/* Editable slide/*/}

        <div className={styles.slideCanvas}>
          <SlideDisplay />
        </div>
        {/* Sidebar Right */}

        <div className={styles.rightSidebar}>Right sidebar</div>
      </div>
    </div>
  );
};

export { CreateDashboard };
