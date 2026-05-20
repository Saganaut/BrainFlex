/**
 * Author surface for a non-interactive Slide (title screen / section divider /
 * callout / content / end card). Captures slide kind, title, the block-stacked
 * body content, and display seconds. Speaker notes live in the global drawer
 * below the canvas; pacing (auto-advance) lives in the right-sidebar Slide
 * options section.
 *
 * Block stack (chunk 10c): replaces the single rich-text body with a list of
 * SlideBlocks (heading / body / bullet-list / image / callout). Each block
 * has its own per-kind editor; reorder via up/down arrows, remove via the
 * trash icon, append via the "Add block" dropdown at the bottom. Migration
 * is graceful: when the server still returns `body` but no `blocks`, the
 * first commit wraps the legacy body into a single BodyBlock and clears
 * `body`, so re-opening the editor shows the canonical shape.
 *
 * Audio / video / embed pickers live below the block list — each one persists
 * a MediaAsset id (audioAssetId / videoAssetId) and the inline preview
 * confirms the selection. The image / background slots still wait on the
 * media-library v2 migration of legacy gallery_images.
 */
import { useState } from "react";
import {
  ChevronDownIcon,
  ChevronUpIcon,
  MinusIcon,
  PlusIcon,
  TrashIcon,
} from "@heroicons/react/24/outline";
import { SlideContentWrapper } from "./SlideContentWrapper";
import { RichTextInput } from "@/components/Common/Input/RichTextInput/RichTextInput";
import { Input } from "@/components/Common/Input/Input/Input";
import { NumberInput } from "@/components/Common/Input/NumberInput/NumberInput";
import { Dropdown } from "@/components/Common/Input/Dropdown/Dropdown";
import { Btn } from "@/components/Common/Buttons/Btn";
import { IconBtn } from "@/components/Common/Buttons/IconBtn";
import { MediaAssetChip } from "@/components/Common/MediaPicker/MediaAssetChip";
import { useMediaPicker } from "@/hooks/useMediaPicker";
import { largestUrl } from "@/utils/image";
import { useElementEditor } from "./useElementEditor";
import type { Image, Slide } from "@/store/BrainFlexApi";
import {
  createSlideBlock,
  narrowSlideBlock,
  type BodyBlock,
  type BulletListBlock,
  type CalloutBlock,
  type CalloutTone,
  type HeadingBlock,
  type ImageBlock,
  type SlideBlockKind,
  type SlideBlockUnion,
} from "@/store/slideBlockTypes";
import sharedStyles from "./SlideContentTypes.module.css";
import styles from "./SlideContent.module.css";

const isSlide = (e: { kind: string }): e is Slide => e.kind === "Slide";

const SLIDE_KIND_OPTIONS: { value: NonNullable<Slide["slideKind"]>; label: string }[] = [
  { value: "TITLE", label: "Title" },
  { value: "SECTION", label: "Section" },
  { value: "CALLOUT", label: "Callout" },
  { value: "CONTENT", label: "Content" },
  { value: "END", label: "End" },
];

const BLOCK_KIND_OPTIONS: { value: SlideBlockKind; label: string }[] = [
  { value: "HeadingBlock", label: "Heading" },
  { value: "BodyBlock", label: "Body text" },
  { value: "BulletListBlock", label: "Bullet list" },
  { value: "ImageBlock", label: "Image" },
  { value: "CalloutBlock", label: "Callout" },
];

const BLOCK_KIND_LABEL: Record<SlideBlockKind, string> = {
  HeadingBlock: "Heading",
  BodyBlock: "Body text",
  BulletListBlock: "Bullet list",
  ImageBlock: "Image",
  CalloutBlock: "Callout",
};

const HEADING_LEVELS: { value: string; label: string }[] = [
  { value: "1", label: "H1 — large" },
  { value: "2", label: "H2 — medium" },
  { value: "3", label: "H3 — small" },
];

const CALLOUT_TONES: { value: CalloutTone; label: string }[] = [
  { value: "INFO", label: "Info" },
  { value: "WARN", label: "Warn" },
  { value: "SUCCESS", label: "Success" },
];

/** Hydrate the cached `blocks` (typed `SlideBlock[]` from codegen) into the
 *  typed discriminated union, dropping any blocks whose `kind` we don't know
 *  about — they'll be filtered until the codegen catches up. */
const hydrateBlocks = (raw: SlideBlock[] | undefined): SlideBlockUnion[] => {
  if (!raw) return [];
  return raw
    .map(narrowSlideBlock)
    .filter((b): b is SlideBlockUnion => b !== null);
};

