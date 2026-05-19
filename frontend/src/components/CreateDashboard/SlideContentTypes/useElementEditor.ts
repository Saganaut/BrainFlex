/**
 * Shared plumbing for slide-content editors. Each kind-specific editor owns
 * its own local-state shape, but the read/write boundary is identical: pull
 * the active element from the deck cache, debounce a commit, and flush on
 * blur / structural change. This hook centralises that.
 *
 * Returns:
 *   - element        — the active element, narrowed to T by the caller's
 *                      `selectKind` predicate; undefined while loading or if
 *                      the active element is of a different kind.
 *   - schedule(p)    — debounced PUT /elements/{id}.
 *   - flush()        — fire the buffered commit now (use on blur).
 *   - commit(p)      — fire immediately (use for structural changes —
 *                      add/remove option, etc.).
 *   - syncedFromId   — last element.id we synced local state from. Editors
 *                      compare this against `element.id` during render to
 *                      decide whether to reset their local mirror.
 *   - markSynced(id) — update syncedFromId after a successful resync.
 */
import { useState } from "react";
import { getRouteApi } from "@tanstack/react-router";
import { isSortable } from "@dnd-kit/dom/sortable";
import type { DragEndEvent } from "@dnd-kit/dom";
import {
  BrainFlex,
  useGetDeckQuery,
  useMoveMcqOptionMutation,
  useUpdateElementMutation,
  type DeckDto,
  type McqOption,
  type McqQuestion,
} from "@/store/BrainFlexApi";
import { useAppDispatch } from "@/store/hooks";
import { useCurrentUser } from "@/hooks/useCurrentUser";
import { useDebouncedCommit } from "@/hooks/useDebouncedCommit";

type DeckElement = NonNullable<DeckDto["elements"]>[number];

/** Shared option-count bounds for MCQ-shaped questions. Enforced inside
 *  the hooks so out-of-bounds calls are silent no-ops rather than corrupt
 *  saves; exported so callers can hide their UI when at the bound. */
const MIN_MCQ_OPTIONS = 2;
const MAX_MCQ_OPTIONS = 6;

const isMcqQuestion = (e: DeckElement): e is McqQuestion =>
  e.kind === "McqQuestion";

const routeApi = getRouteApi("/decks/$deckId/edit");

interface ElementEditorApi<T extends DeckElement> {
  element: T | undefined;
  schedule: (patch: T) => void;
  flush: () => void;
  commit: (patch: T) => void;
  syncedFromId: string | undefined;
  markSynced: (id: string | undefined) => void;
}

const useElementEditor = <T extends DeckElement>(
  selectKind: (e: DeckElement) => e is T,
  delay = 500,
): ElementEditorApi<T> => {
  const { deckId } = routeApi.useParams();
  const { questionId } = routeApi.useSearch();
  const currentUser = useCurrentUser();
  const currentUserId =
    currentUser.state === "registered" || currentUser.state === "guest"
      ? currentUser.user.id
      : undefined;

  const { element } = useGetDeckQuery(
    { id: deckId },
    {
      selectFromResult: ({ data }) => {
        const e = data?.elements?.find((el) => el.id === questionId);
        return { element: e && selectKind(e) ? e : undefined };
      },
    },
  );

  const [updateElement] = useUpdateElementMutation();

  // Optimistic provenance stamp (chunk 10b). The backend overwrites these on
  // save, but stamping client-side gives the in-flight cache patch the right
  // version + author for the editor's "last edited by" footer to render
  // immediately. `version` bumps off whatever the patch carries, which is
  // built from the cached element above.
  const commit = (patch: T) => {
    if (!element?.id) return;
    const stampedPatch: T = {
      ...patch,
      lastEditedByUserId: currentUserId,
      version: (patch.version ?? 0) + 1,
    };
    void updateElement({
      id: deckId,
      elementId: element.id,
      body: stampedPatch,
    })
      .unwrap()
      .catch((err: unknown) => {
        console.error("Failed to update element", err);
      });
  };

  const { schedule, flush } = useDebouncedCommit<T>(commit, delay);

  const [syncedFromId, setSyncedFromId] = useState<string | undefined>(
    element?.id,
  );

  return {
    element,
    schedule,
    flush,
    commit,
    syncedFromId,
    markSynced: setSyncedFromId,
  };
};

