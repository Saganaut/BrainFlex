/**
 * Top-level layout for the deck editor at /decks/$deckId/view.
 *
 * Owns the three-column canvas (slide rail | active slide | inspector) and the
 * editor navbar. The navbar's title is an inline-editable input that patches the
 * deck name through `PUT /api/decks/{id}` on blur/Enter — no save button.
 */
import { useNavigate } from "@tanstack/react-router";

import { Btn } from "../Common/Buttons/Btn";
import { SplitBtn } from "../Common/Buttons/SplitBtn/SplitBtn";
import { DropdownMenuItem } from "../Menus/DropdownMenu";
import { LeftSidebarContent } from "./LeftSidebar/LeftSidebarContent";
import { PublishStatusControl } from "./PublishStatusControl";
import { RightSidebarContent } from "./RightSidebar/RightSidebarContent";
import { ScheduleSessionModal } from "./ScheduleSessionModal/ScheduleSessionModal";
import { ShareDeckModal } from "./ShareDeckModal/ShareDeckModal";
import { SlideDisplay } from "./SlideDisplay";
import { SpeakerNotesDrawer } from "./SpeakerNotesDrawer/SpeakerNotesDrawer";
import styles from "./DeckEditor.module.css";
import { useDeckEditor } from "./useDeckEditor";
import { useFullScreen } from "@/context/useFullScreen";
import { useModal } from "@/context/useModal";
import { useCurrentUser } from "@/hooks/useCurrentUser";
import { useStartInteractiveSession } from "@/hooks/useStartInteractiveSession";
import {
  ArrowsPointingOutIcon,
  PlayIcon,
  ShareIcon,
} from "@heroicons/react/24/outline";
import { MainBodyDashboard } from "../Layout/MainBodyDashboard";
import { CanvasHeader } from "../Layout/CanvasHeader";
import { CanvasBody } from "../Layout/CanvasBody";

import { InnerDisplay } from "../Layout/InnerDisplay";
import { useGetDeckQuery } from "@/store/BrainFlexApi";
import { Input } from "../Common/Input/Input/Input";

const DeckEditor = () => {
  const navigate = useNavigate();
  const { titleDraft, setTitleDraft, commitTitle, serverName, deckId } =
    useDeckEditor();
  const { toggleFullScreen } = useFullScreen();
  const { openModal, closeModal } = useModal();
  const userState = useCurrentUser();
  const callerUserId =
    userState.state === "registered" ? userState.user.id : undefined;
  const { data: deck } = useGetDeckQuery({ id: deckId });
  const callerIsOwner = deck?.myRole === "OWNER";
  const {
    quickStart,
    isStarting,
    error: startError,
  } = useStartInteractiveSession();

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

  const handleScheduleClick = () => {
    openModal({
      title: "Schedule session",
      content: <ScheduleSessionModal deckId={deckId} onClose={closeModal} />,
    });
  };

  return (
    <MainBodyDashboard className={styles.deckEditor}>
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
            </Btn>{" "}
            <Input
              ariaLabel='Deck title'
              value={titleDraft}
              placeholder='Untitled Deck'
              className={styles.titleDeck}
              maxLength={100}
              onChange={(e) => {
                setTitleDraft(e.target.value);
              }}
              isBordered={false}
              onBlur={commitTitle}
              onKeyDown={(e) => {
                if (e.key === "Enter") e.currentTarget.blur();
                if (e.key === "Escape") {
                  setTitleDraft(serverName);
                  e.currentTarget.blur();
                }
              }}
            />
          </div>

          <div className={styles.rightControlButtons}>
            <PublishStatusControl />
            <Btn size={"md"} shape={"pill"} onClick={handleShareClick}>
              <ShareIcon style={{ width: "1rem", height: "1rem" }} />
              Share
            </Btn>
            <SplitBtn
              size={"md"}
              shape={"pill"}
              variant={"brand"}
              disabled={isStarting}
              aria-describedby={
                startError ? "start-interactiveSession-error" : undefined
              }
              onClick={() => {
                void quickStart(deckId);
              }}
              menuAriaLabel='More start options'
              menuItems={
                <>
                  <DropdownMenuItem
                    onClick={() => {
                      // TODO: open the deck preview view (read-only renderer)
                      console.log("preview deck", serverName);
                    }}>
                    Preview
                  </DropdownMenuItem>
                  <DropdownMenuItem onClick={handleScheduleClick}>
                    Schedule
                  </DropdownMenuItem>
                </>
              }>
              <PlayIcon style={{ width: "1rem", height: "1rem" }} />
              {isStarting ? "Starting…" : "Start"}
            </SplitBtn>
            {startError && (
              <span
                id='start-interactiveSession-error'
                className={styles.startError}
                role='alert'>
                {startError}
              </span>
            )}
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

export { DeckEditor };