/** Migration glue: when the server still returns the legacy `body` string but
 *  has no `blocks`, surface it as a single BodyBlock so the editor renders
 *  consistently. The next commit replaces both fields with the canonical
 *  block list. */
const initialBlocksFromSlide = (slide: Slide): SlideBlockUnion[] => {
  const fromBlocks = hydrateBlocks(slide.blocks);
  if (fromBlocks.length > 0) return fromBlocks;
  if (slide.body && slide.body.trim().length > 0) {
    return [
      {
        kind: "BodyBlock",
        id: `legacy-body-${slide.id ?? "anon"}`,
        richBody: slide.body,
      },
    ];
  }
  return [];
};

const SlideContent = () => {
  const { element, schedule, flush, commit, syncedFromId, markSynced } =
    useElementEditor<Slide>(isSlide);
  const openMediaPicker = useMediaPicker();

  const [title, setTitle] = useState<string>(element?.title ?? "");
  const [displaySeconds, setDisplaySeconds] = useState<number>(
    element?.displaySeconds ?? 0,
  );
  const [slideKind, setSlideKind] = useState<Slide["slideKind"]>(
    element?.slideKind ?? "CONTENT",
  );
  const [blocks, setBlocks] = useState<SlideBlockUnion[]>(() =>
    element ? initialBlocksFromSlide(element) : [],
  );

  if (element && syncedFromId !== element.id) {
    markSynced(element.id);
    setTitle(element.title ?? "");
    setDisplaySeconds(element.displaySeconds ?? 0);
    setSlideKind(element.slideKind ?? "CONTENT");
    setBlocks(initialBlocksFromSlide(element));
  }

  if (!element) {
    return (
      <SlideContentWrapper title='Slide'>
        <p>Select a slide to edit.</p>
      </SlideContentWrapper>
    );
  }

  /** Every patch clears the legacy `body` field — once the author has touched
   *  the block list, the slide is fully migrated and we never want the server
   *  to fall back to `body` again. */
  const buildPatch = (overrides: {
    title?: string;
    blocks?: SlideBlockUnion[];
    displaySeconds?: number;
    slideKind?: Slide["slideKind"];
    audioAssetId?: Slide["audioAssetId"];
    audioUrl?: Slide["audioUrl"];
    videoAssetId?: Slide["videoAssetId"];
    videoUrl?: Slide["videoUrl"];
  }): Slide => ({
    ...element,
    title: overrides.title ?? title,
    body: "",
    blocks: overrides.blocks ?? blocks,
    displaySeconds: overrides.displaySeconds ?? displaySeconds,
    slideKind: overrides.slideKind ?? slideKind,
    audioAssetId:
      "audioAssetId" in overrides ? overrides.audioAssetId : element.audioAssetId,
    audioUrl: "audioUrl" in overrides ? overrides.audioUrl : element.audioUrl,
    videoAssetId:
      "videoAssetId" in overrides ? overrides.videoAssetId : element.videoAssetId,
    videoUrl: "videoUrl" in overrides ? overrides.videoUrl : element.videoUrl,
  });

  const commitBlocks = (next: SlideBlockUnion[]) => {
    setBlocks(next);
    commit(buildPatch({ blocks: next }));
  };

  const scheduleBlocks = (next: SlideBlockUnion[]) => {
    setBlocks(next);
    schedule(buildPatch({ blocks: next }));
  };

  const handleAddBlock = (kind: SlideBlockKind) => {
    flush();
    commitBlocks([...blocks, createSlideBlock(kind)]);
  };

  const handleRemoveBlock = (id: string) => {
    flush();
    commitBlocks(blocks.filter((b) => b.id !== id));
  };

  const handleMoveBlock = (id: string, direction: -1 | 1) => {
    const idx = blocks.findIndex((b) => b.id === id);
    if (idx < 0) return;
    const target = idx + direction;
    if (target < 0 || target >= blocks.length) return;
    flush();
    const next = [...blocks];
    [next[idx], next[target]] = [next[target], next[idx]];
    commitBlocks(next);
  };

  const handleUpdateBlock = (
    next: SlideBlockUnion,
    mode: "schedule" | "commit",
  ) => {
    const updated = blocks.map((b) => (b.id === next.id ? next : b));
    if (mode === "commit") commitBlocks(updated);
    else scheduleBlocks(updated);
  };

  return (
    <SlideContentWrapper
      title='Slide'
      description='A non-interactive screen — title, content, callout, or end card.'>
      <Dropdown
        label='Slide kind'
        id={`slide-kind-${element.id ?? ""}`}
        options={SLIDE_KIND_OPTIONS}
        value={slideKind ? [slideKind] : []}
        onChange={(values) => {
          const next = (values[0] ?? "CONTENT") as Slide["slideKind"];
          setSlideKind(next);
          commit(buildPatch({ slideKind: next }));
        }}
      />

      <Input
        label='Title'
        id={`slide-title-${element.id ?? ""}`}
        type='text'
        value={title}
        placeholder='Slide title…'
        onChange={(e) => {
          const next = e.target.value;
          setTitle(next);
          schedule(buildPatch({ title: next }));
        }}
        onBlur={flush}
      />

      <div className={styles.blocksHeader}>
        <span className={styles.blocksHeaderLabel}>Content blocks</span>
        <BlockAdder onAdd={handleAddBlock} elementId={element.id ?? ""} />
      </div>

      {blocks.length === 0 ? (
        <p className={styles.emptyBlocks}>
          No content yet. Add a heading, body text, bullet list, image, or callout.
        </p>
      ) : (
        <div className={styles.blockList}>
          {blocks.map((block, idx) => (
            <BlockCard
              key={block.id}
              block={block}
              index={idx}
              total={blocks.length}
              onRemove={handleRemoveBlock}
              onMove={handleMoveBlock}
              onUpdate={handleUpdateBlock}
              onFlush={flush}
            />
          ))}
        </div>
      )}

      <NumberInput
        label='Display seconds (0 = manual)'
        id={`slide-display-seconds-${element.id ?? ""}`}
        min={0}
        value={displaySeconds}
        onChange={(next) => {
          setDisplaySeconds(next);
          schedule(buildPatch({ displaySeconds: next }));
        }}
        onBlur={flush}
      />

      <MediaAssetChip
        label='Audio'
        assetId={element.audioAssetId}
        onReplace={() => {
          flush();
          openMediaPicker("AUDIO", (asset) => {
            commit(buildPatch({ audioAssetId: asset.id, audioUrl: undefined }));
          });
        }}
        onRemove={() => {
          flush();
          commit(buildPatch({ audioAssetId: undefined }));
        }}
      />
      {!element.audioAssetId && (
        <Btn
          size='sm'
          onClick={() => {
            flush();
            openMediaPicker("AUDIO", (asset) => {
              commit(buildPatch({ audioAssetId: asset.id, audioUrl: undefined }));
            });
          }}>
          Add audio
        </Btn>
      )}

      <MediaAssetChip
        label='Video'
        assetId={element.videoAssetId}
        onReplace={() => {
          flush();
          openMediaPicker("VIDEO_FILE", (asset) => {
            commit(buildPatch({ videoAssetId: asset.id, videoUrl: undefined }));
          });
        }}
        onRemove={() => {
          flush();
          commit(buildPatch({ videoAssetId: undefined }));
        }}
      />
      {!element.videoAssetId && (
        <div style={{ display: "flex", gap: "var(--space-2)" }}>
          <Btn
            size='sm'
            onClick={() => {
              flush();
              openMediaPicker("VIDEO_FILE", (asset) => {
                commit(
                  buildPatch({ videoAssetId: asset.id, videoUrl: undefined }),
                );
              });
            }}>
            Add video
          </Btn>
          <Btn
            size='sm'
            onClick={() => {
              flush();
              openMediaPicker("VIDEO_EMBED", (asset) => {
                commit(
                  buildPatch({ videoAssetId: asset.id, videoUrl: undefined }),
                );
              });
            }}>
            Add video link
          </Btn>
        </div>
      )}
    </SlideContentWrapper>
  );
};

