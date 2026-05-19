# BrainFlex — Mentimeter/Kahoot Parity Roadmap

This folder tracks the work needed to bring the BrainFlex backend (and the frontend that depends on it) to feature parity with Mentimeter and Kahoot. Each numbered subfolder is a self-contained chunk of work with its own `README.md` describing scope, models, endpoints, and a checklist.

## How to use this

- Each chunk lives in its own subfolder so notes, scratch code, migration scripts, and design docs can be added next to the README later.
- The order below is the **recommended** order. Some chunks can be done in parallel — see the dependency notes inside each chunk README.
- When you finish a chunk, tick the box here **and** in the chunk's own README.
- Don't shortcut the rules in `AGENTS.md`: tests, lint, codegen, and the existing patterns (debounced commit, RTK Query cache-sync, sealed DeckElement / AnswerPayload) apply to every chunk.

## Progress tracker

### Discovery & social

- [x] **01** — [Tags & taxonomy](./01-tags-and-taxonomy/README.md) — promote free-form `tags: List<String>` to a real `Tag` model
- [x] **02** — [Deck discovery metadata](./02-deck-discovery-metadata/README.md) — `publishStatus`, `language`, rating/play counters, license
- [ ] **03** — [Deck favorites](./03-deck-favorites/README.md) — `DeckFavorite` join collection + star icon
- [ ] **04** — [Deck ratings & comments](./04-deck-ratings-and-comments/README.md) — 1–5 star ratings and threaded comments
- [x] **05** — [Deck collections](./05-deck-collections/README.md) — user/org folders of decks
- [ ] **06** — [Deck collaborators](./06-deck-collaborators/README.md) — co-editors on a deck

### New element kinds

- [ ] **07** — [Word Cloud & True/False](./07-element-word-cloud-and-true-false/README.md) — two straightforward additions to `ElementKind`
- [ ] **08** — [Allocation & Matching](./08-element-allocation-and-matching/README.md) — Mentimeter "100 points" and Kahoot "puzzle pairs"
- [ ] **09** — [Drawing](./09-element-drawing/README.md) — open canvas with stroke-based answers
- [ ] **10** — [Common element additions](./10-element-common-additions/README.md) — provenance, shuffle, fuzzy match, slide blocks

### Live game polish

- [ ] **11** — [Showcase reactions & chat](./11-showcase-reactions-and-chat/README.md) — emoji reactions + audience chat
- [ ] **12** — [Showcase teams](./12-showcase-teams/README.md) — team mode + team leaderboard
- [ ] **13** — [Showcase settings & player additions](./13-showcase-settings-and-player-additions/README.md) — shuffle, auto-advance, podium, avatars, streaks, answer timing
- [ ] **14** — [Scheduled showcases & invites](./14-scheduled-showcases-and-invites/README.md) — schedule a game and email invites

### Analytics & reporting

- [ ] **15** — [Game history](./15-game-history/README.md) — per-user `GameHistoryEntry`
- [ ] **16** — [Deck analytics](./16-deck-analytics/README.md) — rolled-up `DeckAnalytics` + per-element stats + reports
- [ ] **17** — [Achievements](./17-achievements/README.md) — `Achievement` + `UserAchievement` + trigger evaluator
- [ ] **18** — [Notifications](./18-notifications/README.md) — in-app notification stream

### Platform

- [ ] **19** — [Media asset](./19-media-asset/README.md) — generalize `GalleryImage` to audio/video upload
- [ ] **20** — [User/Org/Theme/Membership additions](./20-user-organization-theme-membership-additions/README.md) — catch-all field additions

## Dependency graph

```
01 ──┬─> 02 ──┬─> 03
     │       ├─> 04
     │       └─> 05
     └─> 10
02 ──> 06
07, 08, 09 are independent of each other (each adds a new ElementKind + AnswerPayload)
07–10 should land before 16 so analytics knows about every element kind
11 ──> 13 (reactionsSent counter on ShowcasePlayer)
12 ──> 13 (teamMode flag)
13 ──> 15 ──> 16 (game history & analytics need timeTakenMs / streak fields)
15 ──> 17 (achievement triggers read GameHistoryEntry)
18 depends on nothing but is more useful after 04, 06, 14, 17 exist
19 can land any time; chunk 10 will reference it if audio/video upload is wanted alongside provenance
20 can land any time; it's a catch-all and may be split inline as you do other chunks
```

## Cross-cutting reminders

- **Don't edit `BrainFlexApi.ts` by hand** — regenerate via `npx @rtk-query/codegen-openapi openapi-config.cts` after every backend model change.
- **`enhanceEndpoints` for cache sync** — every new mutation that affects deck/showcase state needs a matching `onQueryStarted` in `frontend/src/store/apiEnhancements.ts`.
- **Sealed types** — `DeckElement` and `AnswerPayload` are sealed. When you add a new element kind you must extend the `permits` list **and** handle it in `ElementScorer`, `ElementRedactor`, `DeckElementCloner`, `DeckImageHydrationService`, and `DeckImageMapper`.
- **Element payload primitives** — every payload must include defaults for `displaySeconds`, `pointValue`, `bestAnswerMode`, `bestAnswerBonus`, etc. (Jackson can't deserialize `null` into a primitive). See `useCreateDashboard.ts:buildNewElement`.
- **Tests** — every chunk must add tests. Service tests go under `src/test/.../service/`, controller tests under `src/test/.../controller/`. Mock `MongoTemplate` correctly (see existing `HealthControllerTest`).
- **Tokens** — every new piece of UI uses the design tokens from `frontend/src/tokens.css`. No hardcoded colors, no `box-shadow`, no palette references in components.
