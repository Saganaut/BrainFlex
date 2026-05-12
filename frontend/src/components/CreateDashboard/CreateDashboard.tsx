/**
 * Top-level layout for the deck editor at /decks/$deckId/view.
 *
 * Owns the three-column canvas (slide rail | active slide | inspector) and the
 * editor navbar. The navbar's title is an inline-editable input that patches the
 * deck name through `PUT /api/decks/{id}` on blur/Enter — no save button.
 */
import { useNavigate } from "@tanstack/react-router";

import { Btn } from "../Common/Buttons/Btn";
import { DeckSettingsMenu } from "./DeckSettingsMenu";
import { LeftSidebar } from "./LeftSidebar";
import { SlideDisplay } from "./SlideDisplay";
import styles from "./CreateDashboard.module.css";
import { useCreateDashboard } from "./useCreateDashboard";

const CreateDashboard = () => {
  const navigate = useNavigate();

  const { titleDraft, setTitleDraft, commitTitle, serverName } =
    useCreateDashboard();

  return (
    <div className={styles.createDashboard}>
      <div className={styles.navbar}>
        <Btn
          onClick={() => {
            void navigate({ to: "/my-packs" });
          }}>
          Back
        </Btn>

        <input
          aria-label='Deck title'
          value={titleDraft}
          placeholder='Untitled Deck'
          maxLength={100}
          onChange={(e) => {
            setTitleDraft(e.target.value);
          }}
          onBlur={commitTitle}
          onKeyDown={(e) => {
            if (e.key === "Enter") e.currentTarget.blur();
            if (e.key === "Escape") {
              setTitleDraft(serverName);
              e.currentTarget.blur();
            }
          }}
        />
        <DeckSettingsMenu />
      </div>

      <div className={styles.mainCanvas}>
        <LeftSidebar />

        <div className={styles.slideCanvas}>
          <SlideDisplay />
        </div>

        <div className={styles.rightSidebar}>Right sidebar</div>
      </div>
    </div>
  );
};

export { CreateDashboard };
