# 21 — Slide design shape & right-sidebar polish

**Status:** Mostly complete — every checkbox in Part B and Part C is now done; Part A is done except for A.4 (Design value object) and the frontend half of A.7 (titleLabel surfacing + render). A.7 backend is in flight via the chunk-25 `ElementChrome` refactor.
**Depends on:** 01 (tags / TagPicker create affordance), 02 (deck metadata foundation), 10 (Slide common additions — provenance + autoAdvance already in place)
**Unblocks:** nothing critical — this is the UX-completion pass for the deck-editor right rail

## Remaining work

- **A.4 — `Design` value object.** No `Design` record on backend; `Theme` / `Slide` / `InteractiveSession` still carry `image` + `background` as separate top-level fields. `ImagePicker` lives inline in `ThemePanel.tsx` (no shared `DesignEditor` component). Background-color picker and "Reset to theme" toggle don't exist anywhere. This is the biggest remaining piece of Part A — best landed as its own chunk because it touches three models, image hydration, and the theme/slide/session UI.
- **A.7 frontend — `titleLabel` surfacing + render.** Backend field is being delivered by the chunk-25 `ElementChrome` refactor (`backend/.../model/element/ElementChrome.java` already declares `titleLabel`; McqQuestion has it as a flat field today; remaining records inherit via chrome once converted). Still TODO once the backend stabilises: `Input` in `EditSlidePanel.tsx`, mirror in per-kind question inspectors, render small uppercase chip in `SlideDisplay` + player views. Frontend can't move until codegen runs again — currently blocked by the in-flight chunk-25 compile errors.

## What's already done

- **A.1 chart picker** — `ResultsDisplayType` extended (`BAR_HORIZONTAL`, `BAR_VERTICAL`, `WORD_CLOUD`, `PIE_CHART`, `DEFAULT`, `HISTOGRAM` kept as deprecated read-only). 4-button icon row with `Tooltip` lives in `SlideOptionsSection.tsx`; per-kind relevance comes from `RightSidebar/data.ts`.
- **A.2 selectionsPerParticipant=0** — `NumberInput min={0}` + helper "0 means unlimited" wired (`SlideOptionsSection.tsx:229`).
- **A.3 showResultsAsPercentage** — toggle wired and disabled per `OptionRelevance.showResultsAsPercentage`.
- **A.5 showQrCode** — `Slide.showQrCode` boolean lives next to `joinType`/`showJoinInformation` (`Slide.java:67`). UI is two independent toggles ("Display QR code", "Display join info") in `SlideOptionsSection.tsx`. Legacy `joinType` still read as the fallback for `showQrCode` in the frontend default-init.
- **B inline-create for subject + tags** — `DeckCategorizePanel.tsx` uses `TagPicker singleSelect creatable` for Subject and `TagPicker creatable` for Deck tags. `TagController.createTag` lets `USER` role create rows but coerces `curated=false` for non-admins.
- **C reviews panel UI** — `DeckReviewsPanel.tsx` renders the histogram, the caller's own star input, a textarea + Save review button (gated on `myStars > 0`), and a paginated review list using the chunk-22 `Pagination` component. The textarea pre-populates from `useGetMyRatingQuery`.

## Scope

Three right-sidebar panels currently have backend models that are richer than what the UI exposes (or missing a few small fields). This chunk closes those gaps, introduces the reusable **`Design`** value object for theme/slide/interactive session background + content imagery, and finishes the unfinished bits of chunks 01 and 04 that the user actually feels when they sit in the editor.

The three panels:

- **`EditSlidePanel`** — `frontend/src/components/DeckEditor/RightSidebar/EditSlidePanel.tsx`
- **`DeckCategorizePanel`** — same folder
- **`DeckReviewsPanel`** — same folder

## Part A — `EditSlidePanel` additions

A lot of the underlying `Slide` fields already exist (`resultsDisplayType`, `multipleSelectionsEnabled`, `selectionsPerParticipant`, `showResultsAsPercentage`, `joinType`, `showJoinInformation`, `showResponses`, `heading`, `participantInformation`, `background`, `image`). The work below is mostly **enum extensions**, **two new fields**, and **UI surfacing**.

### A.1 — Data display chart picker

Replace the `resultsDisplayType` dropdown with a row of four icon buttons (with hover tooltips):

- Bar chart — horizontal
- Bar chart — vertical
- Word cloud
- Pie chart

**Backend:**

