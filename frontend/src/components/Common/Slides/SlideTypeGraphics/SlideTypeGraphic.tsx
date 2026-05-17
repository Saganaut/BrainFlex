// Thin wrapper that renders the right decorative icon for a given
// `DeckElement["kind"]`. Callers should prefer this over reaching into the
// `slideTypeGraphics` map directly — it keeps lookup logic in one place.
// Size scale mirrors IconBtn (xs/sm/md/lg) so the two render consistently
// wherever they sit side by side.
import type { BtnSize } from "@/components/Common/Buttons/BtnTypes";
import { slideTypeGraphics, type ElementKind } from "./slideTypeGraphics";
import styles from "./SlideTypeGraphic.module.css";

interface SlideTypeGraphicProps {
  kind: ElementKind;
  size?: BtnSize;
}

const SlideTypeGraphic = ({ kind, size = "md" }: SlideTypeGraphicProps) => {
  const Graphic = slideTypeGraphics[kind];
  return (
    <div className={[styles.wrapper, styles[size]].join(" ")}>
      <Graphic />
    </div>
  );
};

export { SlideTypeGraphic };