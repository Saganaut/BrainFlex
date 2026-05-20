# BrainFlex — Mentimeter/Kahoot Parity Roadmap

This folder tracks the work needed to bring the BrainFlex backend (and the frontend that depends on it) to feature parity with Mentimeter and Kahoot. Each numbered subfolder is a self-contained chunk of work with its own `README.md` describing scope, models, endpoints, and a checklist.

For the loose, unstructured component/feature checklist see [todo.md](todo.md).

## How to use this

- Each chunk lives in its own subfolder so notes, scratch code, migration scripts, and design docs can be added next to the README later.
- The order below is the **recommended** order. Some chunks can be done in parallel — see the dependency notes inside each chunk README.
- When you finish a chunk, tick the box here **and** in the chunk's own README.
- Don't shortcut the rules in `AGENTS.md`: tests, lint, codegen, and the existing patterns (debounced commit, RTK Query cache-sync, sealed DeckElement / AnswerPayload) apply to every chunk.

## Progress tracker

### Discovery & social

- [x] **01** — [Tags & taxonomy](./01-tags-and-taxonomy/README.md) — promote free-form `tags: List<String>` to a real `Tag` model
- [x] **02** — [Deck discovery metadata](./02-deck-discovery-metadata/README.md) — `publishStatus`, `language`, rating/play counters, license
- [x] **03** — [Deck favorites](./03-deck-favorites/README.md) — `DeckFavorite` join collection + star icon
- [x] **04** — [Deck ratings & comments](./04-deck-ratings-and-comments/README.md) — 1–5 star ratings and threaded comments
- [x] **05** — [Deck collections](./05-deck-collections/README.md) — user/org folders of decks
- [x] **06** — [Deck collaborators](./06-deck-collaborators/README.md) — co-editors on a deck

### New element kinds

- [x] **07** — [Word Cloud](./07-element-word-cloud-and-true-false/README.md) — Mentimeter-style survey kind (True/False dropped: an MCQ with 2 options covers that use case)
- [x] **08** — [Allocation & Matching](./08-element-allocation-and-matching/README.md) — Mentimeter "100 points" and Kahoot "puzzle pairs"
- [x] **09** — [Drawing](./09-element-drawing/README.md) — open canvas with stroke-based answers
- [x] **10** — [Common element additions](./10-element-common-additions/README.md) — provenance, shuffle, fuzzy match, slide blocks

### Live game polish

- [x] **11** — [InteractiveSession reactions & chat](./11-interactive-session-reactions-and-chat/README.md) — emoji reactions + audience chat *(backend + codegen done; player/host UI deferred to chunk 13)*
- [x] **12** — [InteractiveSession teams](./12-interactive-session-teams/README.md) — team mode + team leaderboard *(backend + codegen done; lobby team picker / team leaderboard / team podium deferred to chunk 13)*
- [x] **13** — [InteractiveSession settings & player additions](./13-interactive-session-settings-and-player-additions/README.md) — shuffle, auto-advance, podium, avatars, streaks, answer timing *(backend + codegen done; lobby avatar picker / streak indicator / autoAdvance ring / placement card UI deferred to the holistic chunk-13 player-UI pass)*
- [x] **14** — [Scheduled interactive sessions & invites](./14-scheduled-interactive-sessions-and-invites/README.md) — schedule a session and email invites

### Analytics & reporting

- [x] **15** — [Game history](./15-game-history/README.md) — per-user `GameHistoryEntry` *(backend + tests done; profile history tab / your-best widget / stats cards deferred to the holistic chunk-13 player-UI pass)*
- [ ] **16** — [Deck analytics](./16-deck-analytics/README.md) — rolled-up `DeckAnalytics` + per-element stats + reports
- [ ] **17** — [Achievements](./17-achievements/README.md) — `Achievement` + `UserAchievement` + trigger evaluator
- [ ] **18** — [Notifications](./18-notifications/README.md) — in-app notification stream

### Platform

- [ ] **19** — [Media asset](./19-media-asset/README.md) — generalize `GalleryImage` to audio/video upload
- [ ] **20** — [User/Org/Theme/Membership additions](./20-user-organization-theme-membership-additions/README.md) — catch-all field additions

### Right-sidebar polish

- [ ] **21** — [Slide design shape & sidebar polish](./21-slide-design-and-sidebar-polish/README.md) — `EditSlidePanel` chart picker + `Design` value object + custom subjects/tags + finish review posting

### Editor shell & shared components

- [ ] **22** — [Common-component gaps](./22-common-component-gaps/README.md) — Pagination + inline Alert/Banner; sweep existing one-off error spans
- [ ] **23** — [Deck editor shell polish](./23-deck-editor-shell-polish/README.md) — Preview / Start (interactive session) navbar buttons, new-deck first-slide skeleton, right-sidebar vertical icon rail + drawer-on-drawer (Edit slide / Theme / Participants / Sharing)

