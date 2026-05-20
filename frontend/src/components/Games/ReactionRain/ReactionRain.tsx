/**
 * Host-facing animated reaction overlay (chunk 11).
 *
 * Subscribes to {@code state.interactiveSession.liveReactions} and renders
 * each new burst as a single emoji that drifts upward, fades, and is then
 * popped off the rolling window via reactionConsumed. Animation is pure CSS
 * keyframes — no per-frame RAF state — so adding 30 simultaneous reactions
 * doesn't pin the main thread.
 *
 * Mounted absolutely at the bottom of PlayPage on the host view only. The
 * overlay is pointer-events: none so reactions can't intercept clicks on the
 * underlying scoreboard / next-round button.
 */
import { useEffect, useMemo } from "react";
import { useAppDispatch, useAppSelector } from "../../../store/hooks";
import {
  reactionConsumed,
  type LiveReaction,
} from "../../../store/interactiveSessionSlice";
import styles from "./ReactionRain.module.css";

/** Total animation duration in ms. Kept in sync with the CSS keyframes. */
const ANIM_MS = 3000;

const ReactionRain = () => {
  const dispatch = useAppDispatch();
  const liveReactions = useAppSelector(
    (s) => s.interactiveSession.liveReactions,
  );

  // Derive a stable lateral offset per reaction so a salvo of identical
  // emojis doesn't stack on top of itself. Hashing the id keeps the offset
  // deterministic across re-renders (no useState dance needed).
  const positioned = useMemo(
    () =>
      liveReactions.map((r) => ({
        ...r,
        offsetPct: hashToPct(r.id),
      })),
    [liveReactions],
  );

  // After each reaction's animation completes, drop it from the slice so the
  // DOM node unmounts and the rolling window doesn't bloat over a long game.
  useEffect(() => {
    if (liveReactions.length === 0) return;
    const timers = liveReactions.map((r) =>
      window.setTimeout(() => {
        dispatch(reactionConsumed(r.id));
      }, ANIM_MS),
    );
    return () => {
      for (const t of timers) clearTimeout(t);
    };
  }, [liveReactions, dispatch]);

  return (
    <div className={styles.layer} aria-hidden='true'>
      {positioned.map((r) => (
        <ReactionBurst key={r.id} reaction={r} offsetPct={r.offsetPct} />
      ))}
    </div>
  );
};

interface BurstProps {
  reaction: LiveReaction;
  offsetPct: number;
}

const ReactionBurst = ({ reaction, offsetPct }: BurstProps) => (
  <span className={styles.burst} style={{ left: `${offsetPct}%` }}>
    {reaction.emoji}
  </span>
);

// Deterministic 0–90 fractional position from the reaction id; keeps each
// emoji clear of the right edge while staying spread across the column.
const hashToPct = (id: string): number => {
  let h = 0;
  for (let i = 0; i < id.length; i++) h = (h * 31 + id.charCodeAt(i)) | 0;
  return Math.abs(h) % 90;
};

export { ReactionRain };
