/**
 * Author surface for a non-interactive Slide (title screen / section divider /
 * callout / content / end card). Captures slide kind, title, rich-text body,
 * and display seconds. Speaker notes live in the global drawer below the
 * canvas (see SpeakerNotesDrawer) so every element kind exposes them in the
 * same place. Media URLs (image/video/audio/background) are intentionally not
 * exposed yet — that requires the media-picker library.
 */
import { useState } from "react";
import { SlideContentWrapper } from "./SlideContentWrapper";
import { RichTextInput } from "@/components/Common/Input/RichTextInput/RichTextInput";
import { Input } from "@/components/Common/Input/Input/Input";
import { NumberInput } from "@/components/Common/Input/NumberInput/NumberInput";
import { Dropdown } from "@/components/Common/Input/Dropdown/Dropdown";
import { useElementEditor } from "./useElementEditor";
import type { Slide } from "@/store/BrainFlexApi";

const isSlide = (e: { kind: string }): e is Slide => e.kind === "Slide";

const SLIDE_KIND_OPTIONS: { value: Slide["slideKind"]; label: string }[] = [
  { value: "TITLE", label: "Title" },
  { value: "SECTION", label: "Section" },
  { value: "CALLOUT", label: "Callout" },
  { value: "CONTENT", label: "Content" },
  { value: "END", label: "End" },
];

const SlideContent = () => {
  const { element, schedule, flush, commit, syncedFromId, markSynced } =
    useElementEditor<Slide>(isSlide);

  const [title, setTitle] = useState<string>(element?.title ?? "");
  const [body, setBody] = useState<string>(element?.body ?? "");
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
    setDisplaySeconds(element.displaySeconds ?? 0);
    setSlideKind(element.slideKind ?? "CONTENT");
  }

  if (!element) {
    return (
      <SlideContentWrapper title='Slide'>
        <p>Select a slide to edit.</p>
      </SlideContentWrapper>
    );
  }

  const buildPatch = (overrides: Partial<Slide>): Slide => ({
    ...element,
    title,
    body,
    displaySeconds,
    slideKind,
    ...overrides,
  });

  return (
    <SlideContentWrapper
      title='Slide'
      description='A non-interactive screen — title, content, callout, or end card.'>
      <Dropdown
        label='Slide kind'
        id={`slide-kind-${element.id ?? ""}`}
        options={SLIDE_KIND_OPTIONS}
        value={[slideKind]}
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

      {/* TODO: Get more specs — background/image/video/audio fields once the
          media library is in place. */}
    </SlideContentWrapper>
  );
};

export { SlideContent };