/**
 * Option-scoped editor on top of `useElementEditor<McqQuestion>`. The active
 * deck element must be an `McqQuestion`; the hook finds the option by id
 * inside `parent.options` and returns helpers that rebuild the parent
 * question with that single option swapped, then route the commit through
 * the shared deck-element mutation pipeline.
 *
 * Why this lives next to `useElementEditor`:
 *   `McqOption` is embedded in `McqQuestion`, which is what the
 *   `updateElement` mutation actually accepts. There is no per-option
 *   endpoint — every option edit becomes a full McqQuestion write. This
 *   hook keeps that mechanical detail out of the option-editor component
 *   and reuses the existing debouncer/cache-sync plumbing.
 *
 * Concurrency note: each option editor instance owns its own debounce
 * timer (because `useElementEditor` is called once per option editor). In
 * practice this is safe because only one input can hold focus at a time,
 * and every input flushes on blur — switching from option A to option B
 * fires A's pending commit first. Color/image clicks commit immediately,
 * sidestepping the debounce window entirely.
 */
interface McqOptionEditorApi {
  /** The freshest option from the deck cache, narrowed by id. Undefined
   *  while the deck is loading or if the active element isn't an MCQ. */
  option: McqOption | undefined;
  /** Parent question — exposed for callers that need sibling option
   *  context (e.g. cache mutation keyed off the question id). */
  parent: McqQuestion | undefined;

  // ── field-level commits (option's own shape) ─────────────────────────
  /** Debounced commit of `next` as the new value for this option id. */
  schedule: (next: McqOption) => void;
  /** Immediate commit of `next` (use for structural changes — image pick,
   *  color swatch click, etc.). */
  commit: (next: McqOption) => void;
  /** Flush the pending debounced commit (typical: onBlur). Also called
   *  internally before structural ops below. */
  flush: () => void;

  // ── derived parent-state, scoped to this option ──────────────────────
  /** Zero-based position in `parent.options`. -1 if not found. */
  index: number;
  /** Whether this option's id is in `parent.correctOptionIds`. */
  isCorrect: boolean;
  /** Whether the parent has more than `MIN_MCQ_OPTIONS` options (so
   *  removing this one is allowed). */
  canRemove: boolean;

  // ── question-shape ops scoped to this option (immediate commits) ────
  /** Flip this option's id in `parent.correctOptionIds`. */
  toggleCorrect: () => void;
  /** Remove this option from `parent.options` and strip its id from
   *  `parent.correctOptionIds`. No-op below the min-option bound. */
  remove: () => void;

  syncedFromId: string | undefined;
  markSynced: (id: string | undefined) => void;
}

const useMcqOptionEditor = (
  optionId: string | undefined,
  delay = 500,
): McqOptionEditorApi => {
  const {
    element: parent,
    schedule: scheduleParent,
    commit: commitParent,
    flush,
    syncedFromId,
    markSynced,
  } = useElementEditor<McqQuestion>(isMcqQuestion, delay);

  const options = parent?.options ?? [];
  const index = optionId ? options.findIndex((o) => o.id === optionId) : -1;
  const option = index >= 0 ? options[index] : undefined;
  const isCorrect = !!(
    optionId && parent?.correctOptionIds?.includes(optionId)
  );
  const canRemove = options.length > MIN_MCQ_OPTIONS;

  /** Build a complete McqQuestion patch from `parent` with overrides
   *  applied. The defaults read from the cache, so concurrent edits in
   *  sibling McqOptionEditable instances aren't stomped (the latest
   *  flushed value is whatever's in `parent.options` right now). Returns
   *  undefined when there's no parent — callers treat that as a no-op. */
  const patchParent = (overrides: {
    options?: McqOption[];
    correctOptionIds?: string[];
  }): McqQuestion | undefined => {
    if (!parent) return undefined;
    return {
      ...parent,
      options: overrides.options ?? parent.options,
      correctOptionIds: overrides.correctOptionIds ?? parent.correctOptionIds,
    };
  };

  /** Patch this single option in place inside `parent.options`. */
  const replaceOption = (next: McqOption): McqOption[] | undefined => {
    if (!optionId) return undefined;
    return options.map((o) => (o.id === optionId ? next : o));
  };

  const schedule = (next: McqOption) => {
    const swapped = replaceOption(next);
    if (!swapped) return;
    const patched = patchParent({ options: swapped });
    if (patched) scheduleParent(patched);
  };

  const commit = (next: McqOption) => {
    const swapped = replaceOption(next);
    if (!swapped) return;
    const patched = patchParent({ options: swapped });
    if (patched) commitParent(patched);
  };

  const toggleCorrect = () => {
    if (!parent || !optionId) return;
    flush();
    const current = parent.correctOptionIds ?? [];
    const next = current.includes(optionId)
      ? current.filter((cid) => cid !== optionId)
      : [...current, optionId];
    const patched = patchParent({ correctOptionIds: next });
    if (patched) commitParent(patched);
  };

  const remove = () => {
    if (!parent || !optionId || !canRemove) return;
    flush();
    const nextOptions = options.filter((o) => o.id !== optionId);
    const nextCorrect = (parent.correctOptionIds ?? []).filter(
      (cid) => cid !== optionId,
    );
    const patched = patchParent({
      options: nextOptions,
      correctOptionIds: nextCorrect,
    });
    if (patched) commitParent(patched);
  };

  return {
    option,
    parent,
    schedule,
    commit,
    flush,
    index,
    isCorrect,
    canRemove,
    toggleCorrect,
    remove,
    syncedFromId,
    markSynced,
  };
};

