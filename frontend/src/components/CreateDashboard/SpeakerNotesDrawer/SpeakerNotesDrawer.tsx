/**
 * Bottom drawer below the slide canvas where the author records private
 * notes-to-self for the active element. Speaker notes live on EVERY element
 * kind (not just Slide), so this drawer reads/writes the active element's
 * `speakerNotes` field regardless of kind.
 *
 * Edits use the same debounced-commit pattern as the SlideContentTypes
 * editors: type into RichTextInput → schedule(patch) → flush() on blur. The
 * apiEnhancements layer syncs the response into the getDeck cache.
 */
import { useState } from "react";
import { getRouteApi } from "@tanstack/react-router";
import { ChevronUpIcon, ChevronDownIcon } from "@heroicons/react/24/outline";
import {
  useGetDeckQuery,
  useUpdateElementMutation,
  type DeckDto,
} from "@/store/BrainFlexApi";
import { useDebouncedCommit } from "@/hooks/useDebouncedCommit";
import { RichTextInput } from "@/components/Common/Input/RichTextInput";
import styles from "./SpeakerNotesDrawer.module.css";

type DeckElement = NonNullable<DeckDto["elements"]>[number];

const routeApi = getRouteApi("/decks/$deckId/edit");

const SpeakerNotesDrawer = () => {
  const { deckId } = routeApi.useParams();
  const { questionId } = routeApi.useSearch();

  const { element } = useGetDeckQuery(
    { id: deckId },
    {
      selectFromResult: ({ data }) => ({
        element: data?.elements?.find((e) => e.id === questionId),
      }),
    },
  );

  const [updateElement] = useUpdateElementMutation();

  const commit = (patch: DeckElement) => {
    if (!element?.id) return;
    void updateElement({
      id: deckId,
      elementId: element.id,
      body: patch,
    })
      .unwrap()
      .catch((err: unknown) => {
        console.error("Failed to update speaker notes", err);
      });
  };

  const { schedule, flush } = useDebouncedCommit<DeckElement>(commit, 500);

  const [notes, setNotes] = useState<string>(element?.speakerNotes ?? "");
  const [syncedFromId, setSyncedFromId] = useState<string | undefined>(
    element?.id,
  );
  if (element && syncedFromId !== element.id) {
    setSyncedFromId(element.id);
    setNotes(element.speakerNotes ?? "");
  }

  const [isOpen, setIsOpen] = useState(false);

  const handleNotesChange = (html: string) => {
    setNotes(html);
    if (!element) return;
    schedule({ ...element, speakerNotes: html });
  };

  const hasNotes = notes.trim() !== "" && notes !== "<p></p>";

  return (
    <section
      className={[styles.drawer, isOpen ? styles.open : ""]
        .filter(Boolean)
        .join(" ")}
      aria-label='Speaker notes'>
      <button
        type='button'
        className={styles.header}
        aria-expanded={isOpen}
        aria-controls='speaker-notes-body'
        onClick={() => {
          setIsOpen((prev) => !prev);
        }}>
        <span className={styles.headerLabel}>
          Speaker notes
          {hasNotes && <span className={styles.headerDot} aria-hidden='true' />}
        </span>
        <span className={styles.headerChevron} aria-hidden='true'>
          {isOpen ? <ChevronDownIcon /> : <ChevronUpIcon />}
        </span>
      </button>

      {isOpen && (
        <div
          className={styles.body}
          id='speaker-notes-body'
          role='region'
          aria-label='Speaker notes editor'>
          {element ? (
            <RichTextInput
              id={`speaker-notes-${element.id ?? ""}`}
              placeholder='Notes for the presenter — never shown to participants.'
              value={notes}
              onChange={handleNotesChange}
              onBlur={flush}
            />
          ) : (
            <p className={styles.emptyState}>
              Select a slide to add speaker notes.
            </p>
          )}
        </div>
      )}
    </section>
  );
};

export { SpeakerNotesDrawer };
