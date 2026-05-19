/**
 * Top-level layout for the deck editor at /decks/$deckId/view.
 *
 * Owns the three-column canvas (slide rail | active slide | inspector) and the
 * editor navbar. The navbar's title is an inline-editable input that patches the
 * deck name through `PUT /api/decks/{id}` on blur/Enter — no save button.
 */
import { useNavigate } from "@tanstack/react-router";

import { Btn } from "../Common/Buttons/Btn";
import { LeftSidebarContent } from "./LeftSidebar/LeftSidebarContent";
import { PublishStatusControl } from "./PublishStatusControl";
import { RightSidebarContent } from "./RightSidebar/RightSidebarContent";
import { ShareDeckModal } from "./ShareDeckModal/ShareDeckModal";
import { SlideDisplay } from "./SlideDisplay";
import { SpeakerNotesDrawer } from "./SpeakerNotesDrawer/SpeakerNotesDrawer";
import styles from "./CreateDashboard.module.css";
import { useCreateDashboard } from "./useCreateDashboard";
import { useFullScreen } from "@/context/useFullScreen";
import { useModal } from "@/context/useModal";
import { useCurrentUser } from "@/hooks/useCurrentUser";
import {
  ArrowsPointingOutIcon,
  EyeIcon,
  PlayIcon,
  ShareIcon,
} from "@heroicons/react/24/outline";
import { MainBodyDashboard } from "../Layout/MainBodyDashboard";
import { CanvasHeader } from "../Layout/CanvasHeader";
import { CanvasBody } from "../Layout/CanvasBody";

import { InnerDisplay } from "../Layout/InnerDisplay";
import { useGetDeckQuery } from "@/store/BrainFlexApi";

const CreateDashboard = () => {
  const navigate = useNavigate();
  const { titleDraft, setTitleDraft, commitTitle, serverName, deckId } =
    useCreateDashboard();
  const { toggleFullScreen } = useFullScreen();
  const { openModal, closeModal } = useModal();
  const userState = useCurrentUser();
  const callerUserId =
    userState.state === "registered" ? userState.user.id : undefined;
  const { data: deck } = useGetDeckQuery({ id: deckId });
  const callerIsOwner = deck?.myRole === "OWNER";

  const handleShareClick = () => {
    openModal({
      title: "Share deck",
      content: (
        <ShareDeckModal
          deckId={deckId}
          callerUserId={callerUserId}
          callerIsOwner={callerIsOwner}
          onClose={closeModal}
        />
      ),
    });
  };

  return (
    <MainBodyDashboard className={styles.createDashboard}>
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
            <PublishStatusControl />
            <Btn
              size={"md"}
              shape={"pill"}
              variant={"default"}
              onClick={handleShareClick}>
              <ShareIcon style={{ width: "1rem", height: "1rem" }} />
              Share
            </Btn>
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
          </div>
        </div>
      </CanvasHeader>
      <CanvasBody>
        <LeftSidebarContent />
        <InnerDisplay className={styles.slideCanvasContainer}>
          <SlideDisplay />
          <SpeakerNotesDrawer />
        </InnerDisplay>
        <RightSidebarContent />
      </CanvasBody>
    </MainBodyDashboard>
  );
};

export { CreateDashboard };
