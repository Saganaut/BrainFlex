/**
 * Author surface for a non-interactive Slide (title screen / section divider /
 * callout / content / end card). Captures slide kind, title, rich-text body,
 * host notes, and display seconds. Media URLs (image/video/audio/background)
 * are intentionally not exposed yet — that requires the media-picker library.
 */
import { useState } from "react";
import { SlideContentWrapper } from "./SlideContentWrapper";
import { RichTextInput } from "@/components/Common/Input/RichTextInput";
import { useElementEditor } from "./useElementEditor";
import type { Slide } from "@/store/BrainFlexApi";
import styles from "./SlideContentTypes.module.css";

const isSlide = (e: { kind: string }): e is Slide => e.kind === "Slide";

const SLIDE_KINDS = ["TITLE", "SECTION", "CALLOUT", "CONTENT", "END"] as const;

const SlideContent = () => {
  const { element, schedule, flush, commit, syncedFromId, markSynced } =
    useElementEditor<Slide>(isSlide);

  const [title, setTitle] = useState<string>(element?.title ?? "");
  const [body, setBody] = useState<string>(element?.body ?? "");
  const [hostNotes, setHostNotes] = useState<string>(element?.hostNotes ?? "");
  const [displaySeconds, setDisplaySeconds] = useState<number>(
    element?.displaySeconds ?? 0,
  );
  const [slideKind, setSlideKind] = useState<Slide["slideKind"]>(
    element?.slideKind ?? "CONTENT",
  );

  if (element && syncedFromId !== element.id) {
    markSynced(element.id);
    setTitle(element.title ?? "");
    setBody(element.body ?? "");
    setHostNotes(element.hostNotes ?? "");
    setDisplaySeconds(element.displaySeconds ?? 0);
    setSlideKind(element.slideKind ?? "CONTENT");
  }

  if (!element) {
    return (
      <SlideContentWrapper>
        <p>Select a slide to edit.</p>
      </SlideContentWrapper>
    );
  }

  const buildPatch = (overrides: Partial<Slide>): Slide => ({
    ...element,
    title,
    body,
    hostNotes,
    displaySeconds,
    slideKind,
    ...overrides,
  });

  return (
    <SlideContentWrapper>
      <label className={styles.fieldLabel}>
        Slide kind
        <select
          className={styles.select}
          value={slideKind}
          onChange={(e) => {
            const next = e.target.value as Slide["slideKind"];
            setSlideKind(next);
            commit(buildPatch({ slideKind: next }));
          }}>
          {SLIDE_KINDS.map((k) => (
            <option key={k} value={k}>
              {k}
            </option>
          ))}
        </select>
      </label>

      <label className={styles.fieldLabel}>
        Title
        <input
          type='text'
          className={styles.textInput}
          value={title}
          placeholder='Slide title…'
          onChange={(e) => {
            const next = e.target.value;
            setTitle(next);
            schedule(buildPatch({ title: next }));
          }}
          onBlur={flush}
        />
      </label>

      <RichTextInput
        label='Body'
        id={`slide-body-${element.id ?? ""}`}
        placeholder='Slide content…'
        value={body}
        onChange={(html) => {
          setBody(html);
          schedule(buildPatch({ body: html }));
        }}
        onBlur={flush}
      />

      <label className={styles.fieldLabel}>
        Host notes
        <input
          type='text'
          className={styles.textInput}
          value={hostNotes}
          placeholder='Notes for the host (not shown to players)'
          onChange={(e) => {
            const next = e.target.value;
            setHostNotes(next);
            schedule(buildPatch({ hostNotes: next }));
          }}
          onBlur={flush}
        />
      </label>

      <label className={styles.fieldLabel}>
        Display seconds (0 = manual)
        <input
          type='number'
          min={0}
          className={styles.numberInput}
          value={displaySeconds}
          onChange={(e) => {
            const next = Number(e.target.value) || 0;
            setDisplaySeconds(next);
            schedule(buildPatch({ displaySeconds: next }));
          }}
          onBlur={flush}
        />
      </label>

      {/* TODO: Get more specs — background/image/video/audio fields once the
          media library is in place. */}
    </SlideContentWrapper>
  );
};

export { SlideContent };
