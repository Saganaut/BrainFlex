/**
 * Author surface for a ranking question.
 *
 * Players are asked to order a list of items; the scoring uses
 * `correctOrder` (a list of item ids in the intended sequence). For now the
 * authoring UI lets you add/remove items and edit their labels, and treats
 * the on-screen order as the correct order — drag-to-reorder is a later pass.
 */
import { useState } from "react";
import { MinusIcon } from "@heroicons/react/24/outline";
import { SlideContentWrapper } from "./SlideContentWrapper";
import { RichTextInput } from "@/components/Common/Input/RichTextInput";
import { Input } from "@/components/Common/Input/Input";
import { useElementEditor } from "./useElementEditor";
import { Btn } from "@/components/Common/Buttons/Btn";
import { IconBtn } from "@/components/Common/Buttons/IconBtn";
import type { RankingItem, RankingQuestion } from "@/store/BrainFlexApi";
import styles from "./SlideContentTypes.module.css";

const isRanking = (e: { kind: string }): e is RankingQuestion =>
  e.kind === "RankingQuestion";

const MIN_ITEMS = 2;
const MAX_ITEMS = 8;

const RankingSlideContent = () => {
  const { element, schedule, flush, commit, syncedFromId, markSynced } =
    useElementEditor<RankingQuestion>(isRanking);

  const [prompt, setPrompt] = useState(element?.prompt ?? "");
  const [items, setItems] = useState<RankingItem[]>(element?.items ?? []);

  if (element && syncedFromId !== element.id) {
    markSynced(element.id);
    setPrompt(element.prompt ?? "");
    setItems(element.items ?? []);
  }

  if (!element) {
    return (
      <SlideContentWrapper>
        <p>Select a slide to edit.</p>
      </SlideContentWrapper>
    );
  }

  // The on-screen order is treated as the correct order until drag-to-reorder
  // ships — keeps the backend's correctOrder in lock-step with `items`.
  const buildPatch = (overrides: {
    prompt?: string;
    items?: RankingItem[];
  }): RankingQuestion => {
    const nextItems = overrides.items ?? items;
    return {
      ...element,
      prompt: overrides.prompt ?? prompt,
      items: nextItems,
      correctOrder: nextItems
        .map((i) => i.id)
        .filter((id): id is string => id !== undefined),
    };
  };

  const handleAddItem = () => {
    if (items.length >= MAX_ITEMS) return;
    flush();
    const next = [...items, { id: crypto.randomUUID(), label: "" }];
    setItems(next);
    commit(buildPatch({ items: next }));
  };

  const handleRemoveItem = (id: string) => {
    if (items.length <= MIN_ITEMS) return;
    flush();
    const next = items.filter((i) => i.id !== id);
    setItems(next);
    commit(buildPatch({ items: next }));
  };

  const handleItemLabelChange = (id: string, label: string) => {
    const next = items.map((i) => (i.id === id ? { ...i, label } : i));
    setItems(next);
    schedule(buildPatch({ items: next }));
  };

  return (
    <SlideContentWrapper>
      <RichTextInput
        label='Question'
        id={`rank-prompt-${element.id ?? ""}`}
        placeholder='How should the player rank these?'
        value={prompt}
        onChange={(html) => {
          setPrompt(html);
          schedule(buildPatch({ prompt: html }));
        }}
        onBlur={flush}
      />

      <div className={styles.sectionHeader}>
        <span className={styles.sectionLabel}>
          Items (top → bottom is the correct order)
        </span>
        <Btn
          size='sm'
          onClick={handleAddItem}
          disabled={items.length >= MAX_ITEMS}>
          + Add item
        </Btn>
      </div>

      <div className={styles.itemList}>
        {items.map((item, idx) => (
          <div key={item.id} className={styles.itemRow}>
            <span>{idx + 1}.</span>
            <div className={styles.itemRowField}>
              <Input
                type='text'
                fullWidth
                value={item.label ?? ""}
                placeholder={`Item ${(idx + 1).toString()}`}
                onChange={(e) => {
                  if (item.id) handleItemLabelChange(item.id, e.target.value);
                }}
                onBlur={flush}
              />
            </div>
            <IconBtn
              type='default'
              size='xs'
              bordered
              icon={<MinusIcon />}
              aria-label={`Remove item ${(idx + 1).toString()}`}
              disabled={items.length <= MIN_ITEMS}
              onClick={() => {
                if (item.id) handleRemoveItem(item.id);
              }}
            />
          </div>
        ))}
      </div>

      {/* TODO: Get more specs — drag-to-reorder, item images (Lorem Picsum
          placeholder + media library), per-item scoring weights, partial
          credit configuration (q.scoring), best-answer modifier. */}
    </SlideContentWrapper>
  );
};

export { RankingSlideContent };
