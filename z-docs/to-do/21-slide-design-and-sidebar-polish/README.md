# 21 — Slide design shape & right-sidebar polish

**Status:** Not started
**Depends on:** 01 (tags / TagPicker create affordance), 02 (deck metadata foundation), 10 (Slide common additions — provenance + autoAdvance already in place)
**Unblocks:** nothing critical — this is the UX-completion pass for the deck-editor right rail

## Scope

Three right-sidebar panels currently have backend models that are richer than what the UI exposes (or missing a few small fields). This chunk closes those gaps, introduces the reusable **`Design`** value object for theme/slide/showcase background + content imagery, and finishes the unfinished bits of chunks 01 and 04 that the user actually feels when they sit in the editor.

The three panels:

- **`EditSlidePanel`** — `frontend/src/components/CreateDashboard/RightSidebar/EditSlidePanel.tsx`
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
- Add a test in `ShowcaseServiceTest` or in the renderer's component test.

### A.4 — Design shape: content image, background image, background color, reset to theme

This is the most significant piece. Introduce a reusable **`Design`** value object that captures the visual chrome a `Theme`, `Slide`, or `Showcase` can override.

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

**Reuse on `Theme` and `Showcase`:**

- `Theme` currently owns `logoVariants` + `backgroundVariants`. Add an embedded `Design design` so the theme picker exposes the same shape; map `backgroundVariants` → `design.backgroundImage`.
- `Showcase` (or `ShowcaseSettings`) gains an optional `Design designOverride` for "Custom branding for this game" — overrides the deck/theme design for the live show.

**Frontend:**

- New `DesignEditor` component under `components/Common/DesignEditor/` — reused by `EditSlidePanel`, the theme editor, and the showcase setup form.
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
- `ShowcaseService.recordAnswer` does **not** broadcast aggregated results when the slide's `showResponses == PRIVATE`.
- Add tests in `ShowcaseServiceTest`.

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

- [ ] `ResultsDisplayType` extended with `BAR_HORIZONTAL`, `BAR_VERTICAL`, `WORD_CLOUD`
- [ ] Chart picker icon row with tooltips replaces the dropdown
- [ ] `selectionsPerParticipant` accepts `0 = unlimited`; helper text + scorer test
- [ ] `showResultsAsPercentage` verified end-to-end (reveal renderers honor it)
- [ ] `Design` record added; embedded on `Slide`, `Theme`, `Showcase`
- [ ] `DesignEditor` component shared by slide / theme / showcase forms
- [ ] `DeckImageMapper` + `DeckImageHydrationService` updated for the new image slots
- [ ] `showQrCode` boolean added on `Slide`; two-toggle UI replaces the `joinType` dropdown
- [ ] `joinType` legacy translation on read for one release
- [ ] `showResponses` modes verified in `ShowcaseService` (private suppresses broadcast)
- [ ] `titleLabel` added on `DeckElement` interface + every permits record
- [ ] `titleLabel` surfaced in slide + question inspectors and rendered in `SlideDisplay` / player views

### Part B — DeckCategorizePanel

- [ ] `TagController.create` allows non-admin authenticated users to create `curated = false` tags
- [ ] `Tag.createdByUserId` added
- [ ] Subject `Dropdown` becomes an inline-create picker
- [ ] `TagPicker` inline-create affordance verified for non-admins
- [ ] Explore filter chips default to `curated = true`

### Part C — DeckReviewsPanel

- [ ] Reproduce the "can't post a review" issue and root-cause it
- [ ] Fix: either widen the submit gate, add the missing RTK Query invalidation, or fix the backend write path — whichever the repro identifies
- [ ] After "Save review", the new review appears in the list without a reload
- [ ] Test covering the post → list-refresh path

### Cross-cutting

- [ ] OpenAPI re-export + frontend codegen
- [ ] `apiEnhancements.ts` `onQueryStarted` patches for every new mutation
- [ ] Backend tests pass (`./mvnw test`)
- [ ] Frontend lint + tests pass
