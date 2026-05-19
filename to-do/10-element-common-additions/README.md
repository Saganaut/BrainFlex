# 10 — Common element additions

**Status:** Not started
**Depends on:** 01 (tags) for per-question `tagIds`
**Unblocks:** 16 (analytics can group by tag), question-bank features later

## Scope

Add the cross-cutting fields every `DeckElement` should have (provenance, versioning, per-question tagging) and the per-kind ergonomic improvements (shuffle, fuzzy match, slide blocks) you'll need once the new element kinds from 07–09 land.

This is the chunk that gets every existing record signature growing — touch lightly and use Jackson `JsonInclude.NON_NULL` so payloads don't bloat.

## Common additions on the `DeckElement` sealed interface

Add to **every** permits record (Slide, McqQuestion, TextQuestion, NumberQuestion, RankingQuestion, ScalesQuestion, GridQuestion, PlaceOnImageQuestion, QAndAQuestion, plus the new WordCloud, TrueFalse, Allocation, Matching, Drawing):

- `String createdByUserId, lastEditedByUserId` — slide-level provenance for collab (chunk 06)
- `LocalDateTime createdAt, updatedAt`
- `List<String> tagIds` — per-question tagging (enables a real question bank later)
- `String mediaCaption, altText` — accessibility + caption under the media slot
- `boolean reactionsEnabled` — per-slide opt-out of audience emoji reactions (chunk 11)
- `Integer version` — bumps on every update; lets clients detect stale edits

Because every record changes, do this carefully:

1. Update the `DeckElement` interface with new default methods returning sensible defaults.
2. Update every record with the new fields and a static factory `withProvenance(...)` for tests/services.
3. Update `useElementEditor.ts` so it sets `lastEditedByUserId = currentUser.id` and increments `version` on every commit.
4. Update `apiEnhancements.ts` — on `updateElement` cache patch, set `updatedAt` client-side so the UI refreshes; the server overwrites it.

## Per-kind additions

### McqQuestion

- `boolean shuffleOptions` — default `true` for new questions; Kahoot ships this default
- `boolean allowMultipleSelect` — explicit (right now inferred from `correctOptionIds.size() > 1`)
- `Integer maxSelections` — cap in multi-select mode

### TextQuestion

- `int maxLength` — default 80
- `boolean trimWhitespace` — default `true`
- `boolean fuzzyMatch` — Levenshtein within `fuzzyDistance` edits
- `int fuzzyDistance` — default 1

### NumberQuestion

- `Double minValue, maxValue` — input clamping (nullable)
- `boolean allowNegative` — default `true`

### QAndAQuestion

- `boolean anonymousSubmissions` — hide author identity on the moderation board
- `int minVotesToShow` — moderation threshold

### RankingQuestion

- `boolean shuffleItemsForPresentation`

### Slide

Replace the single `body: String` with a `List<SlideBlock> blocks` so a Content slide can be heading + body + image + bullet list (Mentimeter Content slides build this way).

```text
SlideBlock (record, polymorphic via Jackson @JsonTypeInfo)
  // Common
  String id, kind                      // "heading", "body", "bulletList", "image", "callout"

  // Heading
  String headingText, headingLevel     // 1..3

  // Body
  String richBody                      // TipTap HTML (matches existing RichTextInput)

  // BulletList
  List<String> items                   // or List<RichTextItem>

  // Image
  Image image
  String caption

  // Callout
  String calloutBody, calloutTone      // info | warn | success
```

Keep `Slide.body` for one release as a fallback; mirror it from `blocks[0]` if present so the migration is non-breaking. Then delete `body` in a follow-up.

Also add `Integer autoAdvanceSeconds` to `Slide` — null = host advances manually.

## Backend changes

- `ElementScorer` updates:
  - `TextQuestion`: when `fuzzyMatch`, do Levenshtein comparison against `correctAnswer` + `acceptedVariants`
  - `McqQuestion`: when `allowMultipleSelect=false` and `optionIds.size() > 1`, reject the answer as malformed (or trim to first id, but rejection is cleaner)
  - `NumberQuestion`: when `minValue`/`maxValue` set, reject answers outside the range as invalid (no scoring)
- `ShowcaseService.startRound`:
  - If `mcq.shuffleOptions`, send a per-player shuffled order of `options[]` (deterministic on `roomCode + elementId + playerId` so the same player sees the same shuffle on reconnect)
  - If `ranking.shuffleItemsForPresentation`, same pattern for `items[]`
- `useCreateDashboard.buildNewElement` — set sensible defaults for every new field per kind

## Frontend changes

- Editor inspectors get the new controls per kind (toggles, sliders, distance input)
- Slide editor switches from single TipTap field to a stacked `SlideBlock` editor with add/remove/reorder
- Question editor toolbar adds a "Tags" affordance reusing the `TagPicker` from chunk 01
- Provenance footer in the inspector: "Created by X, last edited by Y, vN"

## Migration

- One-time script: for every existing element, set `createdByUserId = deck.creatorUserId`, `createdAt = deck.createdAt`, `version = 1`, `reactionsEnabled = true`, `tagIds = []`
- For Slide, set `blocks = [{kind: "body", richBody: oldBody}]` when `body` is non-empty

## Checklist

- [ ] Interface defaults + record updates across all DeckElement permits
- [ ] `useElementEditor.ts` sets `lastEditedByUserId` + bumps `version` on commit
- [ ] McqQuestion / TextQuestion / NumberQuestion / RankingQuestion / QAndAQuestion field additions
- [ ] `SlideBlock` polymorphic record + Slide migration to `blocks[]`
- [ ] `Slide.autoAdvanceSeconds`
- [ ] `ElementScorer` updates (fuzzy match, range validation, multi-select rejection)
- [ ] `ShowcaseService.startRound` deterministic per-player shuffle
- [ ] One-time migration script
- [ ] Editor inspector controls per kind
- [ ] Slide editor switches to block-based
- [ ] Provenance footer in inspector
- [ ] Frontend codegen + lint
- [ ] Backend tests pass
