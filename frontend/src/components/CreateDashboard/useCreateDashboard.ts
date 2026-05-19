import { getRouteApi, useNavigate } from "@tanstack/react-router";
import { useMemo, useState } from "react";
import {
  BrainFlex,
  useAddElementMutation,
  useGetDeckQuery,
  useMoveElementMutation,
  useUpdateDeckMutation,
  type DeckDto,
} from "@/store/BrainFlexApi";
import { useAppDispatch } from "@/store/hooks";
import type { DragEndEvent } from "@dnd-kit/dom";
import { isSortable } from "@dnd-kit/dom/sortable";
import type { ElementKind } from "@/components/Common/Slides/SlideTypeGraphics/slideTypeGraphics";

export type DeckElement = NonNullable<DeckDto["elements"]>[number];
type AddElementBody = Parameters<
  ReturnType<typeof useAddElementMutation>[0]
>[0]["body"];

interface useCreateDashboardResponse {
  titleDraft: string;
  setTitleDraft: (value: string) => void;
  commitTitle: () => void;
  serverName: string;
  handleAddElement: (kind: ElementKind) => void;
  handleDragEnd: (event: DragEndEvent) => void;
  elements: DeckElement[];
  deckId: string;
  questionId?: string;
}

const routeApi = getRouteApi("/decks/$deckId/edit");

/**
 * Construct a minimum-viable payload for the requested element kind. The
 * backend records use primitive `int`/`double`/`boolean` fields that Jackson
 * can't deserialize from null, so every primitive needs a default here. Strings
 * and complex sub-objects can be left out — only the primitives matter.
 */
const buildNewElement = (kind: ElementKind, id: string): AddElementBody => {
  const sharedQuestionDefaults = {
    id,
    prompt: "",
    title: "",
    pointValue: 0,
    scored: true,
    survey: false,
    bestAnswerMode: false,
    bestAnswerBonus: 0,
    displaySeconds: 0,
    mediaPosition: "NONE" as const,
  };

  switch (kind) {
    case "Slide":
      return {
        kind: "Slide",
        id,
        slideKind: "CONTENT",
        title: "New slide",
        body: "",
        scored: false,
        survey: false,
        displaySeconds: 0,
        mediaPosition: "NONE",
        resultsDisplayType: "DEFAULT",
        multipleSelectionsEnabled: false,
        selectionsPerParticipant: 1,
        showResultsAsPercentage: false,
        joinType: "INSTRUCTIONS_BAR",
        showJoinInformation: true,
        showResponses: "INSTANT",
      };
    case "McqQuestion":
      return {
        kind: "McqQuestion",
        ...sharedQuestionDefaults,
        // 4 blank starter options per the MCQ editor's default; user can add
        // up to 6 or remove down to 2. No correct answers selected by default
        // — the editor warns the author that the slide is unscoreable until
        // at least one is set.
        options: Array.from({ length: 4 }, () => ({
          id: crypto.randomUUID(),
          text: "",
        })),
        correctOptionIds: [],
      };
    case "TextQuestion":
      return {
        kind: "TextQuestion",
        ...sharedQuestionDefaults,
        caseSensitive: false,
      };
    case "NumberQuestion":
      return {
        kind: "NumberQuestion",
        ...sharedQuestionDefaults,
        correctValue: 0,
        tolerance: 0,
        decimalPlaces: 0,
      };
    case "RankingQuestion":
      return { kind: "RankingQuestion", ...sharedQuestionDefaults };
    case "ScalesQuestion":
      return {
        kind: "ScalesQuestion",
        ...sharedQuestionDefaults,
        scaleMin: 1,
        scaleMax: 5,
        scored: false,
      };
    case "QAndAQuestion":
      return {
        kind: "QAndAQuestion",
        ...sharedQuestionDefaults,
        scored: false,
        survey: true,
        maxSubmissionsPerPlayer: 0,
        allowVoting: false,
        autoApprove: false,
      };
    case "GridQuestion":
      return {
        kind: "GridQuestion",
        ...sharedQuestionDefaults,
        rows: 3,
        cols: 3,
        multipleCorrect: false,
      };
    case "PlaceOnImageQuestion":
      return {
        kind: "PlaceOnImageQuestion",
        ...sharedQuestionDefaults,
        correctX: 0.5,
        correctY: 0.5,
        tolerance: 0.1,
      };
    case "WordCloudQuestion":
      return {
        kind: "WordCloudQuestion",
        ...sharedQuestionDefaults,
        scored: false,
        survey: true,
        maxSubmissionsPerPlayer: 3,
        maxWordLength: 30,
        caseSensitive: false,
        profanityFilter: true,
        bannedWords: [],
      };
    case "AllocationQuestion":
      return {
        kind: "AllocationQuestion",
        ...sharedQuestionDefaults,
        scored: false,
        survey: true,
        // Two starter options so the player view has something to render; the
        // author can add up to MAX_OPTIONS in the editor.
        options: Array.from({ length: 4 }, () => ({
          id: crypto.randomUUID(),
          text: "",
        })),
        totalPointsToDistribute: 100,
        allowZeroOnItem: true,
        enforceExactTotal: true,
      };
    case "MatchingQuestion":
      return {
        kind: "MatchingQuestion",
        ...sharedQuestionDefaults,
        pointValue: 100,
        // Four starter pairs (mirrors the editor's MIN/DEFAULT). Pair ids are
        // minted up front so optimistic UI and the answer-key invariant
        // (leftId == rightId == pair.id) stay stable from the first render.
        pairs: Array.from({ length: 4 }, () => ({
          id: crypto.randomUUID(),
          leftLabel: "",
          rightLabel: "",
        })),
        scoring: "ALL_OR_NOTHING",
      };
    case "DrawingQuestion":
      return {
        kind: "DrawingQuestion",
        ...sharedQuestionDefaults,
        scored: false,
        survey: true,
        // 1920x1080 logical units. The player canvas scales strokes from
        // here to whatever pixel canvas it renders — see the README in the
        // chunk for the contract.
        canvasWidth: 1920,
        canvasHeight: 1080,
        // Caps keep a single DrawingAnswer bounded; the server enforces an
        // additional per-payload byte cap (default 256 KB).
        maxStrokesPerPlayer: 200,
        maxPointsPerStroke: 500,
        palette: [],
      };
  }
};

