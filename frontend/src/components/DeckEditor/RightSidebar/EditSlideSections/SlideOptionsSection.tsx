// Audience-and-display options for a Slide element. Carries the chrome that
// only Slides expose today — results display type, response gating, join
// info, free-form heading + rich participant note — plus the chunk-10
// `autoAdvanceSeconds` for unattended slideshow mode. The body of this file
// was lifted out of EditSlidePanel when that panel became a per-kind
// dispatcher; the same mirror/sync/commit pattern still applies.
import { useState } from "react";
import {
  ChartBarIcon,
  ChartPieIcon,
  HashtagIcon,
} from "@heroicons/react/24/outline";
import { Toggle } from "@/components/Common/Input/Toggle/Toggle";
import { RadioGroup } from "@/components/Common/Input/RadioGroup/RadioGroup";
import { Input } from "@/components/Common/Input/Input/Input";
import { NumberInput } from "@/components/Common/Input/NumberInput/NumberInput";
import { RichTextInput } from "@/components/Common/Input/RichTextInput/RichTextInput";
import { Tooltip } from "@/components/Common/Tooltip/Tooltip";
import { useElementEditor } from "../../SlideContentTypes/useElementEditor";
import type { Slide } from "@/store/BrainFlexApi";
import styles from "../EditSlidePanel.module.css";

const isSlide = (e: { kind: string }): e is Slide => e.kind === "Slide";

type ResultsDisplayValue = NonNullable<Slide["resultsDisplayType"]>;

const CHART_OPTIONS: {
  value: Exclude<ResultsDisplayValue, "DEFAULT">;
  label: string;
  Icon: typeof ChartBarIcon;
  rotated?: boolean;
}[] = [
  { value: "BAR_HORIZONTAL", label: "Horizontal bars", Icon: ChartBarIcon, rotated: true },
  { value: "BAR_VERTICAL", label: "Vertical bars", Icon: ChartBarIcon },
  { value: "WORD_CLOUD", label: "Word cloud", Icon: HashtagIcon },
  { value: "PIE_CHART", label: "Pie chart", Icon: ChartPieIcon },
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
  // chunk 21 — joinType is the legacy single-source enum. New documents carry
  // showJoinInformation and showQrCode independently; older documents fall
  // back to deriving showQrCode from joinType === "QR_CODE".
  const [showJoinInformation, setShowJoinInformation] = useState<boolean>(
    element?.showJoinInformation ?? true,
  );
  const [showQrCode, setShowQrCode] = useState<boolean>(
    element?.showQrCode ?? element?.joinType === "QR_CODE",
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
    setShowJoinInformation(element.showJoinInformation ?? true);
    setShowQrCode(element.showQrCode ?? element.joinType === "QR_CODE");
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
    showJoinInformation,
    showQrCode,
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
        <div
          className={styles.chartPicker}
          role='radiogroup'
          aria-label='Results display type'>
          {CHART_OPTIONS.map(({ value, label, Icon, rotated }) => {
            // Legacy "HISTOGRAM" documents render as BAR_VERTICAL — the
            // backend kept the enum value for read-compat only.
            const normalized: ResultsDisplayValue =
              resultsDisplayType === "HISTOGRAM"
                ? "BAR_VERTICAL"
                : resultsDisplayType;
            const isActive = normalized === value;
            return (
              <Tooltip key={value} label={label}>
                <button
                  type='button'
                  role='radio'
                  aria-checked={isActive}
                  aria-label={label}
                  className={[
                    styles.chartButton,
                    isActive ? styles.chartButtonActive : "",
                  ]
                    .filter(Boolean)
                    .join(" ")}
                  onClick={() => {
                    // Clicking the active button clears back to DEFAULT so the
                    // host inherits the kind's built-in viz.
                    const next: ResultsDisplayValue = isActive
                      ? "DEFAULT"
                      : value;
                    setResultsDisplayType(next);
                    commit(buildPatch({ resultsDisplayType: next }));
                  }}>
                  <Icon
                    className={[
                      styles.chartIcon,
                      rotated === true ? styles.chartIconRotated : "",
                    ]
                      .filter(Boolean)
                      .join(" ")}
                    aria-hidden='true'
                  />
                </button>
              </Tooltip>
            );
          })}
        </div>
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
            min={0}
            value={selectionsPerParticipant}
            infoMessage='0 means unlimited'
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
        <Toggle
          id={`slide-show-qr-${elId}`}
          label='Display QR code'
          checked={showQrCode}
          onChange={(e) => {
            const next = e.currentTarget.checked;
            setShowQrCode(next);
            commit(buildPatch({ showQrCode: next }));
          }}
        />
        <Toggle
          id={`slide-show-join-${elId}`}
          label='Display join info'
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
