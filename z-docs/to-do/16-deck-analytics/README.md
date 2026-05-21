# 16 — Deck analytics

**Status:** Backend rollup + CSV + dashboard UI done (PR1 + PR2). **PR3 done:** `DeckAnalytics` now carries a `gameRollup` / `presentationRollup` slice; the recorder routes on `session.format`; the dashboard shows a format segment control + format-aware KPI labels + a format-mix chip; CSV gains a "By Format" section. PDF and per-show CSV still deferred.
**Depends on:** 07–09 (new element kinds), 13 (timing/streak), 15 (game history is the input stream)
**Unblocks:** 17 (achievements look at analytics too)

## Scope

Per-deck rolled-up stats and per-element distributions so the deck owner / collaborators can see how their content performs. Powers the "Reports" surface and CSV/PDF export.

This is a read-amplification feature: writes happen on every **session finish** (whether the session was a game or a presentation), reads happen rarely on the analytics dashboard. Maintain the rollup incrementally so reads are O(1).

## Presentation surface — page, not modal

Analytics lives at `/decks/$deckId/analytics` as a full route, not a modal opened from the deck editor. Rationale:

- **Modals don't scale to nested content.** The view will grow tabs/segments (Overall / Games / Presentations), an expandable per-element accordion, distribution charts, a per-session list, and CSV/PDF export. That's a destination, not a one-shot interaction.
- **Deep-linkable.** Sharing a URL to a collaborator works; modal state would not.
- **Already wired.** The deck editor navbar has an "Analytics" pill (visible to OWNER/EDITOR, hidden on system decks) that links into the page. No infrastructure change needed.
- **Modals are reserved** in this codebase for short side-effects (Share, Schedule, AddToCollection). Adding a giant modal here would violate that convention.

A small "Quick view" summary modal triggerable from the deck card on `/my-decks` is fine as a future affordance — it surfaces three KPIs and a "View full analytics" link, but doesn't replace the page.

## Game vs Presentation split

`InteractiveSession.format` is `SessionFormat.GAME | SessionFormat.PRESENTATION` (frozen at session creation; defaulted from `Deck.defaultSessionFormat`). The two formats produce **different shapes of signal**, and the current rollup conflates them:

| Signal               | GAME                            | PRESENTATION                                       |
| -------------------- | ------------------------------- | -------------------------------------------------- |
| Final score          | Yes — scored, leaderboarded     | No — presentations don't score                     |
| Accuracy             | Meaningful (right/wrong)        | Meaningful only when the element has a correct key |
| Distribution         | Side-info                       | **The headline** — round-end shows the chart       |
| Player count framing | "players who finished the game" | "participants who responded"                       |
| Duration             | "Game length"                   | "Presentation length"                              |

Today `DeckAnalyticsService.recordGame(InteractiveSession)` ignores `session.format` and merges both into one rollup, and the dashboard labels assume game framing ("Total plays", "Player-games", "Avg score"). PR3 fixes both.

## New models

```text
DeckAnalytics                          @Document("deck_analytics")
  @Id String deckId                    // shares id with Deck (1:1)
  int totalPlays                       // = total finished sessions (games + presentations) — kept for back-compat
  int totalPlayers                     // distinct players (across plays); maintain via HyperLogLog or accept duplicates
  double averageScore                  // mean finalScore — GAME sessions only (presentations don't score)
  double averageAccuracy               // mean accuracy — both formats, where the element has a correct key
  long averageDurationMs               // mean total session duration (both formats)
  Map<String, ElementStats> perElement // elementId -> stats (still merged across formats; see PR3 notes)
  LocalDateTime lastPlayedAt
  LocalDateTime updatedAt
  // ─── added in PR3 ────────────────────────────────────────────────────────
  Map<SessionFormat, FormatRollup> byFormat   // GAME / PRESENTATION; null on legacy docs until backfilled
```

```text
FormatRollup (embedded record, added in PR3)
  int sessionCount                     // finished sessions of this format
  int participantCount                 // sum of (player,session) tuples — GAME-only field is also named for clarity
  double averageScore                  // GAME: mean finalScore. PRESENTATION: 0 (not surfaced)
  double averageAccuracy               // mean accuracy across scored answers (both formats)
  long averageDurationMs               // mean session length
  LocalDateTime lastRunAt              // most recent finish of this format
```

