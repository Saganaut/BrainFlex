/**
 * Player-facing emoji reaction bar (chunk 11).
 *
 * Six default emojis on a fixed footer row; tap to broadcast via the
 * useSendReactionMutation REST endpoint, which the server then fans out on
 * /topic/interactive-session/{roomCode}/reaction. The host renders the
 * resulting bursts in ReactionRain.
 *
 * Long-press for a richer emoji picker is intentionally deferred — six
 * defaults cover the common Mentimeter/Kahoot reactions without a picker
 * sheet that would require its own focus + accessibility pass. A simple
 * client-side cooldown debounces rapid taps so a single player can't carpet
 * the host view (the server enforces its own rate limit too).
 */
import { useCallback, useRef } from "react";
import { useSendReactionMutation } from "../../../store/BrainFlexApi";
import styles from "./ReactionBar.module.css";

interface ReactionBarProps {
  roomCode: string;
  disabled?: boolean;
}

const DEFAULT_EMOJIS = ["👍", "❤️", "😂", "😮", "😢", "👏"] as const;
const COOLDOWN_MS = 500;

const ReactionBar = ({ roomCode, disabled }: ReactionBarProps) => {
  const [sendReaction] = useSendReactionMutation();
  const lastSentAtRef = useRef<number>(0);

  const send = useCallback(
    (emoji: string) => {
      const now = Date.now();
      if (now - lastSentAtRef.current < COOLDOWN_MS) return;
      lastSentAtRef.current = now;
      void sendReaction({
        roomCode,
        reactionSendRequest: { emoji },
      }).unwrap().catch(() => {
        // Server may rate-limit; nothing user-actionable. Reset the cooldown
        // so a follow-up tap isn't silently swallowed.
        lastSentAtRef.current = 0;
      });
    },
    [roomCode, sendReaction],
  );

  return (
    <div
      className={styles.bar}
      role='group'
      aria-label='React'
      aria-disabled={disabled ? true : undefined}>
      {DEFAULT_EMOJIS.map((emoji) => (
        <button
          key={emoji}
          type='button'
          className={styles.btn}
          aria-label={`Send ${emoji} reaction`}
          disabled={disabled}
          onClick={() => {
            send(emoji);
          }}>
          <span aria-hidden='true'>{emoji}</span>
        </button>
      ))}
    </div>
  );
};

export { ReactionBar };