## Dependency graph

```
01 ──┬─> 02 ──┬─> 03
     │       ├─> 04
     │       └─> 05
     └─> 10
02 ──> 06
07, 08, 09 are independent of each other (each adds a new ElementKind + AnswerPayload)
07–10 should land before 16 so analytics knows about every element kind
11 ──> 13 (reactionsSent counter on InteractiveSessionPlayer)
12 ──> 13 (teamMode flag)
13 ──> 15 ──> 16 (game history & analytics need timeTakenMs / streak fields)
15 ──> 17 (achievement triggers read GameHistoryEntry)
18 depends on nothing but is more useful after 04, 06, 14, 17 exist
19 can land any time; chunk 10 will reference it if audio/video upload is wanted alongside provenance
20 can land any time; it's a catch-all and may be split inline as you do other chunks
21 leans on 01 (TagPicker create affordance) and 04 (rating mutation) — backend foundations exist, this finishes the UI
22 is independent — Pagination + Alert can be picked up any time
23 should land before (or in the same PR series as) 21 — chunk 21 modifies the content of panels that chunk 23 restructures the shell around
```

## Cross-cutting reminders

- **Don't edit `BrainFlexApi.ts` by hand** — regenerate via `npx @rtk-query/codegen-openapi openapi-config.cts` after every backend model change.
- **`enhanceEndpoints` for cache sync** — every new mutation that affects deck/interactive session state needs a matching `onQueryStarted` in `frontend/src/store/apiEnhancements.ts`.
- **Sealed types** — `DeckElement` and `AnswerPayload` are sealed. When you add a new element kind you must extend the `permits` list **and** handle it in `ElementScorer`, `ElementRedactor`, `DeckElementCloner`, `DeckImageHydrationService`, and `DeckImageMapper`.
- **Element payload primitives** — every payload must include defaults for `displaySeconds`, `pointValue`, `bestAnswerMode`, `bestAnswerBonus`, etc. (Jackson can't deserialize `null` into a primitive). See `useDeckEditor.ts:buildNewElement`.
- **Tests** — every chunk must add tests. Service tests go under `src/test/.../service/`, controller tests under `src/test/.../controller/`. Mock `MongoTemplate` correctly (see existing `HealthControllerTest`).
- **Tokens** — every new piece of UI uses the design tokens from `frontend/src/tokens.css`. No hardcoded colors, no `box-shadow`, no palette references in components.

## Deferred work — come back to this

These items were intentionally left out of the chunk that nominally owns them because they don't fit until a different chunk lands first. Each one is unblocked by a specific later chunk; revisit when that chunk starts so the per-kind work all lands in one consistent pass.

- **Per-kind player surfaces (chunk 08).** Chunk 08 shipped the backend + author editor for Allocation ("100 points") and Matching ("puzzle pairs"), but `ElementRenderer` has no case for either — they fall through to the default `return null` branch, so a player sees nothing on those rounds. (Word Cloud — chunk 07 — and Drawing — chunk 09 — both have full player + reveal surfaces.) Specifically still TODO:
  - Allocation player surface (slider/stepper per option that sums to 100; reveal shows the room-aggregate vs. correct allocation)
  - Matching player surface (drag-pair or tap-to-pair UX; reveal shows correct pairings highlighted)

  These naturally cluster with **chunk 13** (InteractiveSession settings & player additions) since that chunk already touches the player UI. Re-open this list when starting chunk 13.

- **Team-mode UI (chunk 12).** Chunk 12 shipped the backend (`Team` embedded model, `InteractiveSession.teams` + `teamMode` + `autoBalanceTeams`, `InteractiveSessionSettings.teamMode`/`teamCount`/`autoBalanceTeams`, `InteractiveSessionPlayer.teamId`, `PlayerPlacement.teamId`, deterministic team seeding + auto-balance join + manual join, per-answer team-score recompute + Best-Answer reveal recompute, host CRUD endpoints, `TeamUpdateMessage` STOMP broadcasts) and the codegen hooks (`useCreateTeamMutation` / `useUpdateTeamMutation` / `useDeleteTeamMutation` / `useMovePlayerToTeamMutation` plus the optional `JoinInteractiveSessionRequest` body), but **no team-mode UI**. Specifically still TODO:
  - Team-mode toggle + team-count slider on `CreateGamePage.tsx`
  - Lobby team picker grid (one card per team with color, name, current member list; "Auto-assign" button when `autoBalanceTeams`)
  - Host team edit affordances (create/rename/recolor/delete; move-player drop target)
  - Team badge under the player's score on `PlayPage.tsx`
  - Team leaderboard alternating with individual leaderboard between rounds
  - Team podium on the results page (top 3 teams + per-team MVP)
  - Subscribe to `/topic/interactive-session/{roomCode}/teams` in `useGameWebSocket` so team scores tick live

  These cluster with **chunk 13** for the same reason as chunks 7–9 and chunk 11: chunk 13 is the holistic player-UI pass, so building the team-mode shell in isolation would establish a layout pattern the other deferred work would have to match.

- **InteractiveSession settings & player additions UI (chunk 13).** Chunk 13 shipped the backend (field additions on InteractiveSession / InteractiveSessionSettings / InteractiveSessionPlayer / PlayerAnswer / PlayerPlacement, `customRoomCode` validation + collision check, `shuffleQuestions` at game start, `shuffleAnswers` gated by settings on top of the chunk-10 per-element flag, `speedBonusAwarded` split out of base points, `currentStreak`/`longestStreak`/`accuracy` updates, `autoAdvance` scheduling for TURN_BASED mode, 16-preset `AvatarService` + `GET /api/avatars` + lobby join wiring, `PresenceService` → `InteractiveSessionPlayer.disconnected`/`lastSeenAt`) and the codegen hooks, but **the player + host UI is deferred**. Specifically still TODO:
  - InteractiveSession create form: "Advanced" section exposing all new toggles (anonymousMode, customRoomCode, shuffleQuestions, shuffleAnswers, autoAdvance, podiumDuration, lobbyCountdownSeconds, requireFullName, spectatorsAllowed)
  - Lobby avatar picker — grid backed by `useListAvatarsQuery`
  - Streak indicator on `PlayPage` ("3x streak 🔥")
  - Host autoAdvance progress ring (uses the new `InteractiveSession.lobbyOpenedAt` + `settings.podiumDuration`)
  - Per-player `accuracy` / `longestStreak` / `speedBonusTotal` chips on the placement card
  - Custom room code displayed prominently in the lobby header (denormalized `hostName` / `hostAvatarUrl` also available)

  These cluster with the same chunk-13 holistic pass as everything else below: re-open this list when starting the player-UI work.

- **Audience engagement UI (chunk 11).** Chunk 11 shipped the backend (models, REST + STOMP, rate limiting, emoji allow-list, Redis aggregation, host moderation) and the codegen hooks (`useSendReactionMutation` / `useSendChatMutation` / `useListChatQuery` / `useModerateChatMutation`), but **no player or host UI**. Specifically still TODO:
  - `ReactionBar` on the player view (six default emojis + long-press picker, sends via STOMP or REST fallback)
  - `ReactionRain` on the host view (subscribes to `/topic/interactive-session/{roomCode}/reaction`, animates emoji bursts using CSS transforms + RAF)
  - `ChatPanel` sidebar (host + player views, toggleable, last-50 replay on mount, host messages styled distinctively)
  - Host moderation interaction (hover-to-hide, server flips `moderated=true`, non-hosts re-render the row as "(hidden by host)")
  - Optimistic chat send + STOMP reconcile (apiEnhancements `onQueryStarted` for `sendChat`)

  These cluster with **chunk 13** for the same reason as the per-kind player surfaces above: chunk 13 is the holistic player-UI pass, and building the reaction/chat shell in isolation would establish a layout pattern the other deferred work would have to match.

- **Game history UI (chunk 15).** Chunk 15 shipped the backend (`GameHistoryEntry` model + `(userId, interactiveSessionId)` unique index, `GameHistoryService.recordFinish` writing per-player + standalone-host rows from `InteractiveSessionService.endGame`, `PlayerStats.currentStreak` reinterpreted as a never-resetting tally of games played, `Membership.monthlyInteractiveSessionCount` + `monthlyCountPeriodStart` with month-boundary rollover, three GET endpoints under `/api/users/me/history`, `/api/users/{userId}/history`, `/api/decks/{deckId}/history/mine`, plus a `GameHistoryBackfillMigration` gated on `--migrate.game-history=true`), but **no frontend yet** (codegen has not been regenerated). Specifically still TODO:
  - Profile page "History" tab with paginated list (deck cover + name, placement badge, score, host, relative played-at)
  - Stats summary cards above the list (total games, best placement, total points, average accuracy)
  - "Your best: 1,240 (rank #3 of 12)" widget on the deck detail page driven by `/api/decks/{deckId}/history/mine`
  - Frontend codegen + RTK Query cache-sync enhancements

  These cluster with **chunk 13** alongside the other deferred player/host surfaces — the deck-detail "your best" widget is small enough to land standalone, but the profile history tab + summary cards share styling decisions with the chunk-13 placement card and read most naturally next to the deferred chunk 11/12/13 work.
