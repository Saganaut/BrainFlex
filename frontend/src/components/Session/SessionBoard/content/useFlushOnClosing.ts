// Chunk 25 — board-content hook for the host "end submit phase" action.
//
// When the host ends the submit window, the server broadcasts /submissionsClosing
// and the slice raises a one-shot `submissionsClosing` signal (a bumped nonce
// scoped to an elementId). Each participant device that is rendering the active
// question watches that signal and, if it holds a typed-but-unsubmitted draft,
// flushes it through the normal answer path before the round freezes — then
// clears the signal so it fires exactly once.
//
// NOTE: the board still runs on mock data and the content components are not yet
// wired to `sendAnswer`, so `flush` is a no-op for now in practice. This hook is
// the seam: it activates automatically once useSession is wired to live Redux
// and the content components pass a real submit in `flush`.
import { useEffect, useRef } from "react";
import { useAppDispatch, useAppSelector } from "@/store/hooks";
import { submissionsClosingConsumed } from "@/store/interactiveSessionSlice";

/**
 * Run `flush` once when the host closes the submit phase for `elementId`.
 * `flush` should submit the current draft if there is one (and is free to no-op
 * when there's nothing to send). The signal is consumed after flushing so a
 * single end-submit can't double-fire.
 */
export function useFlushOnClosing(
  elementId: string | undefined,
  flush: () => void,
): void {
  const dispatch = useAppDispatch();
  const closing = useAppSelector(
    (s) => s.interactiveSession.submissionsClosing,
  );
  // Latest-ref so `nonce` is the only effect trigger — we want the current
  // draft at fire time without re-running when the flush closure changes.
  const flushRef = useRef(flush);
  useEffect(() => {
    flushRef.current = flush;
  });

  // Only react when the close targets this element; the nonce is the trigger.
  const nonce =
    closing && elementId && closing.elementId === elementId
      ? closing.nonce
      : null;

  useEffect(() => {
    if (nonce == null) return;
    flushRef.current();
    dispatch(submissionsClosingConsumed());
  }, [nonce, dispatch]);
}
