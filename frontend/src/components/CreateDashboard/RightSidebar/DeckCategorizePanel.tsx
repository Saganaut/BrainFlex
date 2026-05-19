// Deck-level metadata panel for the right-sidebar inspector. Owns the
// subject-tag dropdown (single curated root) and the multi-select tag picker
// that drives Explore discoverability. Edits commit through `updateDeck` —
// the apiEnhancements layer keeps the cached deck in sync, so the rest of
// the editor sees the change immediately.
import { getRouteApi } from "@tanstack/react-router";
import { useMemo } from "react";
import {
  useGetDeckQuery,
  useListTagsQuery,
  useUpdateDeckMutation,
} from "@/store/BrainFlexApi";
import { TagPicker } from "@/components/Common/TagPicker/TagPicker";
import { Dropdown } from "@/components/Common/Input/Dropdown/Dropdown";
import styles from "./EditSlidePanel.module.css";

const routeApi = getRouteApi("/decks/$deckId/edit");

const DeckCategorizePanel = () => {
  const { deckId } = routeApi.useParams();
  const { data: deck } = useGetDeckQuery({ id: deckId });
  const { data: curated = [] } = useListTagsQuery({ curated: true });
  const [updateDeck] = useUpdateDeckMutation();

  const subjectOptions = useMemo(
    () =>
      curated
        .filter((tag) => tag.id != null)
        .map((tag) => ({ value: tag.id ?? "", label: tag.displayName ?? "" })),
    [curated],
  );

  if (!deck) {
    return (
      <div className={styles.empty}>
        <p>Loading deck…</p>
      </div>
    );
  }

  const tagIds = deck.tagIds ?? [];
  const subjectTagId = deck.subjectTagId;

  const commit = (patch: { tagIds?: string[]; subjectTagId?: string }) => {
    void updateDeck({
      id: deckId,
      updateDeckRequest: patch,
    })
      .unwrap()
      .catch((err: unknown) => {
        console.error("Failed to update deck categorization", err);
      });
  };

  return (
    <div className={styles.panel}>
      <section className={styles.section}>
        <h4 className={styles.heading}>Subject</h4>
        <Dropdown
          options={subjectOptions}
          value={subjectTagId != null ? [subjectTagId] : []}
          onChange={(values) => {
            commit({ subjectTagId: values[0] ?? "" });
          }}
          searchable
          placeholder='Pick a primary subject…'
        />
      </section>

      <section className={styles.section}>
        <h4 className={styles.heading}>Tags</h4>
        <TagPicker
          value={tagIds}
          onChange={(next) => {
            commit({ tagIds: next });
          }}
          placeholder='Search and add tags…'
        />
      </section>
    </div>
  );
};

export { DeckCategorizePanel };
