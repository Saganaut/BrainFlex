// Thin wrapper that renders the right decorative icon for a given
// `DeckElement["kind"]`. Callers should prefer this over reaching into the
// `slideTypeGraphics` map directly — it keeps lookup logic in one place.
import { slideTypeGraphics, type ElementKind } from "./slideTypeGraphics";

interface SlideTypeGraphicProps {
  kind: ElementKind;
}

const SlideTypeGraphic = ({ kind }: SlideTypeGraphicProps) => {
  const Graphic = slideTypeGraphics[kind];
  return <Graphic />;
};

export { SlideTypeGraphic };
