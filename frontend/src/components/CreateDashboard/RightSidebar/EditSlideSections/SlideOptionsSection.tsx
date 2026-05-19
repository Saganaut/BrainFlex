// Audience-and-display options for a Slide element. Carries the chrome that
// only Slides expose today — results display type, response gating, join
// info, free-form heading + rich participant note — plus the chunk-10
// `autoAdvanceSeconds` for unattended slideshow mode. The body of this file
// was lifted out of EditSlidePanel when that panel became a per-kind
// dispatcher; the same mirror/sync/commit pattern still applies.
import { useState } from "react";
import { Toggle } from "@/components/Common/Input/Toggle/Toggle";
import { Dropdown } from "@/components/Common/Input/Dropdown/Dropdown";
import { RadioGroup } from "@/components/Common/Input/RadioGroup/RadioGroup";
import { Input } from "@/components/Common/Input/Input/Input";
import { NumberInput } from "@/components/Common/Input/NumberInput/NumberInput";
import { RichTextInput } from "@/components/Common/Input/RichTextInput/RichTextInput";
import { useElementEditor } from "../../SlideContentTypes/useElementEditor";
import type { Slide } from "@/store/BrainFlexApi";
import styles from "../EditSlidePanel.module.css";

const isSlide = (e: { kind: string }): e is Slide => e.kind === "Slide";

const RESULTS_DISPLAY_OPTIONS: {
  value: NonNullable<Slide["resultsDisplayType"]>;
  label: string;
}[] = [
  { value: "DEFAULT", label: "Default" },
  { value: "HISTOGRAM", label: "Histogram" },
  { value: "PIE_CHART", label: "Pie chart" },
];

const JOIN_TYPE_OPTIONS: {
  value: NonNullable<Slide["joinType"]>;
  label: string;
}[] = [
  { value: "INSTRUCTIONS_BAR", label: "Instructions bar" },
  { value: "QR_CODE", label: "QR code" },
];

const SHOW_RESPONSES_OPTIONS: {
  value: NonNullable<Slide["showResponses"]>;
  label: string;
}[] = [
  { value: "INSTANT", label: "Instant" },
  { value: "ON_CLICK", label: "On click" },
  { value: "PRIVATE", label: "Private" },
];