- Extend `ResultsDisplayType` (`backend/src/main/java/cephadex/brainflex/model/enums/ResultsDisplayType.java`):
  - Add `BAR_HORIZONTAL`, `BAR_VERTICAL`, `WORD_CLOUD`
  - Keep `HISTOGRAM` and `PIE_CHART` for read compatibility; map `HISTOGRAM → BAR_VERTICAL` on write or leave both and let the renderer treat them as equivalent. Decide before touching this — don't ship two enum values that mean the same thing.
  - `DEFAULT` stays (renders the kind's built-in viz).
- No new field; this is just enum expansion.

**Frontend:**

- Replace the `Dropdown` in `EditSlidePanel.tsx` with a new `IconToggleGroup` (or extend `RadioGroup` to support an icon-button variant). Each button gets a `Tooltip` (already exists at `components/Common/Tooltip`).
- Pick icons from the existing lucide-react set (`BarChart3`, `BarChartHorizontal`, `Cloud`/custom, `PieChart`).
- The current `RESULTS_DISPLAY_OPTIONS` array in `EditSlidePanel.tsx` becomes the source of truth — add the new values there.

### A.2 — Selections per participant (allow 0 = unlimited)

The field exists (`selectionsPerParticipant: int`, default `1`). Today the editor lets the user type any positive number. Confirm and (if needed) update:

- Frontend `NumberInput` `min` is set to `0`.
- A helper line below the input reads *"0 means unlimited"*.
- `ElementScorer` already trusts the answer payload, but verify multi-select scoring doesn't choke when `selectionsPerParticipant == 0`. Add a unit test.

### A.3 — Show results as percentage

Field exists (`showResultsAsPercentage: boolean`). The Toggle is already wired. Verify:

- Reveal-time renderers (`SlideReveal`, MCQ reveal, etc.) actually read the flag and switch the y-axis label from counts to `%`.
- Add a test in `InteractiveSessionServiceTest` or in the renderer's component test.

### A.4 — Design shape: content image, background image, background color, reset to theme

This is the most significant piece. Introduce a reusable **`Design`** value object that captures the visual chrome a `Theme`, `Slide`, or `InteractiveSession` can override.

**New model** (`backend/src/main/java/cephadex/brainflex/model/Design.java`):

```text
Design                              (embeddable record, never persisted standalone)
  Image contentImage                // foreground / content image — what Slide.image is today
  Image backgroundImage             // backdrop — what Slide.background is today
  String backgroundColor            // hex or token name; null = inherit
  String contentImagePosition       // "left" | "right" | "top" | "bottom" | "background" — supersedes MediaPosition
  boolean useThemeBackground        // true = ignore the per-slide background and inherit Theme.backgroundVariants
  boolean useThemeColor             // true = ignore backgroundColor and inherit theme tokens
```

**Migration on `Slide`:**

- Add `Design design` to the `Slide` record.
- On read, populate `design` from the legacy `image` / `background` / `mediaPosition` fields if it's null.
- On write, prefer the new `design` field; mirror back to the legacy fields for one release so older clients still read.
- Remove legacy fields in a follow-up after the frontend has fully switched.

**Reuse on `Theme` and `InteractiveSession`:**

- `Theme` currently owns `logoVariants` + `backgroundVariants`. Add an embedded `Design design` so the theme picker exposes the same shape; map `backgroundVariants` → `design.backgroundImage`.
- `InteractiveSession` (or `InteractiveSessionSettings`) gains an optional `Design designOverride` for "Custom branding for this game" — overrides the deck/theme design for the live show.

**Frontend:**

- New `DesignEditor` component under `components/Common/DesignEditor/` — reused by `EditSlidePanel`, the theme editor, and the interactive session setup form.
- Four controls: Content image (existing `ImagePicker`), Background image (`ImagePicker`), Background color (color swatch grid + hex input), **Reset to theme** button (sets `useThemeBackground = true` and `useThemeColor = true`).
- Subject to **tokens.css** rules — no hardcoded colors; swatches come from a token-driven palette.

### A.5 — Display QR code & Display join info — two toggles

Currently `joinType` is one enum dropdown (`INSTRUCTIONS_BAR | QR_CODE`) and `showJoinInformation` is a separate boolean. The user wants two independent toggles so the host can show both, neither, or either.

**Backend:**

- Add `boolean showQrCode` to `Slide` (default `false` for non-lobby slides, `true` for the lobby slide).
- `showJoinInformation` stays as-is.
- Deprecate `joinType` once both toggles ship; keep reading the legacy enum and translate to the new pair (`INSTRUCTIONS_BAR → showJoinInformation=true, showQrCode=false`; `QR_CODE → showQrCode=true, showJoinInformation=true`).

**Frontend:**

- Replace the `joinType` `Dropdown` in `EditSlidePanel.tsx` with two `Toggle` rows: "Display QR code" and "Display join info".
- Update `SlideReveal` / lobby renderers to honor both flags independently.

### A.6 — Responses radio (already present — verify wiring)

`showResponses: ShowResponsesMode` is already there with three values (`INSTANT | ON_CLICK | PRIVATE`) and `EditSlidePanel.tsx` already uses `RadioGroup`. Verify:

- The reveal pipeline actually respects each mode (no live updates when `PRIVATE`; reveal only on host click when `ON_CLICK`).
- `InteractiveSessionService.recordAnswer` does **not** broadcast aggregated results when the slide's `showResponses == PRIVATE`.
- Add tests in `InteractiveSessionServiceTest`.

### A.7 — Title label text input

New field for an optional pre-heading label (the small tag above the heading, e.g. "QUESTION 3 OF 10" or "POLL").

**Backend:**

- Add `String titleLabel` to **every** `DeckElement` permits record. This is small enough to slot into the chunk-10 cross-cutting metadata family — touch each record, default to `null`, `@JsonInclude(NON_NULL)`.

**Frontend:**

- Add an `Input` in `EditSlidePanel.tsx` labelled "Title label" with helper text "Shown above the heading".
- Surface the same field in the question-kind inspectors (since it's on the interface).
- Render the label as a small uppercase chip in `SlideDisplay` and the per-kind player views.

## Part B — `DeckCategorizePanel` additions

The TagPicker already supports an inline "create tag" affordance — chunk 01 ships it as **admin-only**. The user wants regular users to create their own custom subjects + custom tags from this panel.

**Backend:**

- Loosen the auth check in `TagController.create` so non-admin authenticated users can create tags, but only if `curated = false`. Admins can still toggle `curated`.
- Add `String createdByUserId` to `Tag` so we can attribute and (later) garbage-collect user-created tags that stop being used.
- Add an endpoint or query param to `listTags` for "tags I've created" so the picker can show the user's own tags first.

**Frontend:**

- `DeckCategorizePanel.tsx` — replace the curated-only `Dropdown` for subject with a subject picker that lets the user type a new subject and create it inline. The same inline-create UX the `TagPicker` uses for tags.
- Visual: in the dropdown, show a "Create '{input}' as new subject" row at the bottom of the typeahead results.
- Tag-side already works once the controller loosens the auth check — confirm `TagPicker` shows the create affordance for non-admins.
- Inline-created subjects are saved as `Tag` records with `parentTagId = null`, `curated = false`.

**Cross-cutting:**

- An Explore page that lists every user-created subject will be noisy. Default the Explore filter chips to `curated = true`. The user-created subjects only appear in the picker on a deck the user is editing.

## Part C — `DeckReviewsPanel` post-review fix

The panel already has `rateDeck` wired in, but the "Save review" textarea + button only render when `myStars > 0`. The user reports the panel "doesn't let us post a review yet". Investigation needed before deciding the fix — possibilities:

1. The current gating (must pick stars first) is confusing. Allow review text without picking stars and persist a review with `stars = null`, OR persist a "review-only" comment through chunk 04's comment system instead.
2. The `submitReview` call uses `rateDeck`, which posts to `PUT /api/decks/{id}/rating`. Confirm that re-submitting with the same star count and new text actually updates `review` (the backend may be no-op'ing).
3. The success path doesn't re-render the review list — RTK Query cache invalidation for `useListRatingsQuery` after a `rateDeck` mutation may be missing in `apiEnhancements.ts`.

**Plan:**

- Reproduce locally. Open a deck, pick stars, type a review, click "Save review", confirm whether the new review appears in the list below.
- Whatever the cause, this should result in: typing stars + review + clicking Save reliably posts the review and the list refreshes without a page reload.
- If `apiEnhancements.ts` is missing the invalidation, add an `onQueryStarted` for `rateDeck` that patches `useListRatingsQuery({ id, page: 0 })` and bumps `Deck.ratingCount` / `averageRating`.
- If the backend needs to accept review-only submissions (no stars), gate the textarea differently and accept `stars = null` in `RateDeckRequest`.

## Cross-cutting concerns

- **Codegen** — every backend model/enum change here forces an OpenAPI re-export. `npx @rtk-query/codegen-openapi openapi-config.cts` after each backend pass.
- **Sealed types** — `titleLabel` lands on the `DeckElement` interface, so every permits record must update. Mirror the existing chunk-10b approach (defaults on the interface, fields on each record, no behavior in `ElementScorer`).
- **Tokens** — `DesignEditor` color swatches come from `tokens.css`. Hex input still needs to live behind a "Custom" affordance so the typical user picks tokens, not arbitrary hex.
- **Image plumbing** — `Image` already round-trips through `DeckImageMapper` / `DeckImageHydrationService`. The new `Design` shape needs the same hydration treatment — register `design.contentImage` and `design.backgroundImage` in `DeckImageMapper`.
- **Backwards compat** — `Slide.image` / `Slide.background` / `Slide.mediaPosition` / `Slide.joinType` are *not* removed in this chunk. Mirror to `Design` on read; the deletion is a follow-up after the frontend swap.

## Checklist

### Part A — EditSlidePanel

- [x] `ResultsDisplayType` extended with `BAR_HORIZONTAL`, `BAR_VERTICAL`, `WORD_CLOUD`
- [x] Chart picker icon row with tooltips replaces the dropdown
- [x] `selectionsPerParticipant` accepts `0 = unlimited`; helper text *(scorer test still TODO)*
- [x] `showResultsAsPercentage` toggle wired *(reveal-renderer end-to-end verification + test still TODO)*
- [ ] `Design` record added; embedded on `Slide`, `Theme`, `InteractiveSession`
- [ ] `DesignEditor` component shared by slide / theme / interactive session forms
- [ ] `DeckImageMapper` + `DeckImageHydrationService` updated for the new image slots
- [x] `showQrCode` boolean added on `Slide`; two-toggle UI replaces the `joinType` dropdown
- [x] `joinType` legacy translation on read for one release *(frontend falls back to `joinType === "QR_CODE"` when `showQrCode` is unset)*
- [x] `showResponses` modes verified in `InteractiveSessionService` (`PRIVATE` suppresses interim WordCloud broadcast in `submitAnswer`; covered by `submitAnswer_OnWordCloudRound_WhenShowResponsesPrivate_DoesNotBroadcastWordCloud`)
- [x] `titleLabel` on the backend element family *(delivered via chunk-25 `ElementChrome` refactor — already in `ElementChrome.java`; McqQuestion has it as a flat field today; other records pick it up automatically when chrome composition lands)*
- [ ] `titleLabel` surfaced in slide + question inspectors and rendered in `SlideDisplay` / player views *(blocked on codegen re-run, which is blocked on chunk-25 compile stabilising)*

### Part B — DeckCategorizePanel

- [x] `TagController.create` allows non-admin authenticated users to create `curated = false` tags
- [x] `Tag.createdByUserId` added *(indexed; stamped on the user-inline-create path; `findByCreatedByUserId` repo method backs `?createdByMe=true` on `listTags`)*
- [x] Subject `Dropdown` becomes an inline-create picker (`TagPicker singleSelect creatable`)
- [x] `TagPicker` inline-create affordance verified for non-admins
- [x] Explore filter chips default to `curated = true` *(`ExplorePage.tsx:65` already calls `useListTagsQuery({ curated: true })`)*

### Part C — DeckReviewsPanel

- [x] Reproduce the "can't post a review" issue and root-cause it *(panel ships textarea + Save review button gated on `myStars > 0`)*
- [x] Fix: widen submit gate / wire `rateDeck` for review-only updates
- [x] After "Save review", the new review appears in the list without a reload *(`apiEnhancements.ts` → `enhancements/rating.ts` `onQueryStarted` for `rateDeck` already refetches every materialized `listRatings` page plus `getDeck` for `ratingCount`/`averageRating` and `getMyRating` for the caller's own row)*
- [ ] Test covering the post → list-refresh path

### Cross-cutting

- [ ] OpenAPI re-export + frontend codegen *(needed once `titleLabel` flows into all records via chrome and `Design` lands)*
- [ ] `apiEnhancements.ts` `onQueryStarted` patches for every new mutation
- [ ] Backend tests pass (`./mvnw test`)
- [ ] Frontend lint + tests pass
