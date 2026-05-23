import type { DeckElement } from "@/types/elements";
import styles from "./SessionRoundTracker.module.css";
import { SlideTypeGraphicSvg } from "@/components/Common/Slides/SlideTypeGraphics/SlideTypeGraphic";

// eslint-disable-next-line unused-imports/no-unused-vars
const PHASES = ["SUBMIT", "VOTE", "REVEAL"] as const;
type Phase = (typeof PHASES)[number];

interface SessionRoundThumbnailInterface {
  className?: string;
  roundPhase?: Phase;
  element: DeckElement;
  isActive: boolean;
}

const phaseClasses: Record<Phase, string> = {
  SUBMIT: "submit",
  VOTE: "vote",
  REVEAL: "reveal",
};

const SessionRoundThumbnail = ({
  className,
  roundPhase,
  element,
}: SessionRoundThumbnailInterface) => {
  console.log("element", element);
  const phaseKey = roundPhase ? phaseClasses[roundPhase] : "inactive";
  return (
    <div
      className={`${styles.sessionRoundThumbnail} ${className ?? ""} ${styles[phaseKey] ?? ""}`}>
      <SlideTypeGraphicSvg kind={element.kind} />
    </div>
  );
};

export { SessionRoundThumbnail };
