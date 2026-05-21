# BrainFlex — Mentimeter/Kahoot Parity Roadmap

This folder tracks the work needed to bring the BrainFlex backend (and the frontend that depends on it) to feature parity with Mentimeter and Kahoot. Each numbered subfolder is a self-contained chunk of work with its own `README.md` describing scope, models, endpoints, and a checklist.

For the loose, unstructured component/feature checklist see [todo.md](todo.md).

For a cross-cutting audit of the Java `model/` and `dto/` packages (repetition, fields on the wrong owner, sealed-hierarchy inconsistencies, naming/typing gaps), see [java-model-issues.md](java-model-issues.md).

## How to use this

- Each chunk lives in its own subfolder so notes, scratch code, migration scripts, and design docs can be added next to the README later.
- The order below is the **recommended** order. Some chunks can be done in parallel — see the dependency notes inside each chunk README.
- When you finish a chunk, tick the box here **and** in the chunk's own README.
- Don't shortcut the rules in `AGENTS.md`: tests, lint, codegen, and the existing patterns (debounced commit, RTK Query cache-sync, sealed DeckElement / AnswerPayload) apply to every chunk.

## Progress tracker

### Discovery & social

- [x] **01** — **ARCHIVED** — [Tags & taxonomy](../archive/to-do/01-tags-and-taxonomy/README.md) — promote free-form `tags: List<String>` to a real `Tag` model
- [x] **02** — **ARCHIVED** — [Deck discovery metadata](../archive/to-do/02-deck-discovery-metadata/README.md) — `publishStatus`, `language`, rating/play counters, license
- [x] **03** — **ARCHIVED** — [Deck favorites](../archive/to-do/03-deck-favorites/README.md) — `DeckFavorite` join collection + star icon
- [x] **04** — **ARCHIVED** — [Deck ratings & comments](../archive/to-do/04-deck-ratings-and-comments/README.md) — 1–5 star ratings and threaded comments
- [x] **05** — **ARCHIVED** — [Deck collections](../archive/to-do/05-deck-collections/README.md) — user/org folders of decks
- [x] **06** — **ARCHIVED** — [Deck collaborators](../archive/to-do/06-deck-collaborators/README.md) — co-editors on a deck

### New element kinds

- [x] **07** — **ARCHIVED** — [Word Cloud](../archive/to-do/07-element-word-cloud-and-true-false/README.md) — Mentimeter-style survey kind (True/False dropped: an MCQ with 2 options covers that use case)
- [x] **08** — **ARCHIVED** — [Allocation & Matching](../archive/to-do/08-element-allocation-and-matching/README.md) — Mentimeter "100 points" and Kahoot "puzzle pairs"
- [x] **09** — **ARCHIVED** — [Drawing](../archive/to-do/09-element-drawing/README.md) — open canvas with stroke-based answers
- [x] **10** — **ARCHIVED** — [Common element additions](../archive/to-do/10-element-common-additions/README.md) — provenance, shuffle, fuzzy match, slide blocks

### Live game polish

- [x] **11** — [InteractiveSession reactions & chat](./11-interactive-session-reactions-and-chat/README.md) — emoji reactions + audience chat *(complete: backend, codegen, ReactionBar / ReactionRain / ChatPanel mounted on PlayPage, host hover-to-hide moderation, optimistic chat send + STOMP reconcile)*
- [x] **12** — [InteractiveSession teams](./12-interactive-session-teams/README.md) — team mode + team leaderboard *(complete: backend, codegen, lobby `TeamPicker` grid with host create/rename/recolor/delete + per-member "Move to…" select, side-by-side `TeamLeaderboard` on `PlayPage`, per-row team chip on `ScoreBoard`, `TeamPodium` on `GameOver`, STOMP `/teams` subscription on `useInteractiveSessionWebSocket`)*
- [x] **13** — [InteractiveSession settings & player additions](./13-interactive-session-settings-and-player-additions/README.md) — shuffle, auto-advance, podium, avatars, streaks, answer timing *(complete: backend + codegen + UI; `CreateGamePage` "More options" exposes shuffleQuestions/shuffleAnswers/autoAdvance/podiumDuration/lobbyCountdownSeconds/requireFullName/spectatorsAllowed, lobby renders an `AvatarSelector` backed by `useListAvatarsQuery` + the new `PUT /api/interactive-sessions/{roomCode}/me/avatar` endpoint, lobby header surfaces `customRoomCode` + host name/avatar, `PlayPage` shows a streak banner + per-row streak chips on `ScoreBoard` + host autoAdvance countdown ring, `GameOver` placement chips for accuracy/streak/speed/reactions backed by the new `PlayerPlacement.speedBonusTotal` snapshot)*
- [x] **14** — **ARCHIVED** — [Scheduled interactive sessions & invites](../archive/to-do/14-scheduled-interactive-sessions-and-invites/README.md) — schedule a session and email invites