interface BlockAdderProps {
  elementId: string;
  onAdd: (kind: SlideBlockKind) => void;
}

/** "Add block" affordance: a Dropdown of kinds plus a confirm button. The
 *  dropdown holds local state for the picked kind so the user can preview
 *  the label before adding. */
const BlockAdder = ({ elementId, onAdd }: BlockAdderProps) => {
  const [picked, setPicked] = useState<SlideBlockKind>("BodyBlock");
  return (
    <div className={styles.blocksHeaderActions}>
      <Dropdown
        id={`slide-add-block-${elementId}`}
        options={BLOCK_KIND_OPTIONS}
        value={[picked]}
        onChange={(values) => {
          const next = values[0] as SlideBlockKind | undefined;
          if (next) setPicked(next);
        }}
      />
      <Btn size='sm' onClick={() => { onAdd(picked); }}>
        <PlusIcon style={{ width: 14, height: 14 }} />
        Add block
      </Btn>
    </div>
  );
};

interface BlockCardProps {
  block: SlideBlockUnion;
  index: number;
  total: number;
  onRemove: (id: string) => void;
  onMove: (id: string, direction: -1 | 1) => void;
  onUpdate: (next: SlideBlockUnion, mode: "schedule" | "commit") => void;
  onFlush: () => void;
}

