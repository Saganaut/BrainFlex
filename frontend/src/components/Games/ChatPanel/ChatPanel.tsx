/**
 * Audience chat sidebar (chunk 11).
 *
 * Reads the chat history once on mount via useListChatQuery, hands it to the
 * slice via chatHistoryLoaded, and from then on renders exclusively from
 * state.interactiveSession.chat — every STOMP /chat broadcast (new sends AND
 * moderation flips) flows into that array, so the panel is always live.
 *
 * Send uses useSendChatMutation with the optimistic patch wired up in
 * apiEnhancements: the message appears immediately and is reconciled when
 * the server echoes back via STOMP. Host viewers also see a "Hide" affordance
 * on every non-own message, which calls useModerateChatMutation; the server
 * rebroadcasts the row with moderated=true so non-hosts replace their copy
 * with the "(hidden by host)" placeholder.
 *
 * The panel is collapsible — closed by default on small viewports — so it
 * doesn't compete with the play area when the audience isn't actively
 * chatting.
 */
import { useEffect, useRef, useState } from "react";
import { Btn } from "@/components/Common/Buttons/Btn";
import { Input } from "@/components/Common/Input/Input/Input";
import {
  useListChatQuery,
  useSendChatMutation,
  useModerateChatMutation,
  type InteractiveSessionChatMessageResponse,
} from "../../../store/BrainFlexApi";
import { useAppDispatch, useAppSelector } from "../../../store/hooks";
import { chatHistoryLoaded } from "../../../store/interactiveSessionSlice";
import styles from "./ChatPanel.module.css";

interface ChatPanelProps {
  roomCode: string;
  isHost: boolean;
  currentUserId?: string;
  /** Pass false when the host has disabled chat in InteractiveSessionSettings. */
  enabled?: boolean;
}

const CHAT_PAGE_SIZE = 50;

const ChatPanel = ({
  roomCode,
  isHost,
  currentUserId,
  enabled = true,
}: ChatPanelProps) => {
  const dispatch = useAppDispatch();
  const messages = useAppSelector((s) => s.interactiveSession.chat);
  const [open, setOpen] = useState(true);
  const [draft, setDraft] = useState("");
  const [sendChat, { isLoading: sending }] = useSendChatMutation();
  const [moderateChat] = useModerateChatMutation();
  const { data: history } = useListChatQuery(
    { roomCode, page: 0, size: CHAT_PAGE_SIZE },
    { skip: !enabled },
  );
  const listEndRef = useRef<HTMLDivElement>(null);

  // Seed the slice once with the server's authoritative replay; from then on
  // STOMP /chat broadcasts in the slice are the source of truth.
  const seededRef = useRef(false);
  useEffect(() => {
    if (!seededRef.current && history) {
      dispatch(chatHistoryLoaded(history));
      seededRef.current = true;
    }
  }, [history, dispatch]);

  // Auto-scroll to bottom on new message. Skip when the user has scrolled up
  // far enough that they're clearly reading history — measured by checking
  // distance from the bottom before the new row appended.
  useEffect(() => {
    listEndRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [messages.length]);

  if (!enabled) return null;

  const submit = async (e: React.SyntheticEvent<HTMLFormElement>) => {
    e.preventDefault();
    const body = draft.trim();
    if (!body) return;
    setDraft("");
    try {
      await sendChat({ roomCode, chatSendRequest: { body } }).unwrap();
    } catch {
      // Optimistic patch in apiEnhancements rolled back on reject; nothing
      // to do here. The Input is already cleared — a retry banner would be
      // overkill for a chat send.
    }
  };

  const handleHide = (id: string | undefined) => {
    if (!id) return;
    void moderateChat({ roomCode, messageId: id });
  };

  return (
    <aside
      className={`${styles.panel} ${open ? styles.open : styles.closed}`}
      aria-label='Audience chat'>
      <Btn
        variant='secondary'
        fill='ghost'
        size='sm'
        className={styles.toggleBtn}
        onClick={() => {
          setOpen((v) => !v);
        }}
        aria-expanded={open}>
        Chat {messages.length > 0 ? `· ${messages.length}` : ""}
        <span className={styles.toggleChevron} aria-hidden='true'>
          {open ? "▾" : "▴"}
        </span>
      </Btn>
      {open && (
        <>
          <ol className={styles.list}>
            {messages.length === 0 && (
              <li className={styles.empty}>No messages yet.</li>
            )}
            {messages.map((m) => (
              <ChatRow
                key={m.id ?? `${m.author?.userId}-${m.sentAt}`}
                message={m}
                isHost={isHost}
                isOwn={!!currentUserId && m.author?.userId === currentUserId}
                onHide={() => {
                  handleHide(m.id);
                }}
              />
            ))}
            <div ref={listEndRef} />
          </ol>
          <form
            className={styles.composer}
            onSubmit={(e) => {
              void submit(e);
            }}>
            <Input
              type='text'
              value={draft}
              maxLength={500}
              ariaLabel='Chat message'
              placeholder='Say something…'
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                setDraft(e.target.value);
              }}
              className={styles.input}
            />
            <Btn
              type='submit'
              size='sm'
              disabled={sending || draft.trim().length === 0}>
              Send
            </Btn>
          </form>
        </>
      )}
    </aside>
  );
};

interface ChatRowProps {
  message: InteractiveSessionChatMessageResponse;
  isHost: boolean;
  isOwn: boolean;
  onHide: () => void;
}

const ChatRow = ({ message, isHost, isOwn, onHide }: ChatRowProps) => {
  // For non-hosts, a moderated message collapses to the placeholder. Hosts
  // still see the original text crossed out so they can recover the context
  // of what they hid (matches Mentimeter's moderation UX).
  const isModerated = !!message.moderated;
  const showOriginal = !isModerated || isHost;
  const fromHost = !!message.fromHost;
  return (
    <li
      className={`${styles.row} ${fromHost ? styles.rowHost : ""} ${isOwn ? styles.rowOwn : ""} ${isModerated ? styles.rowModerated : ""}`}>
      <span className={styles.author}>
        {message.author?.name ?? "anon"}
        {fromHost && <span className={styles.hostTag}>host</span>}
        {!fromHost && message.author?.guest && (
          <span className={styles.guestTag}>guest</span>
        )}
      </span>
      <span className={styles.body}>
        {showOriginal ? message.body : "(hidden by host)"}
      </span>
      {isHost && !isModerated && !fromHost && message.id && (
        <Btn
          variant='error'
          fill='ghost'
          size='xs'
          className={styles.hideBtn}
          onClick={onHide}
          aria-label='Hide this message'>
          Hide
        </Btn>
      )}
    </li>
  );
};

export { ChatPanel };