```text
ElementStats (embedded record)
  int presentedCount                   // # of times this element appeared in a finished game
  int answeredCount                    // # of player submissions
  int correctCount                     // # of correct submissions (where applicable)
  long totalTimeMs                     // sum, for computing average
  double averageTimeMs                 // derived: totalTimeMs / answeredCount
  Map<String, Integer> distribution    // option/answer-bucket -> hit count (per element kind)
  int reactionsReceived
  int chatMessagesDuringRound
```

`distribution` shape per element kind:

- `MCQ` → `{optionId: count}`
- `TRUE_FALSE` → `{"true": count, "false": count}`
- `NUMBER` → bucketed `{"<bucket-key>": count}` (e.g. 10-bucket histogram)
- `TEXT` → `{normalized-answer: count}` capped at top-50
- `WORD_CLOUD` → `{word: count}`
- `ALLOCATION` → `{optionId: avgPoints}` — store as int×100 to keep `Map<String,Integer>` shape
- `RANKING` → `{itemId: avgPlacement×100}`
- `MATCHING` → `{leftId: correctCount}`
- `SCALES` → `{statementId: avgRating×100}`
- `PLACE_ON_IMAGE` → `{}` (heatmap stored separately if needed later)
- `DRAWING` → `{}`
- `Q_AND_A` → `{submissionId: upvotes}` capped at top-50

## Backend changes

- `DeckAnalyticsRepository`, `DeckAnalyticsService`
- **PR3:** rename `DeckAnalyticsService.recordGame` → `recordSessionFinish` (callers updated; old name removed — there is no public surface to keep). The handler reads `session.format` and routes to the matching `FormatRollup`, then also updates the deck-wide totals for back-compat.
- `DeckAnalyticsService.recordSessionFinish(InteractiveSession)` — called from `InteractiveSessionService.endGame()` after `GameHistoryService.recordFinish`:
  - `$inc` totalPlays
  - Recompute running averages incrementally (`new_avg = old_avg + (x - old_avg) / n`)
  - For each element in the deck snapshot, update its `ElementStats`:
    - `presentedCount += 1`
    - For each `PlayerAnswer` against this element: increment `answeredCount`, `correctCount` (if `correct`), accumulate `totalTimeMs`, update `distribution[bucket]++`
  - Update `reactionsReceived` from `Reaction` collection filtered by element
  - Update `chatMessagesDuringRound` similarly
- Endpoints (under `DeckController`):
  - `GET /api/decks/{id}/analytics` — full rollup; **auth: owner or `EDITOR` collaborator only**
  - `GET /api/decks/{id}/analytics/csv` — CSV export
  - `GET /api/decks/{id}/analytics/pdf` — PDF export (start with HTML→PDF via `flying-saucer-pdf`; gate behind a `pdf-export` feature flag)
- New `ReportBuilderService` for CSV/PDF generation. Writes to S3 under `reports/{deckId}/{timestamp}.csv` and stamps `Deck.exportedReportUrl` (or similar field on `InteractiveSessionResult` per-show report).

## Frontend changes

- Route `/decks/$deckId/analytics` (auth-gated to owner/editor) — **stays as a full page; not promoted to a modal** (see "Presentation surface" above).
- Layout (PR2 baseline + PR3 polish):
  - **Format segment control** at the top of the page: `All / Games / Presentations`. Selecting one re-skins the KPI strip and per-element view to that format's `FormatRollup`. Defaults to `All`.
  - **KPI strip**, label-aware per segment:
    - `All`: Total sessions · Participants · Avg duration · Last run · (Avg score / Avg accuracy shown when at least one GAME session exists)
    - `Games`: Games played · Players · Avg score · Avg accuracy · Avg game length · Last played
    - `Presentations`: Presentations delivered · Participants · Avg accuracy (only when any element has a correct key, else hidden) · Avg presentation length · Last delivered
  - **Per-element accordion list**, each expandable to:
    - Distribution chart (bar for MCQ/TF, histogram for Number, word cloud for WordCloud, etc.) — for presentations, the chart is emphasised and "Accuracy" demotes/hides when the element has no correct key.
    - Average answer time
    - Accuracy % (only when meaningful)
- "Export CSV" and "Export PDF" buttons at the top — CSV header section gains the per-format breakdown (PR3).
- Page header gains a small format-mix indicator chip ("12 games · 3 presentations") so the user understands the rollup composition before drilling into a segment.
- "Analytics" pill in the deck editor navbar is already in place (PR2); no change.

## Cross-cutting concerns

