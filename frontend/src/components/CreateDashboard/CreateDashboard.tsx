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
import { RightSidebarContent } from "./RightSidebar/RightSidebarContent";
import { SlideDisplay } from "./SlideDisplay";
import { SpeakerNotesDrawer } from "./SpeakerNotesDrawer/SpeakerNotesDrawer";
import styles from "./CreateDashboard.module.css";
import { useCreateDashboard } from "./useCreateDashboard";
import { useFullScreen } from "@/context/useFullScreen";
import {
  ArrowsPointingOutIcon,
  EyeIcon,
  PlayIcon,
} from "@heroicons/react/24/outline";
import { MainBodyDashboard } from "../Layout/MainBodyDashboard";
import { CanvasHeader } from "../Layout/canvasHeader";
import { CanvasBody } from "../Layout/CanvasBody";
import { RightSidebar } from "../Layout/RightSidebar";

const CreateDashboard = () => {
  const navigate = useNavigate();
  const { titleDraft, setTitleDraft, commitTitle, serverName } =
    useCreateDashboard();
  const { toggleFullScreen } = useFullScreen();

  return (
    <MainBodyDashboard id='createDashboard'>
      <CanvasHeader>
        <div className={styles.navbar}>
          <div className={styles.leftControlButtons}>
            <Btn
              size={"md"}
              shape={"pill"}
              onClick={() => {
                void navigate({
                  to: "/decks",
                });
              }}>
              Back
            </Btn>
            <Btn
              shape={"pill"}
              size={"md"}
              aria-label='Enter fullscreen'
              onClick={toggleFullScreen}>
              <ArrowsPointingOutIcon
                style={{ width: "1rem", height: "1rem" }}
              />
            </Btn>
          </div>
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
          <div className={styles.rightControlButtons}>
            <Btn
              size={"md"}
              shape={"pill"}
              variant={"default"}
              onClick={() => {
                // TODO: open the deck preview view (read-only renderer)
                console.log("preview deck", serverName);
              }}>
              <EyeIcon style={{ width: "1rem", height: "1rem" }} />
              Preview
            </Btn>
            <Btn
              size={"md"}
              shape={"pill"}
              variant={"brand"}
              onClick={() => {
                // TODO: kick off a live showcase session for this deck
                console.log("start showcase", serverName);
              }}>
              <PlayIcon style={{ width: "1rem", height: "1rem" }} />
              Start
            </Btn>
            <DeckSettingsMenu />
          </div>
        </div>
      </CanvasHeader>
      <CanvasBody id='canvasBody'>
        <LeftSidebar />

        <div className={styles.slideCanvas}>
          <div className={styles.slideStack}>
            <SlideDisplay />
            <SpeakerNotesDrawer />
          </div>
        </div>

        <RightSidebar id='rightSidebar'>
          <RightSidebarContent />
        </RightSidebar>
      </CanvasBody>
    </MainBodyDashboard>
  );
};

export { CreateDashboard };
