// SessionChat — the floating chat/reaction widget anchored to the bottom-right
// corner of an interactive session. It is intentionally small: its primary job
// is letting players fire off quick emoji reactions during a live game, with an
// optional text composer for actual messages. This file is presentation-only —
// open/close and draft-text are local UI state; all data (messages, sending a
// reaction, sending a message) is delivered through props so the session layer
// can wire it to the live socket later.
import { useState } from "react";
import {
  ChatBubbleLeftRightIcon,
  PaperAirplaneIcon,
  XMarkIcon,
} from "@heroicons/react/24/outline";
import { IconBtn } from "@/components/Common/Buttons/IconBtn";
import styles from "./SessionChat.module.css";

interface ChatMessage {
  id: string;
  /** Display name of the sender. */
  author: string;
  /** Message text, or the emoji for a reaction message. */
  body: string;
  /** `reaction` renders a chrome-less emoji; defaults to `text`. */
  kind?: "text" | "reaction";
  /** Right-aligns the message as the current user's own. */
  isSelf?: boolean;
}

interface SessionChatProps {
  /** Newest-last list of messages to render in the scroll area. */
  messages?: ChatMessage[];
  /** Emoji offered in the quick-reaction row. */
  reactions?: string[];
  /** Show the text composer. Reactions are always available. */
  allowText?: boolean;
  /** Start expanded rather than collapsed to the launcher bubble. */
  defaultOpen?: boolean;
  /** Unread count shown on the launcher badge while collapsed. */
  unreadCount?: number;
  /** Heading shown in the panel. */
  title?: string;
  onReact?: (emoji: string) => void;
  onSendMessage?: (text: string) => void;
  className?: string;
}

const DEFAULT_REACTIONS = ["👍", "❤️", "😂", "🎉", "😮", "👏"];

const SessionChat = ({
  messages = [],
  reactions = DEFAULT_REACTIONS,
  allowText = true,
  defaultOpen = false,
  unreadCount = 0,
  title = "Chat",
  onReact,
  onSendMessage,
  className,
}: SessionChatProps) => {
  const [open, setOpen] = useState(defaultOpen);
  const [draft, setDraft] = useState("");

  const sendDraft = () => {
    const text = draft.trim();
    if (text === "") return;
    onSendMessage?.(text);
    setDraft("");
  };

  return (
    <div
      className={[styles.sessionChat, className].filter(Boolean).join(" ")}
      data-open={open}>
      {open ? (
        <section className={styles.panel} aria-label={`${title} panel`}>
          <header className={styles.header}>
            <h2 className={styles.title}>{title}</h2>
            <IconBtn
              fill='ghost'
              size='xs'
              icon={<XMarkIcon className={styles.chromeIcon} />}
              aria-label='Collapse chat'
              onClick={() => {
                setOpen(false);
              }}
            />
          </header>

          <ol className={styles.messages} aria-live='polite'>
            {messages.length === 0 ? (
              <li className={styles.empty}>No messages yet — say hi 👋</li>
            ) : (
              messages.map((message) => (
                <li
                  key={message.id}
                  className={styles.message}
                  data-self={message.isSelf === true}
                  data-kind={message.kind ?? "text"}>
                  <span className={styles.author}>{message.author}</span>
                  <span className={styles.bubble}>{message.body}</span>
                </li>
              ))
            )}
          </ol>

          <div className={styles.reactions} role='group' aria-label='Reactions'>
            {reactions.map((emoji) => (
              <button
                key={emoji}
                type='button'
                className={styles.reaction}
                aria-label={`React with ${emoji}`}
                onClick={() => {
                  onReact?.(emoji);
                }}>
                {emoji}
              </button>
            ))}
          </div>

          {allowText && (
            <form
              className={styles.composer}
              onSubmit={(event) => {
                event.preventDefault();
                sendDraft();
              }}>
              <input
                className={styles.input}
                type='text'
                value={draft}
                onChange={(event) => {
                  setDraft(event.target.value);
                }}
                placeholder='Send a message…'
                aria-label='Message'
                maxLength={280}
              />
              <IconBtn
                fill='ghost'
                size='sm'
                icon={<PaperAirplaneIcon className={styles.chromeIcon} />}
                aria-label='Send message'
                disabled={draft.trim() === ""}
                onClick={sendDraft}
              />
            </form>
          )}
        </section>
      ) : (
        <button
          type='button'
          className={styles.launcher}
          aria-label='Open chat'
          onClick={() => {
            setOpen(true);
          }}>
          <ChatBubbleLeftRightIcon className={styles.launcherIcon} />
          {unreadCount > 0 && (
            <span className={styles.badge} aria-hidden='true'>
              {unreadCount > 99 ? "99+" : unreadCount}
            </span>
          )}
        </button>
      )}
    </div>
  );
};

export { SessionChat };
export type { ChatMessage, SessionChatProps };
