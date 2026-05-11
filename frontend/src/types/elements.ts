// Discriminated unions mirroring the Java sealed hierarchies.
//
// The OpenAPI codegen produces individual record types per impl but no union;
// these unions are hand-authored so TypeScript can exhaustively switch on the
// `kind` field. Wire format is `kind: "<ClassName>"` (Jackson's default), so
// the literal types here must match those exact strings.
import type {
  GridQuestion,
  ImageChoiceQuestion,
  McqAnswer,
  McqOption,
  McqQuestion,
  NumberQuestion,
  PlaceOnImageQuestion,
  QAndAQuestion,
  RankingAnswer,
  RankingItem,
  RankingQuestion,
  ScalesAnswer,
  ScalesQuestion,
  Slide,
  TextQuestion,
} from "../store/BrainFlexApi";

// Codegen names them `<Class>Base` for the abstract parent so reproduce the answer leaves.
export interface TextAnswer {
  kind: "TextAnswer";
  text?: string;
}
export interface NumberAnswer {
  kind: "NumberAnswer";
  value?: number;
}
export interface ImageChoiceAnswer {
  kind: "ImageChoiceAnswer";
  optionId?: string;
}
export interface GridAnswer {
  kind: "GridAnswer";
  selectedCellIndexes?: number[];
}
export interface PlaceOnImageAnswer {
  kind: "PlaceOnImageAnswer";
  x?: number;
  y?: number;
}
export interface TimeoutAnswer {
  kind: "TimeoutAnswer";
}

export type DeckElement =
  | Slide
  | McqQuestion
  | TextQuestion
  | NumberQuestion
  | ImageChoiceQuestion
  | RankingQuestion
  | ScalesQuestion
  | QAndAQuestion
  | GridQuestion
  | PlaceOnImageQuestion;

export type AnswerPayload =
  | McqAnswer
  | TextAnswer
  | NumberAnswer
  | ImageChoiceAnswer
  | RankingAnswer
  | ScalesAnswer
  | GridAnswer
  | PlaceOnImageAnswer
  | TimeoutAnswer;

export type {
  McqOption,
  RankingItem,
  Slide,
  McqQuestion,
  TextQuestion,
  NumberQuestion,
  ImageChoiceQuestion,
  RankingQuestion,
  ScalesQuestion,
  QAndAQuestion,
  GridQuestion,
  PlaceOnImageQuestion,
  McqAnswer,
  RankingAnswer,
  ScalesAnswer,
};

/** True when an element is a non-interactive Slide. */
export const isSlide = (e: DeckElement): e is Slide => e.kind === "Slide";

/** True when an element is one of the scored question kinds. */
export const isQuestion = (e: DeckElement): boolean => e.kind !== "Slide";
