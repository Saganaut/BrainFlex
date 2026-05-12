/**
 * Modal body that lets the user pick which element kind to add to the deck.
 *
 * Renders the existing `SlideTypeGraphics` as clickable tiles. Clicking a tile
 * immediately invokes `onPick` (which closes the modal and creates the element
 * via the deck-dashboard hook). No two-step "select + confirm" — the click is
 * the commit.
 */
import type { ElementKind } from "@/components/Common/Slides/SlideTypeGraphics/slideTypeGraphics";
import { slideTypeGraphics } from "@/components/Common/Slides/SlideTypeGraphics/slideTypeGraphics";
import styles from "./NewElementPicker.module.css";

interface NewElementPickerProps {
  onPick: (kind: ElementKind) => void;
}

const KIND_LABELS: Record<ElementKind, string> = {
  Slide: "Slide",
  McqQuestion: "Multiple Choice",
  TextQuestion: "Text Answer",
  NumberQuestion: "Number Answer",
  ImageChoiceQuestion: "Image Choice",
  RankingQuestion: "Ranking",
  ScalesQuestion: "Scales",
  QAndAQuestion: "Q & A",
  GridQuestion: "Grid",
  PlaceOnImageQuestion: "Place on Image",
};

// Preserve the order defined in the graphics map by reading its keys directly.
const ELEMENT_KINDS = Object.keys(slideTypeGraphics) as ElementKind[];

const NewElementPicker = ({ onPick }: NewElementPickerProps) => {
  return (
    <div className={styles.grid}>
      {ELEMENT_KINDS.map((kind) => {
        const Graphic = slideTypeGraphics[kind];
        return (
          <button
            key={kind}
            type='button'
            className={styles.tile}
            onClick={() => {
              onPick(kind);
            }}
            aria-label={`Add ${KIND_LABELS[kind]}`}>
            <Graphic />
            <span className={styles.label}>{KIND_LABELS[kind]}</span>
          </button>
        );
      })}
    </div>
  );
};

export { NewElementPicker };
