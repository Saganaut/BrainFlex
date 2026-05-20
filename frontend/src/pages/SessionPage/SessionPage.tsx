import { MainBodyDashboard } from "@/components/Layout/MainBodyDashboard";
import { useSession } from "./useSession";
import { CanvasHeader } from "@/components/Layout/CanvasHeader";
import { CanvasBody } from "@/components/Layout/CanvasBody";
import { InnerDisplay } from "@/components/Layout/InnerDisplay";
import { LeftSidebar } from "@/components/Layout/LeftSidebar";
import { RightSidebar } from "@/components/Layout/RightSidebar";

const SessionPage = () => {
  const { sessionId } = useSession();

  return (
    <MainBodyDashboard className={""}>
      <CanvasHeader>
        <div>Header {sessionId}</div>
      </CanvasHeader>
      <CanvasBody>
        <LeftSidebar>A progress tracker </LeftSidebar>
        <InnerDisplay>
          {" "}
          {sessionId}
          {/* 
            Main presentation is one row

            Bottom row is controls */}
        </InnerDisplay>
        <RightSidebar>Players, leaderboards, chat, emojis</RightSidebar>
      </CanvasBody>
    </MainBodyDashboard>
  );
};

export { SessionPage };