const SlideOptionsSection = () => {
  const { element, schedule, flush, commit, syncedFromId, markSynced } =
    useElementEditor<Slide>(isSlide);

  const [resultsDisplayType, setResultsDisplayType] = useState<
    NonNullable<Slide["resultsDisplayType"]>
  >(element?.resultsDisplayType ?? "DEFAULT");
  const [multipleSelectionsEnabled, setMultipleSelectionsEnabled] =
    useState<boolean>(element?.multipleSelectionsEnabled ?? false);
  const [selectionsPerParticipant, setSelectionsPerParticipant] =
    useState<number>(element?.selectionsPerParticipant ?? 1);
  const [showResultsAsPercentage, setShowResultsAsPercentage] =
    useState<boolean>(element?.showResultsAsPercentage ?? false);
  const [joinType, setJoinType] = useState<NonNullable<Slide["joinType"]>>(
    element?.joinType ?? "INSTRUCTIONS_BAR",
  );
  const [showJoinInformation, setShowJoinInformation] = useState<boolean>(
    element?.showJoinInformation ?? true,
  );
  const [showResponses, setShowResponses] = useState<
    NonNullable<Slide["showResponses"]>
  >(element?.showResponses ?? "INSTANT");
  const [heading, setHeading] = useState<string>(element?.heading ?? "");
  // RichTextInput consumes/produces an HTML string today, but the backend
  // field is a TipTap/ProseMirror JSON doc. Until the picker round-trips JSON
  // we store the HTML in `participantInformation.html` as a one-key Map; a
  // later codegen pass will swap this for the proper TipTap shape.
  const [participantInformationHtml, setParticipantInformationHtml] =
    useState<string>(
      typeof element?.participantInformation?.html === "string"
        ? element.participantInformation.html
        : "",
    );
  // chunk 10: null means the host clicks Next manually.
  const [autoAdvanceEnabled, setAutoAdvanceEnabled] = useState<boolean>(
    element?.autoAdvanceSeconds !== undefined,
  );
  const [autoAdvanceSeconds, setAutoAdvanceSeconds] = useState<number>(
    element?.autoAdvanceSeconds ?? 30,
  );

  if (element && syncedFromId !== element.id) {
    markSynced(element.id);
    setResultsDisplayType(element.resultsDisplayType ?? "DEFAULT");
    setMultipleSelectionsEnabled(element.multipleSelectionsEnabled ?? false);
    setSelectionsPerParticipant(element.selectionsPerParticipant ?? 1);
    setShowResultsAsPercentage(element.showResultsAsPercentage ?? false);
    setJoinType(element.joinType ?? "INSTRUCTIONS_BAR");
    setShowJoinInformation(element.showJoinInformation ?? true);
    setShowResponses(element.showResponses ?? "INSTANT");
    setHeading(element.heading ?? "");
    setParticipantInformationHtml(
      typeof element.participantInformation?.html === "string"
        ? element.participantInformation.html
        : "",
    );
    setAutoAdvanceEnabled(element.autoAdvanceSeconds !== undefined);
    setAutoAdvanceSeconds(element.autoAdvanceSeconds ?? 30);
  }

  if (!element) return null;

  const buildPatch = (overrides: Partial<Slide>): Slide => ({
    ...element,
    resultsDisplayType,
    multipleSelectionsEnabled,
    selectionsPerParticipant,
    showResultsAsPercentage,
    joinType,
    showJoinInformation,
    showResponses,
    heading,
    participantInformation:
      participantInformationHtml === ""
        ? undefined
        : { html: participantInformationHtml },
    autoAdvanceSeconds: autoAdvanceEnabled ? autoAdvanceSeconds : undefined,
    ...overrides,
  });

  const elId = element.id ?? "";

  return (
    <>
      <section className={styles.section}>
        <h4 className={styles.heading}>Results</h4>
        <Dropdown
          label='Results display type'
          id={`slide-results-${elId}`}
          options={RESULTS_DISPLAY_OPTIONS}
          value={[resultsDisplayType]}
          onChange={(values) => {
            const next =
              (values[0] as Slide["resultsDisplayType"]) ?? "DEFAULT";
            setResultsDisplayType(next);
            commit(buildPatch({ resultsDisplayType: next }));
          }}
        />
        <Toggle
          id={`slide-percent-${elId}`}
          label='Show results as percentage'
          checked={showResultsAsPercentage}
          onChange={(e) => {
            const next = e.currentTarget.checked;
            setShowResultsAsPercentage(next);
            commit(buildPatch({ showResultsAsPercentage: next }));
          }}
        />
      </section>

      <section className={styles.section}>
        <h4 className={styles.heading}>Responses</h4>
        <Toggle
          id={`slide-multi-${elId}`}
          label='Allow multiple selections'
          checked={multipleSelectionsEnabled}
          onChange={(e) => {
            const next = e.currentTarget.checked;
            setMultipleSelectionsEnabled(next);
            commit(buildPatch({ multipleSelectionsEnabled: next }));
          }}
        />
        {multipleSelectionsEnabled && (
          <NumberInput
            label='Selections per participant'
            id={`slide-multi-count-${elId}`}
            min={1}
            value={selectionsPerParticipant}
            onChange={(next) => {
              setSelectionsPerParticipant(next);
              schedule(buildPatch({ selectionsPerParticipant: next }));
            }}
            onBlur={flush}
          />
        )}
        <RadioGroup
          name={`slide-show-responses-${elId}`}
          legend='Show responses'
          options={SHOW_RESPONSES_OPTIONS}
          value={showResponses}
          onChange={(value) => {
            const next = value as Slide["showResponses"];
            setShowResponses(next);
            commit(buildPatch({ showResponses: next }));
          }}
        />
      </section>

      <section className={styles.section}>
        <h4 className={styles.heading}>Pacing</h4>
        <Toggle
          id={`slide-auto-advance-${elId}`}
          label='Auto-advance after a fixed delay'
          checked={autoAdvanceEnabled}
          onChange={(e) => {
            const next = e.currentTarget.checked;
            setAutoAdvanceEnabled(next);
            commit(
              buildPatch({
                autoAdvanceSeconds: next ? autoAdvanceSeconds : undefined,
              }),
            );
          }}
        />
        {autoAdvanceEnabled && (
          <NumberInput
            id={`slide-auto-advance-seconds-${elId}`}
            label='Seconds before advancing'
            min={1}
            max={600}
            value={autoAdvanceSeconds}
            onChange={(next) => {
              setAutoAdvanceSeconds(next);
              schedule(buildPatch({ autoAdvanceSeconds: next }));
            }}
            onBlur={flush}
          />
        )}
      </section>

      <section className={styles.section}>
        <h4 className={styles.heading}>Join</h4>
        <Dropdown
          label='Join type'
          id={`slide-join-${elId}`}
          options={JOIN_TYPE_OPTIONS}
          value={[joinType]}
          onChange={(values) => {
            const next =
              (values[0] as Slide["joinType"]) ?? "INSTRUCTIONS_BAR";
            setJoinType(next);
            commit(buildPatch({ joinType: next }));
          }}
        />
        <Toggle
          id={`slide-show-join-${elId}`}
          label='Show join information'
          checked={showJoinInformation}
          onChange={(e) => {
            const next = e.currentTarget.checked;
            setShowJoinInformation(next);
            commit(buildPatch({ showJoinInformation: next }));
          }}
        />
      </section>

      <section className={styles.section}>
        <h4 className={styles.heading}>Labels</h4>
        <Input
          label='Heading'
          id={`slide-heading-${elId}`}
          type='text'
          value={heading}
          placeholder='Slide heading…'
          onChange={(e) => {
            const next = e.target.value;
            setHeading(next);
            schedule(buildPatch({ heading: next }));
          }}
          onBlur={flush}
        />
        <RichTextInput
          label='Information for participants'
          id={`slide-participant-info-${elId}`}
          placeholder='What participants should know before answering…'
          value={participantInformationHtml}
          onChange={(html) => {
            setParticipantInformationHtml(html);
            schedule(
              buildPatch({
                participantInformation:
                  html === "" ? undefined : { html },
              }),
            );
          }}
          onBlur={flush}
        />
      </section>
    </>
  );
};

export { SlideOptionsSection };
