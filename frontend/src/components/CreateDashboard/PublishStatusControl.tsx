// Publish/Unpublish/Republish button + status pill for the deck editor
// navbar. Pulls publishStatus from the cached deck (cache-sync in
// apiEnhancements keeps the pill fresh after any of the lifecycle
// mutations resolve) and dispatches the matching mutation on click.
import { getRouteApi } from "@tanstack/react-router";
import { useState } from "react";
import {
  useArchiveDeckMutation,
  useGetDeckQuery,
  usePublishDeckMutation,
  useUnpublishDeckMutation,
} from "@/store/BrainFlexApi";
import { Btn } from "../Common/Buttons/Btn";
import { Badge } from "../Common/Badge";
import styles from "./PublishStatusControl.module.css";

const routeApi = getRouteApi("/decks/$deckId/edit");

const STATUS_LABEL = {
  DRAFT: "Draft",
  PUBLISHED: "Published",
  ARCHIVED: "Archived",
} as const;

const STATUS_BADGE_VARIANT = {
  DRAFT: "info",
  PUBLISHED: "success",
  ARCHIVED: "warning",
} as const;

const PublishStatusControl = () => {
  const { deckId } = routeApi.useParams();
  const { data: deck } = useGetDeckQuery({ id: deckId });
  const [publishDeck, publishState] = usePublishDeckMutation();
  const [unpublishDeck, unpublishState] = useUnpublishDeckMutation();
  const [archiveDeck, archiveState] = useArchiveDeckMutation();
  const [archiveOpen, setArchiveOpen] = useState(false);

  // Default to DRAFT so the button renders something useful while the deck
  // is loading or for legacy decks that pre-date publishStatus.
  const status = deck?.publishStatus ?? "DRAFT";
  const busy =
    publishState.isLoading ||
    unpublishState.isLoading ||
    archiveState.isLoading;

  const handlePublish = () => {
    void publishDeck({ id: deckId }).unwrap().catch((err: unknown) => {
      console.error("Failed to publish deck", err);
    });
  };
  const handleUnpublish = () => {
    void unpublishDeck({ id: deckId }).unwrap().catch((err: unknown) => {
      console.error("Failed to unpublish deck", err);
    });
  };
  const handleArchive = () => {
    setArchiveOpen(false);
    void archiveDeck({ id: deckId }).unwrap().catch((err: unknown) => {
      console.error("Failed to archive deck", err);
    });
  };

  return (
    <div className={styles.control}>
      <Badge
        variant={STATUS_BADGE_VARIANT[status]}
        label={STATUS_LABEL[status]}
      />
      {status === "DRAFT" && (
        <Btn
          size='md'
          shape='pill'
          variant='default'
          disabled={busy}
          onClick={handlePublish}>
          Publish
        </Btn>
      )}
      {status === "PUBLISHED" && (
        <>
          <Btn
            size='md'
            shape='pill'
            variant='default'
            disabled={busy}
            onClick={handleUnpublish}>
            Unpublish
          </Btn>
          <div className={styles.archiveSlot}>
            <Btn
              size='md'
              shape='pill'
              variant='default'
              aria-haspopup='menu'
              aria-expanded={archiveOpen}
              disabled={busy}
              onClick={() => {
                setArchiveOpen((open) => !open);
              }}>
              ⋯
            </Btn>
            {archiveOpen && (
              <div role='menu' className={styles.archiveMenu}>
                <button
                  type='button'
                  role='menuitem'
                  className={styles.archiveItem}
                  onClick={handleArchive}>
                  Archive deck
                </button>
              </div>
            )}
          </div>
        </>
      )}
      {status === "ARCHIVED" && (
        <Btn
          size='md'
          shape='pill'
          variant='default'
          disabled={busy}
          onClick={handlePublish}>
          Republish
        </Btn>
      )}
    </div>
  );
};

export { PublishStatusControl };
