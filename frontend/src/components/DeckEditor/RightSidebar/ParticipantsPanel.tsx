// Participants drawer for the deck-editor right sidebar. Roster + per-
// participant moderation actions are still TBD; for now this panel hosts
// the per-slide emoji-reactions opt-out, since reactions are an audience-
// facing behavior. The toggle reads/writes the active element via the
// same updateElement commit path used elsewhere in EditSlideSections, so
// edits stay in sync with the slide rail and live editor.
import { useState } from "react";
import { getRouteApi } from "@tanstack/react-router";
import { Toggle } from "@/components/Common/Input/Toggle/Toggle";
import { useCurrentUser } from "@/hooks/useCurrentUser";
import {
  useGetDeckQuery,
  useUpdateElementMutation,
  type DeckResponse,
} from "@/store/BrainFlexApi";
import styles from "./EditSlidePanel.module.css";

type DeckElement = NonNullable<DeckResponse["elements"]>[number];

const routeApi = getRouteApi("/decks/$deckId/edit");

const ParticipantsPanel = () => {
  const { deckId } = routeApi.useParams();
  const { questionId } = routeApi.useSearch();
  const currentUser = useCurrentUser();
  const currentUserId =
    currentUser.state === "registered" || currentUser.state === "guest"
      ? currentUser.user.id
      : undefined;

  const { element } = useGetDeckQuery(
    { id: deckId },
    {
      selectFromResult: ({ data }) => ({
        element: data?.elements?.find((e) => e.id === questionId),
      }),
    },
  );

  const [updateElement] = useUpdateElementMutation();

  const [reactionsEnabled, setReactionsEnabled] = useState<boolean>(
    element?.chrome?.reactionsEnabled ?? true,
  );
  const [syncedFromId, setSyncedFromId] = useState<string | undefined>(
    element?.id,
  );

  if (element && syncedFromId !== element.id) {
    setSyncedFromId(element.id);
    setReactionsEnabled(element.chrome?.reactionsEnabled ?? true);
  }

  if (!element) {
    return (
      <div className={styles.empty}>
        <p>Select a slide to manage audience reactions.</p>
      </div>
    );
  }

  const commit = (next: boolean) => {
    const stamped: DeckElement = {
      ...element,
      chrome: {
        ...element.chrome,
        reactionsEnabled: next,
        lastEditedByUserId: currentUserId,
        version: (element.chrome?.version ?? 0) + 1,
      },
    };
    void updateElement({
      id: deckId,
      elementId: element.id ?? "",
      body: stamped,
    })
      .unwrap()
      .catch((err: unknown) => {
        console.error("Failed to update reactions toggle", err);
      });
  };

  const elId = element.id ?? "";

  return (
    <div className={styles.panel}>
      <section className={styles.section}>
        <h4 className={styles.heading}>Audience reactions</h4>
        <Toggle
          id={`participants-reactions-${elId}`}
          label='Allow emoji reactions'
          checked={reactionsEnabled}
          onChange={(e) => {
            const next = e.currentTarget.checked;
            setReactionsEnabled(next);
            commit(next);
          }}
        />
      </section>
    </div>
  );
};

export { ParticipantsPanel };
