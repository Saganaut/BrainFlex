/**
 * Per-deck analytics dashboard (chunk 16).
 *
 * Reads the rolled-up {@code DeckAnalytics} from the backend and renders a KPI
 * strip plus a per-element accordion. Each element's distribution map is
 * interpreted differently depending on its {@code kind} — count-style maps
 * (MCQ, Grid, Text, …) display raw counts and a label looked up off the live
 * deck (so option ids become "Paris", item ids become "Sahara", etc.), while
 * sum-style maps (Allocation, Scales, Ranking) display the average = sum ÷
 * answeredCount. Survey-only kinds (Drawing, Q&A) get a "no distribution"
 * placeholder since their stats are non-bucketed.
 *
 * The "Export CSV" button bypasses RTK Query (the response is text, not JSON)
 * by opening {@code GET /api/decks/{id}/analytics/csv} in a hidden anchor —
 * the browser then honors the {@code Content-Disposition: attachment} the
 * backend stamps and downloads the file. Cookies travel with the navigation,
 * so the existing session auth is reused without an extra fetch.
 */
import { useMemo, useState } from "react";
import { getRouteApi, Link } from "@tanstack/react-router";
import { ArrowLeftIcon, ArrowDownTrayIcon } from "@heroicons/react/24/outline";

import { Btn } from "@/components/Common/Buttons/Btn";
import {
  useGetDeckQuery,
  useGetDeckAnalyticsQuery,
} from "@/store/BrainFlexApi";
import type {
  DeckAnalytics,
  DeckDto,
  ElementStats,
} from "@/store/BrainFlexApi";
import { apiBaseUrl } from "@/store/emptyApi";
import styles from "./DeckAnalyticsPage.module.css";

const routeApi = getRouteApi("/decks/$deckId/analytics");

// ── Kind classification ──────────────────────────────────────────────────
//
// The backend stores ElementStats.distribution as a discriminated
// Map<String, Integer>. Sum-style kinds (Allocation, Scales, Ranking) need a
// per-row divisor (answeredCount) at render time so we surface an average
// instead of a sum; everything else displays raw counts. Drawing + Q&A have
// no distribution at all.
type DeckElement = NonNullable<DeckDto["elements"]>[number];
type ElementKind = DeckElement["kind"];

const SUM_STYLE_KINDS: ReadonlySet<ElementKind> = new Set<ElementKind>([
  "AllocationQuestion",
  "ScalesQuestion",
  "RankingQuestion",
]);
const NO_DISTRIBUTION_KINDS: ReadonlySet<ElementKind> = new Set<ElementKind>([
  "DrawingQuestion",
  "QAndAQuestion",
]);

const KIND_BADGE_LABEL: Record<ElementKind, string> = {
  Slide: "Slide",
  McqQuestion: "MCQ",
  TextQuestion: "Text",
  NumberQuestion: "Number",
  RankingQuestion: "Ranking",
  ScalesQuestion: "Scales",
  QAndAQuestion: "Q & A",
  GridQuestion: "Grid",
  PlaceOnImageQuestion: "Place on image",
  WordCloudQuestion: "Word cloud",
  AllocationQuestion: "Allocation",
  MatchingQuestion: "Matching",
  DrawingQuestion: "Drawing",
};

