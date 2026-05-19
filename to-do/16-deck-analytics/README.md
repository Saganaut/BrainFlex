# 16 — Deck analytics

**Status:** Not started
**Depends on:** 07–09 (new element kinds), 13 (timing/streak), 15 (game history is the input stream)
**Unblocks:** 17 (achievements look at analytics too)

## Scope

Per-deck rolled-up stats and per-element distributions so the deck owner / collaborators can see how their content performs. Powers the "Reports" surface and CSV/PDF export.

This is a read-amplification feature: writes happen on every game finish, reads happen rarely on the analytics dashboard. Maintain the rollup incrementally so reads are O(1).

## New models

```text
DeckAnalytics                          @Document("deck_analytics")
  @Id String deckId                    // shares id with Deck (1:1)
  int totalPlays                       // = sum across all ShowcaseResults for this deck
  int totalPlayers                     // distinct players (across plays); maintain via HyperLogLog or accept duplicates
  double averageScore                  // mean finalScore
  double averageAccuracy               // mean accuracy
  long averageDurationMs               // mean total game duration
  Map<String, ElementStats> perElement // elementId -> stats
  LocalDateTime lastPlayedAt
  LocalDateTime updatedAt
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
- `DeckAnalyticsService.recordGame(Showcase)` — called from `ShowcaseService.finish()` after `GameHistoryService.recordFinish`:
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
- New `ReportBuilderService` for CSV/PDF generation. Writes to S3 under `reports/{deckId}/{timestamp}.csv` and stamps `Deck.exportedReportUrl` (or similar field on `ShowcaseResult` per-show report).

## Frontend changes

- New route `/decks/$deckId/analytics` (auth-gated to owner/editor)
- Layout:
  - Top KPI strip — total plays, average score, average accuracy, average duration, last played
  - Per-element accordion list, each expandable to:
    - Distribution chart (bar for MCQ/TF, histogram for Number, word cloud for WordCloud, etc.)
    - Average answer time
    - Accuracy %
- "Export CSV" and "Export PDF" buttons at the top
- Add an "Analytics" tab to the deck editor navbar for quick access

## Cross-cutting concerns

- **Don't recompute** on every analytics read — the rollup is the source of truth, computed incrementally on write.
- **Backfill:** one-time job that walks finished `Showcase` rows and feeds them through `recordGame` in chronological order. Reset `DeckAnalytics` first.
- **Survey-only elements** (WordCloud, Drawing, Allocation, QandA, AllocationQuestion) write `correctCount = 0`. Accuracy calculations should exclude unscored elements.
- **Granularity:** rolled-up analytics are deck-level. Per-show reports are different — link via `ShowcaseResult.exportedReportUrl` for the per-game CSV.
- **Privacy:** never include personally-identifying info in deck-level analytics — only aggregate counts.

## Checklist

- [ ] `DeckAnalytics` + `ElementStats` models + repo
- [ ] `DeckAnalyticsService.recordGame` incremental update for each element kind
- [ ] Distribution bucketing per element kind
- [ ] Backfill script for existing finished games
- [ ] `/api/decks/{id}/analytics` endpoint (owner/editor only)
- [ ] CSV export endpoint
- [ ] PDF export endpoint (feature-flagged)
- [ ] Per-show CSV report on `ShowcaseResult.exportedReportUrl`
- [ ] Analytics page with KPI strip + per-element charts
- [ ] Export buttons
- [ ] Frontend codegen + lint
- [ ] Backend tests pass