### Analytics & reporting

- [x] **15** — [Game history](./15-game-history/README.md) — per-user `GameHistoryEntry` *(backend + tests done; profile history tab / your-best widget / stats cards deferred to the holistic chunk-13 player-UI pass)*
- [x] **16** — [Deck analytics](./16-deck-analytics/README.md) — rolled-up `DeckAnalytics` + per-element stats + reports *(PR1 + PR2 done: models, per-kind bucketing for all 13 ElementKinds, backfill, GET endpoint, CSV export, analytics dashboard with KPI strip + per-element accordion, deck-editor "Analytics" tab; PDF export and per-show `InteractiveSessionResult.exportedReportUrl` CSV deferred — revisit if there's demand)*
- [x] **17** — [Achievements](./17-achievements/README.md) — `Achievement` + `UserAchievement` + trigger evaluator *(backend + tests + frontend done: 10-trigger enum, fire-and-forget `AchievementService.evaluate`, trigger wiring in `GameHistoryService.recordFinish` + `DeckService.create/publish` + `DeckFavoriteService.favorite` + `InteractiveSessionService.updateStatsAfterGame`, 3 endpoints, 18-row seed catalog, `/achievements` catalog route, profile "Achievements" tab; `REACTIONS_SENT` + `WORD_CLOUD_SUBMITTED` triggers and the game-results "Achievement unlocked" surface + toast deferred to the holistic chunk-13 player-UI pass)*
- [x] **18** — [Notifications](./18-notifications/README.md) — in-app notification stream *(backend `Notification` model + repo + 90d TTL + per-user STOMP push + 7 ApplicationEvent listeners, `DECK_FAVORITED` Redis throttle, 5 REST endpoints; frontend `NotificationBell` with unread badge + grouped dropdown, STOMP subscription via `useNotificationStream`, RTK Query 60s polling fallback, optimistic mark-read / read-all / dismiss cache enhancements; toast-on-receive deferred until chunk 20 `notificationPrefs` lands)*

### Platform

- [ ] ~~**19** — [Media asset](./19-media-asset/README.md) — generalize `GalleryImage` to audio/video upload~~ — **Deferred (future feature).** Not pursuing audio/video upload for now; YouTube link embedding may be revisited later.
- [x] **20** — [User/Org/Theme/Membership additions](./20-user-organization-theme-membership-additions/README.md) — catch-all field additions *(backend complete: model additions + `UserProfileBackfillMigration` (`scripts/migrate-user-profile.sh`) + `NotificationPrefs` GET/PUT (`/api/users/me/notification-prefs`) + three Organization endpoints (`PUT /api/organizations/{id}`, `POST /api/organizations/{id}/invite-code/rotate`, `POST /api/organizations/join-by-code`) + OAuth email-domain auto-join hook on existing-user / guest-conversion / brand-new-registration paths + `MembershipService.canStartInteractiveSession` quota gate (HTTP 402 on `POST /api/interactive-sessions`) + `PlayerStatsResetScheduler` weekly (`MON 00:00 UTC`) + monthly (`1st 00:00 UTC`) crons + `DeckController.recountFavorites` and `TagController.update/delete` migrated to `@PreAuthorize("hasRole('ADMIN')")` (`TestSecurityConfig` mirrors prod role hierarchy + `@EnableMethodSecurity` so the gate fires under MockMvc) + frontend codegen regenerated; 500/500 tests green; frontend profile / org-settings / notification-prefs / `useTheme.tokenOverrides` pages deferred to a frontend pass)*

### Right-sidebar polish

- [ ] **21** — [Slide design shape & sidebar polish](./21-slide-design-and-sidebar-polish/README.md) — `EditSlidePanel` chart picker + `Design` value object + custom subjects/tags + finish review posting *(Part B + Part C closed out: chart picker, `selectionsPerParticipant=0`, `showResultsAsPercentage`, two-toggle QR/join UI, `showResponses` radio + `PRIVATE` gate in `InteractiveSessionService.submitAnswer` with test, inline-create subject + tags, `Tag.createdByUserId` + `?createdByMe=true` filter, Explore curated default, reviews-panel textarea + cache invalidation all done; `Design` value object + `DesignEditor` and the `titleLabel` UI surfacing + render still TODO — A.7 backend is being delivered via the chunk-25 `ElementChrome` refactor in flight)*

### Editor shell & shared components

- [x] **22** — **ARCHIVED** — [Common-component gaps](../archive/to-do/22-common-component-gaps/README.md) — Pagination + inline Alert/Banner; sweep existing one-off error spans
- [ ] **23** — [Deck editor shell polish](./23-deck-editor-shell-polish/README.md) — Preview / Start (interactive session) navbar buttons, new-deck first-slide skeleton, right-sidebar vertical icon rail + drawer-on-drawer (Edit slide / Theme / Participants / Sharing) *(Start button + first-slide skeleton + vertical icon rail with 7 drawer panels all landed; Preview button still `console.log`, empty-deck guard + tests still TODO, `SharingPreferencesPanel` still a "Coming soon" placeholder, `ParticipantsPanel` only hosts the reactions toggle today — collaborator list TODO)*
- [x] **25** — [Per-kind slide editor hooks](./25-per-kind-slide-editor-hooks/README.md) — Extend the `useElementEditor`/`useMcq*Editor` pattern across every other slide kind (Text/Number/WordCloud/QAndA/Drawing/PlaceOnImage/Grid/Allocation/Matching/Ranking/Scales/Slide) so per-kind buildPatch / bounds / structural-op plumbing leaves the components *(all 13 hooks + 12 component migrations landed; per-item card extraction and drag-to-reorder remain follow-up work)*

### Interactive session runtime

- [x] **24** — **ARCHIVED** — [Session format & runtime cascades](../archive/to-do/24-session-format-and-runtime-cascades/README.md) — rename `DeckPreset` → `SessionFormat` (drop `PULSE`), make it session-authoritative with deck as default, formalize the session > deck > element cascade for `showResponses`, rename `mode` → `answerSubmissionMode`, pluggable best-answer scoring (`POINTS_PER_VOTE` + `FLAT_WINNER`), reveal-on-demand + freeze-responses host controls (host overlays now exposed on `InteractiveSessionDTO` so reconnects rebuild reveal/freeze state), PRESENTATION RoundDataView + SessionSummary, deck-editor `defaultSessionFormat` / `defaultShowResponses` panel, shared `BehaviorSection` for per-element `showResponses` *(backend-up `BrainFlexApi.ts` codegen run completed)*

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
24 is independent of 21–23 but touches the same RoundResult / GameOver surfaces as the chunk-13 deferred player-UI pass; land before or alongside that pass to avoid double-touching those components
25 is independent — pure frontend plumbing on top of the existing `useElementEditor` base; no backend changes
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

- **Game history UI (chunk 15).** Chunk 15 shipped the backend (`GameHistoryEntry` model + `(userId, interactiveSessionId)` unique index, `GameHistoryService.recordFinish` writing per-player + standalone-host rows from `InteractiveSessionService.endGame`, `PlayerStats.currentStreak` reinterpreted as a never-resetting tally of games played, `Membership.monthlyInteractiveSessionCount` + `monthlyCountPeriodStart` with month-boundary rollover, three GET endpoints under `/api/users/me/history`, `/api/users/{userId}/history`, `/api/decks/{deckId}/history/mine`, plus a `GameHistoryBackfillMigration` gated on `--migrate.game-history=true`), but **no frontend yet** (codegen has not been regenerated). Specifically still TODO:
  - Profile page "History" tab with paginated list (deck cover + name, placement badge, score, host, relative played-at)
  - Stats summary cards above the list (total games, best placement, total points, average accuracy)
  - "Your best: 1,240 (rank #3 of 12)" widget on the deck detail page driven by `/api/decks/{deckId}/history/mine`
  - Frontend codegen + RTK Query cache-sync enhancements

  These cluster with **chunk 13** alongside the other deferred player/host surfaces — the deck-detail "your best" widget is small enough to land standalone, but the profile history tab + summary cards share styling decisions with the chunk-13 placement card and read most naturally next to the deferred chunk 11/12/13 work.

- **Achievement player-surface + missing triggers (chunk 17).** Chunk 17 shipped the catalog backend (`Achievement` + `UserAchievement` models with `(userId, achievementId)` unique index, `AchievementTrigger` enum, fire-and-forget `AchievementService.evaluate`, trigger wiring across `GameHistoryService.recordFinish` / `DeckService.createDeck` / `DeckService.publish` / `DeckFavoriteService.favorite` / `InteractiveSessionService.updateStatsAfterGame`, three GET endpoints, 18-row seed catalog) and the frontend catalog + profile tab, but two deferrals remain:
  - **Game-results "Achievement unlocked" cards.** The spec calls for showing newly-earned achievements before the standard placement card. `AchievementService.evaluate` already returns the awarded list per-call, so the wiring on the player side is mostly UI — but it shares layout decisions with the deferred chunk-13 placement card and is cheaper to land in the same pass.
  - **Achievement toast component.** Per-user live notification when a badge fires. Needs either chunk 18 (notification stream) to ship first or a stop-gap `SimpMessagingTemplate.convertAndSendToUser` push from `AchievementService` — re-open when chunk 18 starts.
  - **`REACTIONS_SENT` + `WORD_CLOUD_SUBMITTED` triggers.** Both would require either a lifetime counter on `User.stats` (cheap) or a full history aggregation per game-end (expensive). Skipped for the first cut — add a counter and re-enable the enum values when there's a use case.

  These cluster with **chunk 13** for the player-surface portion and **chunk 18** for the toast — the catalog browse + profile summary tab are already live and don't depend on the deferred items.
