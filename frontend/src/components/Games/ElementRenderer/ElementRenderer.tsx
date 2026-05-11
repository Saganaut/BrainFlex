/**
 * Dispatches a polymorphic DeckElement to the right per-kind renderer.
 *
 * Each renderer is fed the un-redacted post-round element when a result is
 * available (so it can highlight the correct answer) — during SUBMIT it gets
 * the element as broadcast (correct-answer fields nulled out).
 *
 * Renderers that aren't implemented yet show a placeholder; the player can
 * Skip to register a TimeoutAnswer and move on.
 */
import { SlideView } from "../SlideView/SlideView";
import { AnswerOptions } from "../AnswerOptions/AnswerOptions";
import { TextAnswerInput } from "../TextAnswerInput/TextAnswerInput";
import { NumberAnswerInput } from "../NumberAnswerInput/NumberAnswerInput";
import { PlaceholderAnswer } from "../PlaceholderAnswer/PlaceholderAnswer";
import type { AnswerPayload, DeckElement } from "../../../types/elements";

export interface ElementRendererProps {
  element: DeckElement;
  round: number;
  totalRounds: number;
  timeRemaining: number;
  // Local user's submitted payload (null until they answer).
  mySubmission: AnswerPayload | null;
  // Server's reveal data (un-redacted element + per-player results) when round ends.
  roundResultElement: DeckElement | null;
  onSubmit: (payload: AnswerPayload) => void;
}

const ElementRenderer = ({
  element,
  round,
  totalRounds,
  timeRemaining,
  mySubmission,
  roundResultElement,
  onSubmit,
}: ElementRendererProps) => {
  // Slides have no input — render and let the round timer advance.
  if (element.kind === "Slide") {
    return (
      <SlideView
        slide={element}
        round={round}
        totalRounds={totalRounds}
        timeRemaining={timeRemaining}
      />
    );
  }

  // Use the revealed (un-redacted) element if the round result has arrived;
  // otherwise the redacted broadcast version.
  const liveElement = roundResultElement ?? element;
  const disabled = mySubmission !== null;

  switch (liveElement.kind) {
    case "McqQuestion": {
      const options = liveElement.options ?? [];
      const selectedIdx = optionIndex(options, mySubmission);
      const correctIdx = roundResultElement
        ? options.findIndex((o) => o.id === liveElement.correctOptionId)
        : -1;
      return (
        <AnswerOptions
          options={options.map((o) => o.text ?? "")}
          selectedOption={selectedIdx >= 0 ? selectedIdx : null}
          correctOption={correctIdx >= 0 ? correctIdx : undefined}
          onSelect={(idx) => {
            const opt = options[idx];
            if (opt.id) {
              onSubmit({ kind: "McqAnswer", optionId: opt.id });
            }
          }}
          disabled={disabled}
        />
      );
    }

    case "ImageChoiceQuestion": {
      const options = liveElement.options ?? [];
      const selectedIdx = optionIndex(options, mySubmission);
      const correctIdx = roundResultElement
        ? options.findIndex((o) => o.id === liveElement.correctOptionId)
        : -1;
      return (
        <AnswerOptions
          options={options.map((o) => o.text ?? "")}
          selectedOption={selectedIdx >= 0 ? selectedIdx : null}
          correctOption={correctIdx >= 0 ? correctIdx : undefined}
          onSelect={(idx) => {
            const opt = options[idx];
            if (opt.id) {
              onSubmit({ kind: "ImageChoiceAnswer", optionId: opt.id });
            }
          }}
          disabled={disabled}
        />
      );
    }

    case "TextQuestion": {
      const myText =
        mySubmission?.kind === "TextAnswer" ? (mySubmission.text ?? null) : null;
      const correctText = roundResultElement ? liveElement.correctAnswer : undefined;
      return (
        <TextAnswerInput
          key={liveElement.id}
          questionId={liveElement.id ?? ""}
          submittedAnswer={myText}
          correctAnswerText={correctText ?? undefined}
          wasCorrect={undefined}
          onSubmit={(text) => {
            onSubmit({ kind: "TextAnswer", text });
          }}
          disabled={disabled}
        />
      );
    }

    case "NumberQuestion": {
      const myValue =
        mySubmission?.kind === "NumberAnswer" ? (mySubmission.value ?? null) : null;
      return (
        <NumberAnswerInput
          key={liveElement.id}
          element={liveElement}
          submittedValue={myValue}
          revealedCorrectValue={
            roundResultElement ? liveElement.correctValue : undefined
          }
          onSubmit={(value) => {
            onSubmit({ kind: "NumberAnswer", value });
          }}
          disabled={disabled}
        />
      );
    }

    case "RankingQuestion":
    case "ScalesQuestion":
    case "QAndAQuestion":
    case "GridQuestion":
    case "PlaceOnImageQuestion":
      return (
        <PlaceholderAnswer
          element={liveElement}
          disabled={disabled}
          onSkip={() => {
            // Submit a TimeoutAnswer so the round can complete cleanly while
            // the per-kind renderer is still on the roadmap.
            onSubmit({ kind: "TimeoutAnswer" });
          }}
        />
      );

    default:
      return null;
  }
};

const optionIndex = (
  options: { id?: string }[],
  payload: AnswerPayload | null,
): number => {
  if (!payload) return -1;
  if (payload.kind !== "McqAnswer" && payload.kind !== "ImageChoiceAnswer") {
    return -1;
  }
  const oid = payload.optionId;
  if (!oid) return -1;
  return options.findIndex((o) => o.id === oid);
};

export { ElementRenderer };