const BlockCard = ({
  block,
  index,
  total,
  onRemove,
  onMove,
  onUpdate,
  onFlush,
}: BlockCardProps) => {
  return (
    <div className={styles.blockCard}>
      <div className={styles.blockHeader}>
        <span className={styles.blockHeaderLabel}>
          {BLOCK_KIND_LABEL[block.kind]} · #{(index + 1).toString()}
        </span>
        <div className={styles.blockHeaderActions}>
          <IconBtn
            fill='bordered'
            size='xs'
            icon={<ChevronUpIcon />}
            aria-label='Move block up'
            disabled={index === 0}
            onClick={() => { onMove(block.id, -1); }}
          />
          <IconBtn
            fill='bordered'
            size='xs'
            icon={<ChevronDownIcon />}
            aria-label='Move block down'
            disabled={index === total - 1}
            onClick={() => { onMove(block.id, 1); }}
          />
          <IconBtn
            fill='bordered'
            size='xs'
            icon={<TrashIcon />}
            aria-label='Remove block'
            onClick={() => { onRemove(block.id); }}
          />
        </div>
      </div>
      <div className={styles.blockBody}>
        <BlockBody block={block} onUpdate={onUpdate} onFlush={onFlush} />
      </div>
    </div>
  );
};

interface BlockBodyProps {
  block: SlideBlockUnion;
  onUpdate: (next: SlideBlockUnion, mode: "schedule" | "commit") => void;
  onFlush: () => void;
}

const BlockBody = ({ block, onUpdate, onFlush }: BlockBodyProps) => {
  switch (block.kind) {
    case "HeadingBlock":
      return (
        <HeadingBlockEditor
          block={block}
          onUpdate={onUpdate}
          onFlush={onFlush}
        />
      );
    case "BodyBlock":
      return (
        <BodyBlockEditor block={block} onUpdate={onUpdate} onFlush={onFlush} />
      );
    case "BulletListBlock":
      return (
        <BulletListBlockEditor
          block={block}
          onUpdate={onUpdate}
          onFlush={onFlush}
        />
      );
    case "ImageBlock":
      return (
        <ImageBlockEditor block={block} onUpdate={onUpdate} onFlush={onFlush} />
      );
    case "CalloutBlock":
      return (
        <CalloutBlockEditor
          block={block}
          onUpdate={onUpdate}
          onFlush={onFlush}
        />
      );
  }
};

interface HeadingEditorProps {
  block: HeadingBlock;
  onUpdate: (next: SlideBlockUnion, mode: "schedule" | "commit") => void;
  onFlush: () => void;
}

const HeadingBlockEditor = ({
  block,
  onUpdate,
  onFlush,
}: HeadingEditorProps) => {
  return (
    <div className={sharedStyles.fieldRow}>
      <div style={{ flex: 1, minWidth: 0 }}>
        <Input
          label='Heading text'
          id={`heading-text-${block.id}`}
          type='text'
          fullWidth
          value={block.text ?? ""}
          placeholder='Section heading…'
          onChange={(e) => {
            onUpdate({ ...block, text: e.target.value }, "schedule");
          }}
          onBlur={onFlush}
        />
      </div>
      <Dropdown
        id={`heading-level-${block.id}`}
        label='Level'
        options={HEADING_LEVELS}
        value={[(block.level ?? 2).toString()]}
        onChange={(values) => {
          const next = parseInt(values[0] ?? "2", 10);
          onUpdate({ ...block, level: next }, "commit");
        }}
      />
    </div>
  );
};

interface BodyEditorProps {
  block: BodyBlock;
  onUpdate: (next: SlideBlockUnion, mode: "schedule" | "commit") => void;
  onFlush: () => void;
}

const BodyBlockEditor = ({ block, onUpdate, onFlush }: BodyEditorProps) => {
  return (
    <RichTextInput
      label='Body'
      id={`body-rich-${block.id}`}
      placeholder='Slide content…'
      value={block.richBody ?? ""}
      onChange={(html) => {
        onUpdate({ ...block, richBody: html }, "schedule");
      }}
      onBlur={onFlush}
    />
  );
};