const DeckAnalyticsPage = () => {
  const { deckId } = routeApi.useParams();

  const {
    data: deck,
    isLoading: deckLoading,
    error: deckError,
  } = useGetDeckQuery({ id: deckId });
  const {
    data: analytics,
    isLoading: analyticsLoading,
    error: analyticsError,
  } = useGetDeckAnalyticsQuery({ id: deckId });

  const loading = deckLoading || analyticsLoading;
  const fatalError = deckError ?? analyticsError;

  const orderedElements = useMemo(
    () => buildOrderedElements(deck, analytics),
    [deck, analytics],
  );

  const handleExportCsv = () => {
    // window.open keeps the existing session cookie attached so the backend
    // sees the same auth as RTK Query would; the Content-Disposition the
    // backend sets makes the browser download the response instead of
    // navigating away from the dashboard.
    window.open(
      `${apiBaseUrl}/api/decks/${deckId}/analytics/csv`,
      "_blank",
      "noopener",
    );
  };

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <div className={styles.headerLeft}>
          <span className={styles.breadcrumb}>Deck analytics</span>
          <h1 className={styles.title}>{deck?.name ?? "Loading…"}</h1>
        </div>
        <div className={styles.headerActions}>
          <Link
            to='/decks/$deckId/edit'
            params={{ deckId }}
            search={{ questionId: undefined }}>
            <Btn size='md' shape='pill'>
              <ArrowLeftIcon style={{ width: "1rem", height: "1rem" }} />
              Back to editor
            </Btn>
          </Link>
          <Btn
            size='md'
            shape='pill'
            variant='brand'
            onClick={handleExportCsv}
            disabled={loading}>
            <ArrowDownTrayIcon style={{ width: "1rem", height: "1rem" }} />
            Export CSV
          </Btn>
        </div>
      </div>

      {fatalError ? (
        <div className={styles.errorBanner} role='alert'>
          You don&apos;t have permission to view this deck&apos;s analytics, or
          the deck doesn&apos;t exist.
        </div>
      ) : loading ? (
        <div className={styles.loading}>Loading analytics…</div>
      ) : (
        <>
          <KpiStrip analytics={analytics} />
          <h2 className={styles.sectionTitle}>Per-element breakdown</h2>
          {orderedElements.length === 0 ? (
            <div className={styles.empty}>
              No play data yet. Run a session to start populating analytics.
            </div>
          ) : (
            <div className={styles.elementList}>
              {orderedElements.map((row) => (
                <ElementCard key={row.elementId} {...row} />
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
};

// ── KPI strip ────────────────────────────────────────────────────────────

const KpiStrip = ({ analytics }: { analytics: DeckAnalytics | undefined }) => {
  const totalPlays = analytics?.totalPlays ?? 0;
  const totalPlayers = analytics?.totalPlayers ?? 0;
  const avgScore = analytics?.averageScore ?? 0;
  const avgAccuracy = analytics?.averageAccuracy ?? 0;
  const avgDurationMs = analytics?.averageDurationMs ?? 0;
  const lastPlayedAt = analytics?.lastPlayedAt;
  return (
    <div className={styles.kpiStrip}>
      <Kpi label='Total plays' value={totalPlays.toLocaleString()} />
      <Kpi
        label='Player-games'
        value={totalPlayers.toLocaleString()}
        sub='(counts each finish, not distinct people)'
      />
      <Kpi label='Avg score' value={avgScore.toFixed(1)} />
      <Kpi label='Avg accuracy' value={`${(avgAccuracy * 100).toFixed(1)}%`} />
      <Kpi label='Avg duration' value={formatDuration(avgDurationMs)} />
      <Kpi
        label='Last played'
        value={lastPlayedAt ? formatRelativeDate(lastPlayedAt) : "—"}
      />
    </div>
  );
};

const Kpi = ({
  label,
  value,
  sub,
}: {
  label: string;
  value: string;
  sub?: string;
}) => (
  <div className={styles.kpiCard}>
    <span className={styles.kpiLabel}>{label}</span>
    <span className={styles.kpiValue}>{value}</span>
    {sub && <span className={styles.kpiSub}>{sub}</span>}
  </div>
);

// ── Per-element card ─────────────────────────────────────────────────────

interface ElementRow {
  elementId: string;
  element: DeckElement | undefined;
  stats: ElementStats;
}

const ElementCard = ({ elementId, element, stats }: ElementRow) => {
  const [expanded, setExpanded] = useState(false);
  const kind = element?.kind;
  const title = element
    ? (element.title?.trim() ? element.title : "(untitled)")
    : "(deleted element)";
  const accuracy =
    stats.answeredCount && stats.answeredCount > 0
      ? (stats.correctCount ?? 0) / stats.answeredCount
      : undefined;
  const kindLabel = kind ? KIND_BADGE_LABEL[kind] : "Element";
  return (
    <div className={styles.elementCard}>
      <button
        type='button'
        className={styles.elementHeader}
        aria-expanded={expanded}
        onClick={() => {
          setExpanded((prev) => !prev);
        }}>
        <span className={styles.kindBadge}>{kindLabel}</span>
        <span className={styles.elementTitle}>{title}</span>
        <span className={styles.elementSummary}>
          <span className={styles.summaryStat}>
            <span className={styles.summaryLabel}>Presented</span>
            <span className={styles.summaryValue}>
              {stats.presentedCount ?? 0}
            </span>
          </span>
          <span className={styles.summaryStat}>
            <span className={styles.summaryLabel}>Answered</span>
            <span className={styles.summaryValue}>
              {stats.answeredCount ?? 0}
            </span>
          </span>
          {accuracy !== undefined && (
            <span className={styles.summaryStat}>
              <span className={styles.summaryLabel}>Accuracy</span>
              <span className={styles.summaryValue}>
                {(accuracy * 100).toFixed(0)}%
              </span>
            </span>
          )}
        </span>
      </button>
      {expanded && (
        <div className={styles.elementBody}>
          <div>
            <h3 className={styles.distributionTitle}>Response distribution</h3>
            <DistributionChart
              elementId={elementId}
              element={element}
              stats={stats}
            />
          </div>
          <div className={styles.statsTable}>
            <span className={styles.statLabel}>Presented</span>
            <span className={styles.statValue}>{stats.presentedCount ?? 0}</span>
            <span className={styles.statLabel}>Answered</span>
            <span className={styles.statValue}>{stats.answeredCount ?? 0}</span>
            <span className={styles.statLabel}>Correct</span>
            <span className={styles.statValue}>
              {kind && NO_DISTRIBUTION_KINDS.has(kind)
                ? "—"
                : (stats.correctCount ?? 0)}
            </span>
            <span className={styles.statLabel}>Accuracy</span>
            <span className={styles.statValue}>
              {accuracy === undefined ? "—" : `${(accuracy * 100).toFixed(1)}%`}
            </span>
            <span className={styles.statLabel}>Avg time</span>
            <span className={styles.statValue}>
              {stats.averageTimeMs
                ? `${(stats.averageTimeMs / 1000).toFixed(1)} s`
                : "—"}
            </span>
            <span className={styles.statLabel}>Reactions</span>
            <span className={styles.statValue}>
              {stats.reactionsReceived ?? 0}
            </span>
            <span className={styles.statLabel}>Chat messages</span>
            <span className={styles.statValue}>
              {stats.chatMessagesDuringRound ?? 0}
            </span>
          </div>
        </div>
      )}
    </div>
  );
};

// ── Distribution chart ───────────────────────────────────────────────────

interface DistributionRow {
  key: string;
  label: string;
  value: number;
  display: string;
}

const DistributionChart = ({
  elementId,
  element,
  stats,
}: {
  elementId: string;
  element: DeckElement | undefined;
  stats: ElementStats;
}) => {
  const kind = element?.kind;
  const rows = useMemo(
    () => buildDistributionRows(element, stats),
    [element, stats],
  );
  if (kind && NO_DISTRIBUTION_KINDS.has(kind)) {
    return (
      <p className={styles.distributionEmpty}>
        {kind === "DrawingQuestion"
          ? "Drawings aren't bucketed — review them in the host replay instead."
          : "Audience Q&A submissions live on the session record, not the rollup."}
      </p>
    );
  }
  if (rows.length === 0) {
    return (
      <p className={styles.distributionEmpty}>
        No responses recorded for {elementId.slice(0, 8)}…
      </p>
    );
  }
  const max = rows.reduce((acc, row) => Math.max(acc, row.value), 0);
  return (
    <div className={styles.distributionList}>
      {rows.map((row) => {
        const pct = max > 0 ? (row.value / max) * 100 : 0;
        return (
          <div key={row.key} className={styles.distributionRow}>
            <span className={styles.distributionLabel} title={row.label}>
              {row.label}
            </span>
            <span className={styles.distributionBar}>
              <span
                className={styles.distributionFill}
                style={{ width: `${pct}%` }}
              />
            </span>
            <span className={styles.distributionValue}>{row.display}</span>
          </div>
        );
      })}
    </div>
  );
};

// ── Data plumbing ────────────────────────────────────────────────────────

const buildOrderedElements = (
  deck: DeckDto | undefined,
  analytics: DeckAnalytics | undefined,
): ElementRow[] => {
  const perElement = analytics?.perElement ?? {};
  const rows: ElementRow[] = [];
  const seen = new Set<string>();

  // 1. Live deck order — questions the author still owns appear first, in
  //    the order they're presented during a session.
  for (const element of deck?.elements ?? []) {
    if (!element.id) continue;
    if (element.kind === "Slide") continue;
    const stats = perElement[element.id];
    // TS treats the perElement index signature as total, but the rollup
    // only carries elements that have been presented in a finished game.
    // eslint-disable-next-line @typescript-eslint/no-unnecessary-condition
    if (!stats) continue;
    rows.push({ elementId: element.id, element, stats });
    seen.add(element.id);
  }

  // 2. Orphans — analytics rows that no longer match any deck element
  //    (renamed, deleted, etc.). Surface them so the export still
  //    accounts for them.
  for (const [elementId, stats] of Object.entries(perElement)) {
    if (seen.has(elementId)) continue;
    rows.push({ elementId, element: undefined, stats });
  }

  return rows;
};

const buildDistributionRows = (
  element: DeckElement | undefined,
  stats: ElementStats,
): DistributionRow[] => {
  const dist = stats.distribution ?? {};
  const entries = Object.entries(dist);
  if (entries.length === 0) return [];

  const kind = element?.kind;
  const labelByKey = buildLabelLookup(element);
  const answeredCount = stats.answeredCount ?? 0;
  const sumStyle = kind ? SUM_STYLE_KINDS.has(kind) : false;

  const rows: DistributionRow[] = entries.map(([key, raw]) => {
    const value = sumStyle && answeredCount > 0 ? raw / answeredCount : raw;
    const display = sumStyle ? value.toFixed(1) : value.toLocaleString();
    return {
      key,
      label: labelByKey[key] ?? key,
      value,
      display,
    };
  });
  rows.sort((a, b) => b.value - a.value);
  return rows;
};

const buildLabelLookup = (
  element: DeckElement | undefined,
): Record<string, string> => {
  if (!element) return {};
  const out: Record<string, string> = {};
  switch (element.kind) {
    case "McqQuestion":
    case "AllocationQuestion":
      for (const opt of element.options ?? []) {
        if (opt.id) out[opt.id] = opt.text ?? opt.id;
      }
      return out;
    case "RankingQuestion":
      for (const item of element.items ?? []) {
        if (item.id) out[item.id] = item.label ?? item.id;
      }
      return out;
    case "ScalesQuestion":
      for (const s of element.statements ?? []) {
        if (s.id) out[s.id] = s.text ?? s.id;
      }
      return out;
    case "MatchingQuestion": {
      // Matching distribution keys are "leftId>rightId". Build a label
      // that reads as "Left → Right" so the dashboard column lines up
      // with the player's experience.
      const leftById: Record<string, string> = {};
      const rightById: Record<string, string> = {};
      for (const pair of element.pairs ?? []) {
        if (pair.id) {
          if (pair.leftLabel) leftById[pair.id] = pair.leftLabel;
          if (pair.rightLabel) rightById[pair.id] = pair.rightLabel;
        }
      }
      // The dict has all pair ids — when reading the bucket key
      // "leftId>rightId" the caller composes the label.
      const target: Record<string, string> = {};
      return new Proxy(target, {
        get(_t, prop) {
          if (typeof prop !== "string") return undefined;
          const arrow = prop.indexOf(">");
          if (arrow < 0) return undefined;
          const left = prop.slice(0, arrow);
          const right = prop.slice(arrow + 1);
          const leftLabel = leftById[left] ?? left;
          const rightLabel = rightById[right] ?? right;
          return `${leftLabel} → ${rightLabel}`;
        },
      });
    }
    default:
      // Number/Text/WordCloud/Grid/PlaceOnImage — the bucket key is already
      // human-readable (or close enough). Slide/Drawing/Q&A never reach here.
      return out;
  }
};

// ── Formatters ───────────────────────────────────────────────────────────

const formatDuration = (ms: number): string => {
  if (!ms || ms <= 0) return "—";
  const totalSeconds = Math.round(ms / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  if (minutes <= 0) return `${seconds}s`;
  return `${minutes}m ${seconds.toString().padStart(2, "0")}s`;
};

const formatRelativeDate = (iso: string): string => {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString();
};

export { DeckAnalyticsPage };
