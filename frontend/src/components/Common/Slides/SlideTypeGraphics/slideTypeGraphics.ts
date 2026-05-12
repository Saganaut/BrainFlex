// Mapping from `DeckElement["kind"]` to the matching decorative icon. Lives
// in its own module so the `SlideTypeGraphic` wrapper file can export a
// component and only a component (react-refresh/only-export-components).
import type { ComponentType } from "react";
import type { DeckElement } from "@/types/elements";
import { GridGraphic } from "./GridGraphic";
import { ImageChoiceGraphic } from "./ImageChoiceGraphic";
import { McqGraphic } from "./McqGraphic";
import { NumberGraphic } from "./NumberGraphic";
import { PlaceOnImageGraphic } from "./PlaceOnImageGraphic";
import { QAndAGraphic } from "./QAndAGraphic";
import { RankingGraphic } from "./RankingGraphic";
import { ScalesGraphic } from "./ScalesGraphic";
import { SlideGraphic } from "./SlideGraphic";
import { TextGraphic } from "./TextGraphic";

export type ElementKind = DeckElement["kind"];

export const slideTypeGraphics: Record<ElementKind, ComponentType> = {
  Slide: SlideGraphic,
  McqQuestion: McqGraphic,
  TextQuestion: TextGraphic,
  NumberQuestion: NumberGraphic,
  ImageChoiceQuestion: ImageChoiceGraphic,
  RankingQuestion: RankingGraphic,
  ScalesQuestion: ScalesGraphic,
  QAndAQuestion: QAndAGraphic,
  GridQuestion: GridGraphic,
  PlaceOnImageQuestion: PlaceOnImageGraphic,
};