interface BulletEditorProps {
  block: BulletListBlock;
  onUpdate: (next: SlideBlockUnion, mode: "schedule" | "commit") => void;
  onFlush: () => void;
}

const MIN_BULLET_ITEMS = 1;
const MAX_BULLET_ITEMS = 12;

const BulletListBlockEditor = ({
  block,
  onUpdate,
  onFlush,
}: BulletEditorProps) => {
  const items = block.items ?? [];
  const updateItems = (
    nextItems: string[],
    mode: "schedule" | "commit" = "schedule",
  ) => {
    onUpdate({ ...block, items: nextItems }, mode);
  };

  return (
    <div className={sharedStyles.section}>
      {items.map((item, idx) => (
        <div key={`${block.id}-${idx.toString()}`} className={styles.bulletRow}>
          <div className={styles.bulletRowField}>
            <Input
              type='text'
              fullWidth
              value={item}
              placeholder={`Bullet ${(idx + 1).toString()}`}
              onChange={(e) => {
                const next = [...items];
                next[idx] = e.target.value;
                updateItems(next);
              }}
              onBlur={onFlush}
            />
          </div>
          <IconBtn
            fill='bordered'
            size='xs'
            icon={<MinusIcon />}
            aria-label={`Remove bullet ${(idx + 1).toString()}`}
            disabled={items.length <= MIN_BULLET_ITEMS}
            onClick={() => {
              const next = items.filter((_, i) => i !== idx);
              updateItems(next, "commit");
            }}
          />
        </div>
      ))}
      <Btn
        size='sm'
        disabled={items.length >= MAX_BULLET_ITEMS}
        onClick={() => { updateItems([...items, ""], "commit"); }}>
        + Add bullet
      </Btn>
    </div>
  );
};

interface ImageEditorProps {
  block: ImageBlock;
  onUpdate: (next: SlideBlockUnion, mode: "schedule" | "commit") => void;
  onFlush: () => void;
}

/** Materialize an external Image record from a typed URL. Mirrors the shape
 *  the backend's `Image.external(url)` factory produces (single ORIGINAL
 *  variant with the URL set) so the codegen `Image` type stays consistent. */
const makeExternalImage = (url: string): Image => ({
  useExternalImg: true,
  variants: [{ size: "ORIGINAL", url }],
});

const ImageBlockEditor = ({
  block,
  onUpdate,
  onFlush,
}: ImageEditorProps) => {
  const externalUrl: string =
    block.image?.useExternalImg ? (largestUrl(block.image, "") ?? "") : "";
  return (
    <div className={sharedStyles.section}>
      <Input
        label='Image URL'
        id={`image-url-${block.id}`}
        type='text'
        fullWidth
        value={externalUrl}
        placeholder='https://… (gallery picker coming with media library v2)'
        onChange={(e) => {
          const url = e.target.value.trim();
          onUpdate(
            {
              ...block,
              image: url ? makeExternalImage(url) : undefined,
            },
            "schedule",
          );
        }}
        onBlur={onFlush}
      />
      {externalUrl && (
        <img
          src={externalUrl}
          alt={block.caption ?? ""}
          className={sharedStyles.imagePlaceholder}
        />
      )}
      <Input
        label='Caption'
        id={`image-caption-${block.id}`}
        type='text'
        fullWidth
        value={block.caption ?? ""}
        placeholder='Optional caption shown under the image'
        onChange={(e) => {
          onUpdate({ ...block, caption: e.target.value }, "schedule");
        }}
        onBlur={onFlush}
      />
    </div>
  );
};

interface CalloutEditorProps {
  block: CalloutBlock;
  onUpdate: (next: SlideBlockUnion, mode: "schedule" | "commit") => void;
  onFlush: () => void;
}

const CalloutBlockEditor = ({
  block,
  onUpdate,
  onFlush,
}: CalloutEditorProps) => {
  return (
    <div className={sharedStyles.section}>
      <Dropdown
        id={`callout-tone-${block.id}`}
        label='Tone'
        options={CALLOUT_TONES}
        value={[block.tone ?? "INFO"]}
        onChange={(values) => {
          const next = (values[0] ?? "INFO") as CalloutTone;
          onUpdate({ ...block, tone: next }, "commit");
        }}
      />
      <RichTextInput
        label='Callout body'
        id={`callout-body-${block.id}`}
        placeholder='Short, tinted text…'
        value={block.richBody ?? ""}
        onChange={(html) => {
          onUpdate({ ...block, richBody: html }, "schedule");
        }}
        onBlur={onFlush}
      />
    </div>
  );
};

export { SlideContent };