/** Scroll the newly-added thumbnail into view; runs after navigate commits. */
const scrollThumbnailIntoView = (elementId: string) => {
  // Defer one frame so the new SlideThumbnail has mounted with its HTML id.
  requestAnimationFrame(() => {
    document.getElementById(elementId)?.scrollIntoView({
      behavior: "smooth",
      block: "nearest",
    });
  });
};

const useCreateDashboard = (): useCreateDashboardResponse => {
  const { deckId } = routeApi.useParams();
  const { questionId } = routeApi.useSearch();
  const dispatch = useAppDispatch();
  const navigate = useNavigate({ from: routeApi.id });
  const { data: deck } = useGetDeckQuery({ id: deckId });
  const [addElement] = useAddElementMutation();
  const [moveElement] = useMoveElementMutation();

  const elements = useMemo(() => deck?.elements ?? [], [deck?.elements]);

  const [updateDeck] = useUpdateDeckMutation();

  const serverName = deck?.name ?? "";
  const [draft, setDraft] = useState<{ value: string; syncedFrom: string }>({
    value: serverName,
    syncedFrom: serverName,
  });
  if (draft.syncedFrom !== serverName) {
    setDraft({ value: serverName, syncedFrom: serverName });
  }
  const titleDraft = draft.value;

  const setTitleDraft = (value: string) => {
    setDraft({ value, syncedFrom: serverName });
  };

  const commitTitle = () => {
    const next = titleDraft.trim();
    if (!next || next === serverName) {
      setTitleDraft(serverName);
      return;
    }
    void updateDeck({
      id: deckId,
      updateDeckRequest: { name: next },
    })
      .unwrap()
      .catch((err: unknown) => {
        console.error("Failed to update deck title", err);
      });
  };

  /** -----------  SLIDES ------------------**/

  const handleDragEnd = (event: DragEndEvent) => {
    const { source } = event.operation;
    if (!isSortable(source)) return;
    const { initialIndex, index } = source;
    if (initialIndex === index) return;
    const moved = elements[initialIndex];
    if (!moved.id) return;

    // Optimistic local reorder — patch the cached deck so the UI reflects the
    // drop instantly. The server response (full deck) overwrites this once the
    // moveElement call resolves.
    dispatch(
      BrainFlex.util.updateQueryData("getDeck", { id: deckId }, (draft) => {
        const list = draft.elements;
        if (!list) return;
        const [item] = list.splice(initialIndex, 1);
        list.splice(index, 0, item);
      }),
    );

    void moveElement({ id: deckId, elementId: moved.id, to: index })
      .unwrap()
      .catch((err: unknown) => {
        console.error("Failed to move element", err);
      });
  };

  const handleAddElement = (kind: ElementKind) => {
    const newId = crypto.randomUUID();
    const newElement = buildNewElement(kind, newId);

    void addElement({ id: deckId, body: newElement })
      .unwrap()
      .then(() => {
        void navigate({
          search: (prev) => ({ ...prev, questionId: newId }),
        });
        scrollThumbnailIntoView(newId);
      })
      .catch((err: unknown) => {
        console.error("Failed to add element", err);
      });
  };

  return {
    commitTitle,
    serverName,
    titleDraft,
    setTitleDraft,
    deckId,
    handleAddElement,
    handleDragEnd,
    elements,
    questionId,
  };
};

export { useCreateDashboard };
