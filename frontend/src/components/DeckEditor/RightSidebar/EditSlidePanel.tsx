// Top-level dispatcher for the deck-editor's "edit" drawer. Pulls the active
// element from the deck cache, mounts the per-kind options section, then the
// cross-cutting Tags + Common sections that every kind shares, and finally
// the provenance footer. Each subsection owns its own debounced commit and
// local-state mirror (see `useElementEditor`); this file just routes.
import { getRouteApi } from "@tanstack/react-router";
import { useGetDeckQuery } from "@/store/BrainFlexApi";
import { SlideOptionsSection } from "./EditSlideSections/SlideOptionsSection";
import { McqOptionsSection } from "./EditSlideSections/McqOptionsSection";
import { TextOptionsSection } from "./EditSlideSections/TextOptionsSection";
import { NumberOptionsSection } from "./EditSlideSections/NumberOptionsSection";
import { RankingOptionsSection } from "./EditSlideSections/RankingOptionsSection";
import { QAndAOptionsSection } from "./EditSlideSections/QAndAOptionsSection";
import { CommonOptionsSection } from "./EditSlideSections/CommonOptionsSection";
import { ProvenanceFooter } from "./EditSlideSections/ProvenanceFooter";
import styles from "./EditSlidePanel.module.css";

const routeApi = getRouteApi("/decks/$deckId/edit");

const PerKindSection = ({ kind }: { kind: string }) => {
  switch (kind) {
    case "Slide":
      return <SlideOptionsSection />;
    case "McqQuestion":
      return <McqOptionsSection />;
    case "TextQuestion":
      return <TextOptionsSection />;
    case "NumberQuestion":
      return <NumberOptionsSection />;
    case "RankingQuestion":
      return <RankingOptionsSection />;
    case "QAndAQuestion":
      return <QAndAOptionsSection />;
    default:
      return null;
  }
};

const EditSlidePanel = () => {
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

  if (!element) {
    return (
      <div className={styles.empty}>
        <p>Select a slide on the left to edit its display options.</p>
      </div>
    );
  }

  return (
    <div className={styles.panel}>
      <PerKindSection kind={element.kind} />
      <CommonOptionsSection />
      <ProvenanceFooter
        createdByUserId={element.createdByUserId}
        lastEditedByUserId={element.lastEditedByUserId}
        createdAt={element.createdAt}
        updatedAt={element.updatedAt}
        version={element.version}
      />
    </div>
  );
};

export { EditSlidePanel };
