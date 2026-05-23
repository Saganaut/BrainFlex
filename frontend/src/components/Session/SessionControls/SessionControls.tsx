// Host-only admin control bar for a live session. Lives below the SessionBoard
// in the InnerDisplay and drives the round state machine over STOMP: end the
// submit window (flushing players' drafts), reveal results, advance, pause /
// resume the countdown, end the show, or restart from round 1. Non-hosts render
// nothing — moderation/control is the host's surface only (the board itself is
// the shared display + answer surface).
//
// State (status / phase / current element / settings) is read from useSession()
// for parity with SessionBoard; the host actions are sent through the session
// connection (one shared STOMP client, provided by SessionConnectionProvider).
// Live overlays (paused, revealed) come from the Redux slice, which the STOMP
// subscriptions keep in sync.
import { useSession } from "@/pages/SessionPage/useSession";
import { useSessionConnection } from "@/pages/SessionPage/SessionConnectionContext";
import { useConfirm } from "@/components/Common/ConfirmDialog/useConfirm";
import { useAppSelector } from "@/store/hooks";
import { resolveShowResponsesFor } from "@/utils/showResponsesResolver";
import { Btn } from "@/components/Common/Buttons/Btn";
import styles from "./SessionControls.module.css";

interface SessionControlsProps {
  className?: string;
}

const SessionControls = ({ className }: SessionControlsProps) => {
  const { interactiveSession } = useSession();
  const {
    status,
    phase,
    currentRound,
    deckSnapshot,
    settings,
    players,
    viewerPlayerId,
    hostPlayerId,
  } = interactiveSession;

  const {
    sendStart,
    sendEndSubmitPhase,
    sendRevealNow,
    sendNextRound,
    sendPauseTimer,
    sendResumeTimer,
    sendEndInteractiveSession,
    sendRestart,
  } = useSessionConnection();
  const confirm = useConfirm();

  // Live overlays from the slice. Once useSession is wired to Redux these track
  // the real session; until then they read the slice defaults (harmless).
  const timerPaused = useAppSelector((s) => s.interactiveSession.timerPaused);
  const revealedElementIds = useAppSelector(
    (s) => s.interactiveSession.revealedElementIds,
  );

  // Controls are the host's surface only.
  const viewerIsHost = !!viewerPlayerId && viewerPlayerId === hostPlayerId;
  if (!viewerIsHost) return null;

  // Pre-game: the only host action is to start. Starting flips the session to
  // IN_PROGRESS (via the /round broadcast), and the board re-derives its stage
  // to the first question — no navigation, the lobby is just a board stage.
  if (status === "LOBBY") {
    return (
      <div className={`${styles.sessionControls} ${className ?? ""}`}>
        <div className={styles.actions}>
          <Btn
            size='sm'
            variant='brand'
            disabled={players.length < 1}
            onClick={sendStart}>
            Start session
          </Btn>
        </div>
      </div>
    );
  }

  // currentRound can point past the end on terminal states; treat as optional.
  const element = deckSnapshot[currentRound] as
    | (typeof deckSnapshot)[number]
    | undefined;
  const elementId = element?.id ?? "";
  const isSlide = element?.kind === "Slide";
  const inProgress = status === "IN_PROGRESS";
  const inSubmit = phase === "SUBMIT";
  const isTurnBased = settings.answerSubmissionMode === "TURN_BASED";
  const isRevealed = revealedElementIds.includes(elementId);

  // Reveal-now only applies on a live ON_CLICK question that hasn't been
  // revealed yet (mirrors PlayPage's HostRoundControls.canReveal). The
  // per-element showResponses override is Slide-only and reveal never targets a
  // slide, so the element arg is left undefined and the cascade falls through to
  // deck / session / format.
  const resolved = resolveShowResponsesFor(interactiveSession, undefined, undefined);
  const canReveal =
    inProgress && inSubmit && !isSlide && resolved === "ON_CLICK" && !isRevealed;
  const canEndSubmit = inProgress && inSubmit && !isSlide;
  // Pause only matters when the round has a countdown. We key off the session's
  // per-question time; per-element overrides are a rarer case and the button is
  // hidden, not broken, when they're the only timer.
  const hasTimer = (settings.timePerQuestion ?? 0) > 0;
  const canPauseTimer = inProgress && inSubmit && !isSlide && hasTimer;
  const canNextRound = inProgress && isTurnBased;
  const canEnd = inProgress;
  const canRestart = inProgress || status === "FINISHED";

  const handleEnd = async () => {
    const ok = await confirm({
      title: "End session",
      message: "End the session now? Scores so far will be final.",
      confirmLabel: "End session",
      variant: "danger",
    });
    if (ok) sendEndInteractiveSession();
  };

  const handleRestart = async () => {
    const ok = await confirm({
      title: "Restart session",
      message:
        "Restart from round 1? All scores and answers will be cleared — players stay in the room.",
      confirmLabel: "Restart",
      variant: "danger",
    });
    if (ok) sendRestart();
  };

  return (
    <div className={`${styles.sessionControls} ${className ?? ""}`}>
      <div className={styles.actions}>
        <Btn
          size='sm'
          disabled={!canEndSubmit}
          onClick={() => {
            sendEndSubmitPhase(elementId);
          }}>
          End submit phase
        </Btn>
        <Btn
          size='sm'
          disabled={!canReveal}
          onClick={() => {
            sendRevealNow(elementId);
          }}>
          {isRevealed ? "Results revealed" : "Reveal results"}
        </Btn>
        {canNextRound && (
          <Btn size='sm' onClick={sendNextRound}>
            Next round
          </Btn>
        )}
        <Btn
          size='sm'
          variant={timerPaused ? "warning" : undefined}
          disabled={!canPauseTimer}
          onClick={() => {
            if (timerPaused) sendResumeTimer();
            else sendPauseTimer();
          }}>
          {timerPaused ? "Resume timer" : "Pause timer"}
        </Btn>

        <span className={styles.spacer} />

        <Btn
          size='sm'
          variant='error'
          fill='ghost'
          disabled={!canEnd}
          onClick={() => {
            void handleEnd();
          }}>
          End session
        </Btn>
        <Btn
          size='sm'
          variant='error'
          disabled={!canRestart}
          onClick={() => {
            void handleRestart();
          }}>
          Restart
        </Btn>
      </div>
    </div>
  );
};

export { SessionControls };