- **Don't recompute** on every analytics read — the rollup is the source of truth, computed incrementally on write.
- **Backfill:** one-time job that walks finished `InteractiveSession` rows and feeds them through `recordSessionFinish` (PR1/PR2 used the older `recordGame` name) in chronological order. Reset `DeckAnalytics` first.
- **Survey-only elements** (WordCloud, Drawing, Allocation, QandA, AllocationQuestion) write `correctCount = 0`. Accuracy calculations should exclude unscored elements.
- **Granularity:** rolled-up analytics are deck-level. Per-show reports are different — link via `InteractiveSessionResult.exportedReportUrl` for the per-game CSV.
- **Privacy:** never include personally-identifying info in deck-level analytics — only aggregate counts.

## Checklist

- [x] `DeckAnalytics` + `ElementStats` models + repo
- [x] `DeckAnalyticsService.recordGame` incremental update for each element kind
- [x] Distribution bucketing per element kind — discriminated `Map<String,Integer>`:
      count-style for MCQ/GRID/MATCHING/NUMBER/TEXT/WORD_CLOUD/PLACE_ON_IMAGE;
      sum-style (display ÷ answeredCount) for ALLOCATION/SCALES/RANKING; empty
      for DRAWING/Q_AND_A; SLIDE skipped entirely.
- [x] Backfill script for existing finished games (`--migrate.deck-analytics=true` —
      wipes `deck_analytics` then replays sessions in chronological order)
- [x] `/api/decks/{id}/analytics` endpoint (owner/editor via existing
      `AuthorizationService.requireDeckEditable`; empty rollup returned for
      never-played decks so the dashboard can render an empty-state shell)
- [x] CSV export endpoint (`GET /api/decks/{id}/analytics/csv`, owner/editor,
      `text/csv` with `Content-Disposition: attachment`; deleted elements
      still appear with blank kind/title so the export accounts for everything)
- [x] **PR3 — game/presentation split (backend):**
      - [x] Added `gameRollup` / `presentationRollup: FormatRollup` to
            `DeckAnalytics` (two explicit fields instead of an enum-keyed Map
            — cleaner JSON / TS, same semantics)
      - [x] Renamed `DeckAnalyticsService.recordGame` → `recordSessionFinish`
            (callers in `InteractiveSessionService.endGame` and the backfill
            migration updated); each finish updates the matching `FormatRollup`
            alongside the existing deck-wide totals
      - [x] `--migrate.deck-analytics=true` backfill picks up the rename
            automatically; re-running repopulates the new rollups on legacy
            documents (lazy init handles them on first finish post-PR3 too)
      - [x] `DeckAnalyticsServiceTest` gained `GAME-only`,
            `PRESENTATION-only`, mixed, and legacy-rollup-without-byFormat
            cases (26 tests pass; backend suite 446/446)
- [x] **PR3 — game/presentation split (frontend):**
      - [x] Format segment control (`All / Games / Presentations`) on the
            analytics page; selection drives both KPI strip and the
            per-element accuracy-suppression rule
      - [x] Format-aware KPI labels per the matrix in "Frontend changes"
            above ("Total sessions" / "Participants" in All; "Games played"
            / "Players" in Games; "Presentations delivered" /
            "Participants" in Presentations)
      - [x] Format-mix indicator chip in the page header (e.g.
            "12 games · 3 presentations"; hidden when the deck has never
            been run)
      - [x] CSV export adds a per-format breakdown header section
            ("By Format" with one row per `SessionFormat`); null rollups on
            legacy docs render as zero rows so the shape stays stable
      - [x] Per-element accordion suppresses Correct + Accuracy in the
            Presentations segment when the deck has zero scored answers;
            mixed decks still show them with per-row "—" for unscored
            elements
