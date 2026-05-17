/**
 * Vertical icon rail on the right edge of the deck editor with four toggleable
 * panels. Clicking an icon toggles its drawer; clicking a different icon
 * switches to that panel; clicking the active icon closes the drawer. The
 * drawer slides in to the LEFT of the rail (overlaying the slide canvas) so
 * the icon strip itself never moves.
 *
 * Panels are placeholder-only for now — content is "yet to be decided" per
 * spec. Replace the corresponding *Panel sub-components when we have copy.
 */
import { useState } from "react";
import { flushSync } from "react-dom";
import {
  PencilIcon,
  PaintBrushIcon,
  UsersIcon,
  ShareIcon,
} from "@heroicons/react/24/outline";
import { IconBtn } from "@/components/Common/Buttons/IconBtn";
import { ThemePanel } from "./ThemePanel";
import styles from "./RightSidebarContent.module.css";

type PanelKey = "edit" | "theme" | "participants" | "sharing";

const PANEL_TITLES: Record<PanelKey, string> = {
  edit: "Edit slide",
  theme: "Theme",
  participants: "Participants",
  sharing: "Sharing preferences",
};

const PlaceholderPanel = ({ description }: { description: string }) => (
  <div className={styles.placeholder}>
    <p>{description}</p>
    <p className={styles.placeholderMuted}>Coming soon.</p>
  </div>
);

const RightSidebarContent = () => {
  const [openPanel, setOpenPanel] = useState<PanelKey | null>(null);

  const setPanel = (next: PanelKey | null) => {
    const isSwap =
      openPanel !== null && next !== null && openPanel !== next;
    if (isSwap && typeof document.startViewTransition === "function") {
      document.startViewTransition(() => {
        // flushSync is required inside startViewTransition so React commits
        // the state change before the browser snapshots the "after" frame.
        // eslint-disable-next-line react-dom/no-flush-sync
        flushSync(() => {
          setOpenPanel(next);
        });
      });
    } else {
      setOpenPanel(next);
    }
  };

  const toggle = (key: PanelKey) => {
    setPanel(openPanel === key ? null : key);
  };

  return (
    <div className={styles.rightSidebarContent}>
      {openPanel !== null && (
        <aside className={styles.drawer} aria-label={PANEL_TITLES[openPanel]}>
          <div className={styles.panelContent} key={openPanel}>
            <div className={styles.drawerHeader}>
              <h3 className={styles.drawerTitle}>{PANEL_TITLES[openPanel]}</h3>
              <IconBtn
                type='close'
                size='sm'
                aria-label='Close panel'
                onClick={() => {
                  setPanel(null);
                }}
              />
            </div>
            <div className={styles.drawerBody}>
              {openPanel === "edit" && (
                <PlaceholderPanel description='Per-slide layout, animation, transition, and template options will live here.' />
              )}
              {openPanel === "theme" && <ThemePanel />}
              {openPanel === "participants" && (
                <PlaceholderPanel description='Roster of who has joined plus per-participant moderation actions.' />
              )}
              {openPanel === "sharing" && (
                <PlaceholderPanel description='Visibility, invite links, and per-org access controls for this deck.' />
              )}
            </div>
          </div>
        </aside>
      )}

      <div className={styles.iconStrip} role='toolbar' aria-label='Deck panels'>
        <IconBtn
          type='default'
          shape='round'
          bordered
          size='md'
          aria-label={PANEL_TITLES.edit}
          aria-pressed={openPanel === "edit"}
          className={openPanel === "edit" ? styles.active : undefined}
          icon={<PencilIcon />}
          onClick={() => {
            toggle("edit");
          }}
        />
        <IconBtn
          type='default'
          shape='round'
          bordered
          size='md'
          aria-label={PANEL_TITLES.theme}
          aria-pressed={openPanel === "theme"}
          className={openPanel === "theme" ? styles.active : undefined}
          icon={<PaintBrushIcon />}
          onClick={() => {
            toggle("theme");
          }}
        />
        <IconBtn
          type='default'
          shape='round'
          bordered
          size='md'
          aria-label={PANEL_TITLES.participants}
          aria-pressed={openPanel === "participants"}
          className={openPanel === "participants" ? styles.active : undefined}
          icon={<UsersIcon />}
          onClick={() => {
            toggle("participants");
          }}
        />
        <IconBtn
          type='default'
          shape='round'
          bordered
          size='md'
          aria-label={PANEL_TITLES.sharing}
          aria-pressed={openPanel === "sharing"}
          className={openPanel === "sharing" ? styles.active : undefined}
          icon={<ShareIcon />}
          onClick={() => {
            toggle("sharing");
          }}
        />
      </div>
    </div>
  );
};

export { RightSidebarContent };