/**
 * Question-scoped editor on top of `useElementEditor<McqQuestion>`. Owns
 * the genuinely *question-level* operations — prompt edits and appending
 * options. Per-option ops (text/image/color/remove/toggleCorrect) live on
 * `useMcqOptionEditor` so each option-card component owns its own commit
 * pipeline and there's no shared option-list state to fight over.
 *
 * Mutation semantics:
 *   - `schedulePrompt` is debounced (typing should feel responsive).
 *   - `addOption` commits immediately (structural changes are never
 *     debounced). The new option gets a fresh UUID and an empty text body
 *     so the renderer can show it the moment the round-trip lands.
 *
 * Bounds (`MIN_MCQ_OPTIONS` / `MAX_MCQ_OPTIONS`) are enforced inside the
 * hook — callers gate their UI on `canAddOption`, but a slipped call is a
 * no-op rather than a corrupted save.
 */
interface McqQuestionEditorApi {
  question: McqQuestion | undefined;
  schedulePrompt: (prompt: string) => void;
  flush: () => void;
  syncedFromId: string | undefined;
  markSynced: (id: string | undefined) => void;
  /** True when another option can be appended (below the max). */
  canAddOption: boolean;
  /** Append a blank option. Silent no-op at the max bound. */
  addOption: () => void;
  /** Sortable drop handler — reorders the option list and persists via the
   *  dedicated `moveMcqOption` endpoint (no whole-element write). */
  handleOptionDragEnd: (event: DragEndEvent) => void;
}

const useMcqQuestionEditor = (delay = 500): McqQuestionEditorApi => {
  const { deckId } = routeApi.useParams();
  const dispatch = useAppDispatch();
  const [moveMcqOption] = useMoveMcqOptionMutation();
  const {
    element: question,
    schedule,
    commit,
    flush,
    syncedFromId,
    markSynced,
  } = useElementEditor<McqQuestion>(isMcqQuestion, delay);

  const optionCount = question?.options?.length ?? 0;
  const canAddOption = optionCount < MAX_MCQ_OPTIONS;

  const schedulePrompt = (prompt: string) => {
    if (!question) return;
    schedule({ ...question, prompt });
  };

  const addOption = () => {
    if (!question || !canAddOption) return;
    flush();
    const nextOptions: McqOption[] = [
      ...(question.options ?? []),
      { id: crypto.randomUUID(), text: "" },
    ];
    commit({ ...question, options: nextOptions });
  };

  /**
   * Mirror of `useCreateDashboard.handleDragEnd` but for MCQ options. The
   * source carries the @dnd-kit-tracked indices; we splice the option list
   * in the deck cache for instant UI feedback, then call the dedicated
   * `moveMcqOption` endpoint so the server only sees the reorder (not a
   * full McqQuestion rewrite). Any pending debounced text edit is flushed
   * first so this reorder doesn't race a stale option-text save.
   */
  const handleOptionDragEnd = (event: DragEndEvent) => {
    if (!question?.id) return;
    const { source } = event.operation;
    if (!isSortable(source)) return;
    const { initialIndex, index } = source;
    if (initialIndex === index) return;
    const options = question.options ?? [];
    const moved = options[initialIndex];
    if (!moved.id) return;
    const movedId = moved.id;
    const elementId = question.id;

    flush();

    dispatch(
      BrainFlex.util.updateQueryData("getDeck", { id: deckId }, (draft) => {
        const el = draft.elements?.find((e) => e.id === elementId);
        if (el?.kind !== "McqQuestion" || !el.options) return;
        const [item] = el.options.splice(initialIndex, 1);
        el.options.splice(index, 0, item);
      }),
    );

    void moveMcqOption({
      id: deckId,
      elementId,
      optionId: movedId,
      to: index,
    })
      .unwrap()
      .catch((err: unknown) => {
        console.error("Failed to move MCQ option", err);
      });
  };

  return {
    question,
    schedulePrompt,
    flush,
    syncedFromId,
    markSynced,
    canAddOption,
    addOption,
    handleOptionDragEnd,
  };
};

export {
  useElementEditor,
  useMcqOptionEditor,
  useMcqQuestionEditor,
  MIN_MCQ_OPTIONS,
  MAX_MCQ_OPTIONS,
};