- [ ] PDF export endpoint (feature-flagged — deferred per chunk-16 scoping;
      revisit if/when there's a real request for it)
- [ ] Per-show CSV report on `InteractiveSessionResult.exportedReportUrl`
      (different surface from the deck-level rollup — needs its own writer
      hooked into `InteractiveSessionService.endGame`; not blocking the
      dashboard)
- [ ] (Future) Compact "Quick view" analytics modal on `/my-decks` deck
      cards — three KPIs + "View full analytics" link to the page.
      Out-of-scope for PR3; tracked here so it isn't lost.
- [x] Analytics page with KPI strip + per-element accordion (route at
      `/decks/$deckId/analytics`; bespoke HTML bar list instead of Recharts —
      keeps the bundle lean and matches the hand-rolled design idiom;
      distribution interpretation switches per kind between count-style and
      sum-style automatically)
- [x] Export CSV button (opens the endpoint in a new tab so the browser
      honors the attachment header; cookies travel with the navigation, so
      no extra auth handling is needed)
- [x] Frontend codegen + lint (regenerated `BrainFlexApi.ts`; new
      `useGetDeckAnalyticsCsvQuery` hook is generated but not used —
      RTK Query can't parse the CSV body, so we bypass it for the download)
- [x] Backend tests pass (`DeckAnalyticsServiceTest` 22, `DeckAnalyticsControllerTest` 7,
      new `DeckAnalyticsReportServiceTest` 6; full suite 393/393)

### Carried out by PR1 (backend rollup)

- Hooked `deckAnalyticsService.recordGame(session)` into
  `InteractiveSessionService.endGame` immediately after the chunk-15
  `gameHistoryService.recordFinish` call. The analytics call is failure-isolated
  (swallow + log) so a broken bucket cannot break game-end.
- `chatMessagesDuringRound` is a known weak spot: chunk-11 `ChatMessage` has no
  `elementId` and we don't retain per-round timestamps post-finish, so per-round
  attribution isn't reconstructible. v1 attaches the **total session chat count**
  to the first non-slide element as a best-effort signal. Worth revisiting if/when
  chat gets per-round metadata.
- `averageScore` and `averageAccuracy` aggregate per (player, game) tuple;
  `totalPlayers` is "player-games observed" (no HyperLogLog dedup yet — per spec).

### Carried out by PR2 (CSV export + dashboard UI)

- New `DeckAnalyticsReportService` builds a two-section CSV (deck KPI row +
  per-element table) and emits it via `GET /api/decks/{id}/analytics/csv`.
  Distribution maps survive the round-trip as JSON inside an RFC-4180-quoted
  cell so downstream consumers can parse them without losing the discriminated
  shape.
- New `/decks/$deckId/analytics` route + `DeckAnalyticsPage` component renders
  the KPI strip and a per-element accordion. The distribution chart is a
  bespoke HTML bar list rather than a chart library — the spec called for
  Recharts but no chart deps are in use yet, and a labeled bar list is enough
  for the shapes the rollup produces (count-style + sum-style maps). The
  switch happens automatically from the element kind: Allocation / Scales /
  Ranking show `sum ÷ answeredCount`, everything else shows the raw bucket
  count.
- Deleted elements (rows in the rollup whose `id` no longer matches any deck
  element) still surface in the dashboard and the CSV, with a blank kind and
  title — losing them would silently erase historic play data when the
  author deletes a question.
- Deck editor navbar gained an "Analytics" pill button that links to the new
  page; visibility is gated on `myRole === OWNER | EDITOR` and `!isSystem`
  so viewers / system decks never see a button that would just 403.

### Carried out by PR3 (game/presentation split + page polish)

- **Modal vs page settled — page wins.** The "intricacy" the dashboard is
  growing toward (segmented KPIs, per-format averages, per-element drill-in,
  exports) doesn't fit in a `<dialog>`. The existing route stays; the deck
  editor's "Analytics" pill keeps linking into it.
- **`SessionFormat` is now a first-class dimension of the rollup.**
  `DeckAnalytics` carries `gameRollup` and `presentationRollup` (two
  explicit `FormatRollup` fields rather than the Map originally sketched
  here — the explicit fields generate cleaner TS, and we only have two
  formats). Deck-wide totals stay populated for back-compat (consumers that
  don't care about the split still work).
- **Service rename:** `recordGame` → `recordSessionFinish`. The current name
  lies — it fires for presentations too. No public API surface is keeping
  the old name alive, so this is a straight rename (callers in
  `InteractiveSessionService.endGame` + the backfill loop get updated in
  the same PR).
- **Language pass on the dashboard.** Replace game-centric copy
  ("Total plays", "Player-games", "Last played", "No play data yet") with
  format-aware copy that reads correctly for both GAME and PRESENTATION
  sessions. Internal field names like `totalPlays` stay (back-compat / CSV
  consumers); only the UI labels change.
- **Format-mix chip** in the header so the user sees "12 games · 3
  presentations" before they pick a segment — prevents the "where did my
  scores go?" reaction when a presentation-heavy deck shows a low avg score
  on the All segment.
- **Accuracy in presentation mode — resolved.** For elements without a
  correct key (open-text, drawing, word cloud), accuracy is meaningless.
  PR3 hides the Correct + Accuracy columns + the accuracy KPI in the
  Presentations segment when the deck has zero scored answers; mixed decks
  still show the column with per-row "—" for unscored elements. The All
  segment shows "Avg score (games)" only when at least one game session
  has been recorded so presentation-only decks don't show a stale "0.0"
  score.
