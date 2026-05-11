# Showcases & Decks — Implementation Checklist

## Vocabulary (unified model)

A **Deck** is the authored content: an ordered list of **elements** (questions, slides, sections). It is what the host builds in the editor.

A **Showcase** is a live run of a Deck with a host and participants. Sessions are persisted, broadcast over WebSocket, and have a lifecycle (LOBBY → IN_PROGRESS → FINISHED).

A Showcase has presets that toggle behavior:

- **Game** preset — `scoringEnabled: true`, leaderboard, GameOver screen. (Default; the only preset surfaced today.)
- **Pulse** preset — `scoringEnabled: false`, no leaderboard, data-tracking review only. (Authoring surface not yet built.)
- **Presentation** preset (future) — `scoringEnabled: false`, host paces the deck slide-by-slide, no time pressure.

User-facing routes still split "Create a Game" (`/games/create`) and "Create a Poll" (`/pulse/create`); both ultimately persist `Showcase`s with different presets.

Status legend: ✅ shipped · 🚧 partial · ☐ todo

---

## 0. Model evolution roadmap

> **Update (2026-05-11):** The polymorphic element rework has shipped. `Question` was retired; `Deck.elements` is now an embedded ordered list of sealed `DeckElement` records (`Slide`, `McqQuestion`, `TextQuestion`, `NumberQuestion`, `ImageChoiceQuestion`, `RankingQuestion`, `ScalesQuestion`, `QAndAQuestion`, `GridQuestion`, `PlaceOnImageQuestion`). Answers are likewise a sealed `AnswerPayload` family. Sample data was wiped and reseeded (Welcome Tour + General Knowledge). Most of the field gaps below are now satisfied at the model layer; runtime renderers / authoring UI catch up element-by-element (§3a, §7).

### Deck — fields

- ✅ `position`-supporting ordering for elements (embedded element list; order is positional)
- ✅ `coverImageUrl` — thumbnail shown on template tiles and the My Decks list
- ✅ `backgroundImageUrl` — deck-level background; cascade implemented at deck → Lorem Picsum, theme tier still pending
- ✅ `tags: List<String>` — multi-tag categorization (replaced the single `category` field)
- ✅ `organizationId` — org-scoped sharing (parallels `User.organizationId`)
- ✅ `visibility: PRIVATE | UNLISTED | ORG | PUBLIC` — replaced boolean `isPublic`
- ☐ `themeId` — link to the host's `Theme` so deck inherits color scheme during play
- ☐ `recommendedPreset: GAME | PULSE | PRESENTATION` — advisory; sets defaults in the create flow
- ☐ `defaultSettings: ShowcaseSettings` — author-suggested showcase settings auto-applied at create time
- ☐ `estimatedDurationMinutes` — "~10 min" hint for template browsing
- ☐ `parentDeckId` + `version` — for fork-this-template + history
- ☐ `updatedAt`

### Element — shared chrome (every `DeckElement` record)

- ✅ `id`, `kind` (Jackson `@JsonTypeInfo` discriminator)
- ✅ `displaySeconds` — per-element override; always wins over `Showcase.timePerQuestion`
- ✅ `hostNotes` — speaker notes; sent only to the host's principal queue (redactor strips for public broadcast)
- ✅ `backgroundImageUrl` — per-element background override
- ✅ `imageUrl`, `videoUrl`, `audioUrl` — media URLs
- ✅ `mediaPosition: NONE | TOP | BOTTOM | BACKGROUND` — author chooses where media renders
- ☐ `mediaCaption / altText` — accessibility + caption support
- ☐ `explanation` — post-answer "Here's why" copy
- ☐ `scoringEnabledOverride` — per-element opt-out from scoring (icebreakers, Pulse-style elements in a Game showcase)
- ☐ `updatedAt`

### Element — type-specific fields (all modeled)

- ✅ `Slide` — `title`, `body`, `slideKind: TITLE | SECTION | CALLOUT | CONTENT | END`, `bodyMarkdown`
- ✅ `McqQuestion` — `prompt`, `options`, `correctOptionId`, `pointValue`, `difficulty`, `bestAnswerMode`, `bestAnswerBonus`
- ✅ `TextQuestion` — `prompt`, `correctAnswer`, `acceptedVariants`, `caseSensitive`, `pointValue`, `difficulty`
- ✅ `NumberQuestion` — `prompt`, `correctValue`, `tolerance`, `unitLabel`, `pointValue`, `difficulty`
- ✅ `ImageChoiceQuestion` — `prompt`, `options` (with `imageUrl`), `correctOptionId`, `pointValue`, `difficulty`
- ✅ `RankingQuestion` — `prompt`, `items`, `scoringMode: PARTIAL | EXACT`, `pointValue`, `difficulty`
- ✅ `ScalesQuestion` — `prompt`, `statements`, `scaleMin`, `scaleMax`, `correctRatings`, `pointValue`, `difficulty`
- ✅ `QAndAQuestion` — `prompt`, `bestAnswerMode`, `bestAnswerBonus` (always free-form; relies on AudienceSubmission)
- ✅ `GridQuestion` — `prompt`, `rows`, `cols`, `cellLabels`, `gridImageUrl`, `correctCells`, `pointValue`, `difficulty`
- ✅ `PlaceOnImageQuestion` — `prompt`, `targetImageUrl`, `correctX`, `correctY`, `tolerance` (all 0–1 normalized), `pointValue`, `difficulty`, `bestAnswerMode`, `bestAnswerBonus`

### New collections

- ✅ `AudienceSubmission` — for Q&A and Best Answer mode. `{ id, showcaseId, elementId, userId, payload (AnswerPayload), status: PENDING/PINNED/DISMISSED, upvotes, submittedAt }`. Repository + model shipped; live moderation UI still ☐.
- ✅ `BestAnswerVote` — `{ id, showcaseId, submissionId, voterUserId, votedAt }`. Repository + model shipped; second-phase runtime still ☐ (see §3a-bis).
- ✅ Polymorphic `AnswerPayload` (sealed): `McqAnswer`, `TextAnswer`, `NumberAnswer`, `ImageChoiceAnswer`, `RankingAnswer`, `ScalesAnswer`, `GridAnswer`, `PlaceOnImageAnswer`, `TimeoutAnswer`. Stored on `PlayerAnswer.payload`; scored by `ElementScorer` dispatch.

### Cross-cutting

- ✅ **Polymorphic scoring** — `ElementScorer.score(DeckElement, AnswerPayload)` dispatches per-kind (case-insensitive text + variants, numeric tolerance, ranking PARTIAL vs EXACT, grid set comparison, place-on-image linear distance vs tolerance)
- ✅ **Element redaction** — `ElementRedactor` strips correct-answer fields before broadcasting on `/topic/showcase/{code}/round`; un-redacted form goes to the host's principal queue and to the post-round reveal
- ✅ **Frontend shuffle** — server-side `shuffleMcqOptions` was dropped; the editor will own a shuffle / reorder button (cleaner model, no per-session shuffle state)
- ☐ **Background image cascade** documented and implemented: element → deck → theme → Lorem Picsum placeholder (fields exist; renderer cascade still pending)
- ☐ **Theme integration into Showcases** — the host's active theme drives colors / fonts for every participant during a showcase
- ☐ **Element pool sampling** when `totalRounds < deck.size` — currently first-N; future: always include title slide, optionally stratify by difficulty
- ☐ **Real-time host preview** — host's view shows the correct answer during play
- ☐ **Element validation / "ready" flag** — draft elements with missing required fields can't be included in a playable showcase
- ☐ **Question banks / cross-deck reuse** — elements live inside a single deck today; long term we may want a shared element pool
- ☐ **Branching** (far future) — "skip Q2 if everyone got Q1 right". Out of scope; flagged to avoid baking incompatible assumptions

---

## 1. Entry flow

- ✅ **MainPage** prompts Create Game / Create Poll / Join — `frontend/src/pages/MainPage/MainPage.tsx`
- ✅ Inline 6-char room-code quick-join on MainPage
- ✅ `/games` hub retired, redirects to `/` — `frontend/src/routes/games/index.tsx`
- ✅ Reusable `ActionCard` for entry-point tiles — `frontend/src/components/Common/ActionCard/ActionCard.tsx`
- ✅ Dedicated `JoinGamePage` for code entry — `frontend/src/pages/GamePage/JoinGamePage.tsx`

## 2. Create flow

- ✅ Three-mode picker: **Template / Custom / Auto-Generate** — `frontend/src/pages/GamePage/CreateGamePage.tsx`
- ✅ Template mode: lists system decks, one-click → lobby with defaults
- ✅ Custom mode: lists user's decks + settings form
- ☐ Auto-Generate mode: currently a "Coming Soon" stub — needs UI + backend (§7)
- ☐ Template categorization (movies / science / general knowledge…) — currently a flat grid
- ☐ Template browse with preview (question count, sample question)

## 3. Deck elements

A Deck contains an ordered list of `DeckElement` records — a sealed family. Each kind has its own scoring + redaction logic; see §3a for runtime/authoring coverage.

| Element | Status | Notes |
|---------|--------|-------|
| Slide | ✅ runtime | non-interactive content (title / section divider / callout / content / end). `slideKind` enum + `body` / `bodyMarkdown` fields modeled. Authoring UI in progress. Seed slides included in both system decks so the flow is demo-able end-to-end. |
| Question kinds (9) | see §3a | every kind has backend model + scoring + redaction; renderers + authoring forms catch up per-row |
| Section | ☐ | tree-organization in the editor (groups questions into chapters). No mid-run rendering unless an explicit transition slide is authored |

### 3a. Question types

Every type now has a backend record + `ElementScorer`/`ElementRedactor` dispatch. Runtime renderer + authoring UI catch up per-row.

| Element record | Backend model | Server scoring | Player renderer | Authoring UI | Notes |
|----------------|---------------|----------------|-----------------|--------------|-------|
| `McqQuestion` | ✅ | ✅ | ✅ `AnswerOptions` | 🚧 stub | original surface |
| `TextQuestion` | ✅ | ✅ (case-insensitive trim + `acceptedVariants`) | ✅ `TextAnswerInput` | 🚧 stub | |
| `NumberQuestion` | ✅ | ✅ (tolerance) | ✅ `NumberAnswerInput` | 🚧 stub | results render as a frequency list; histogram is a future polish |
| `ImageChoiceQuestion` | ✅ | ✅ | ✅ `AnswerOptions` | ☐ | image upload per option still pending |
| `RankingQuestion` | ✅ (`PARTIAL` / `EXACT`) | ✅ | ☐ `PlaceholderAnswer` | ☐ | covers both "re-order chronologically" and "rank low→high" |
| `ScalesQuestion` | ✅ | ✅ (only when `correctRatings` set) | ☐ `PlaceholderAnswer` | ☐ | Pulse-friendly when no correct ratings; Game mode needs them |
| `QAndAQuestion` | ✅ | n/a (not scored) | ☐ `PlaceholderAnswer` | ☐ | audience submits → host moderates pinned/dismissed; needs the §3a-bis machinery |
| `GridQuestion` | ✅ | ✅ (set comparison) | ☐ `PlaceholderAnswer` | ☐ | choose-the-right-cells; can overlay an image |
| `PlaceOnImageQuestion` | ✅ | ✅ (linear distance vs tolerance) | ☐ `PlaceholderAnswer` | ☐ | normalized 0–1 coordinates. Future: a separate `PlaceOnMapQuestion` variant for lat/lng |

`PlaceholderAnswer` lets a player Skip to register a `TimeoutAnswer` so the round can complete cleanly while the per-kind renderer is still on the roadmap.

### 3a-bis. "Best Answer" mode (modifier on any free-form question)

A two-phase round modifier that adds social voting on top of any free-form question type — analogous to Dixit / Secret Hitler:

1. **Submission phase** — every player submits an answer normally (text, drawing, pin, etc.).
2. **Vote phase** — all submissions are shown anonymously; each player votes on which one they think is best.
3. **Reveal** — submissions are de-anonymized; the player whose submission won the vote receives bonus points (`bestAnswerBonus`).

Status:
- ✅ Modifier flag `bestAnswerMode: boolean` + `bestAnswerBonus: int` modeled on `McqQuestion`, `TextQuestion`, `QAndAQuestion`, `PlaceOnImageQuestion`
- ✅ `AudienceSubmission` + `BestAnswerVote` collections + repositories shipped
- ✅ `ShowcasePhase` enum (`SUBMIT | VOTE | REVEAL`) modeled on the showcase
- ☐ Phase machine runtime — `ShowcaseService` currently pins phase to `SUBMIT`; advancing to `VOTE` / `REVEAL` and broadcasting the anonymized submission set + tallying votes is still TODO
- ☐ Frontend voting UI

### 3b. Question media + background

Backgrounds cascade: **element override → deck default → host's active theme → Lorem Picsum placeholder**.

- ☐ Image attachment per question — `imageUrl` exists; needs upload UI + `mediaPosition` (TOP / BOTTOM / BACKGROUND)
- ☐ YouTube video embed — new `videoUrl` field; render inline during the question
- ☐ Audio clip — new `audioUrl` field; play during the question
- ☐ Per-element `backgroundImageUrl` override
- ☐ Per-deck `backgroundImageUrl` default
- ☐ Theme cascade — `Showcase` picks up the host's active theme (`User.activeThemeId`) and broadcasts it so all clients render with the same colors / background
- ☐ Lorem Picsum placeholder when nothing else is set
- ☐ `mediaCaption / altText` for accessibility

## 4. Per-showcase settings

Backend: `backend/.../model/ShowcaseSettings.java`. Frontend exposure: `CreateGamePage.tsx`'s "More options" disclosure.

All settings live in `ShowcaseSettings.java` and are surfaced in the Custom-mode disclosure.

- ✅ Total rounds — `totalRounds`
- ✅ Time per question — `timePerQuestion` (0 = unlimited; replaces the old `noTimer` flag, which has been removed)
- ✅ Speed bonus — `speedBonus` (disabled in the UI when `timePerQuestion === 0`)
- ✅ Per-element point value — `pointValue` (on every scored element kind)
- ✅ Per-element time limit — `displaySeconds` (always overrides the showcase `timePerQuestion`)
- ✅ Game mode (simultaneous / turn-based) — `gameMode`
- ✅ Allow guests — `allowGuests`
- ✅ Max players — `maxPlayers`
- ✅ Allow late join — `allowLateJoin`
- ✅ Hide scores during play — `showScoresImmediately`
- ✅ Scoring preset — `scoringEnabled` (plumbing only — Game preset sets true; Pulse will set false)
- ✅ Shuffle MCQ answer order — frontend-only concern now; the deck editor owns a shuffle / reorder button (the server-side `shuffleMcqOptions` setting was dropped during the polymorphic rework to keep showcase state cleaner)
- ☐ Reveal correct answer privately as soon as a player submits
- ☐ Bonus points for correct-guess in Dixit variant

## 5. In-game dashboard

Role-aware control panel + live player list.

**Control panel**

- ✅ **Player view**: leave (`sendLeave`), self-boot detection navigates them home
- ☐ **Player view**: mute sound (sound system itself not yet implemented)
- ✅ **Host view**: next round (turn-based, existing), end showcase early (`sendEndShowcase`), boot a player (`sendBoot`)

**Player list (live)**

- ✅ Lobby player list updates over WebSocket — `frontend/src/components/Games/Lobby/Lobby.tsx`
- ✅ ScoreBoard during play — `frontend/src/components/Games/ScoreBoard/ScoreBoard.tsx`
- ✅ Per-player "answered ✓ / still thinking" indicator — `/topic/showcase/{code}/answered` broadcast piped into `game.answeredThisRound`
- ✅ Host action to boot a player — Lobby + ScoreBoard buttons; server enforces host-only
- ✅ Disconnected indicator — `PresenceService` tracks STOMP sessions; `/topic/presence` broadcasts on transitions; ScoreBoard + Lobby dim disconnected players with an "offline" badge. Initial snapshot on (re)connect is a known v1 limitation — a user offline before you joined will appear online until they reconnect.
- ☐ Idle indicator (still connected but inactive) — separate from disconnected
- ☐ Host action to mute / silence a player

**Question timer**

- ✅ Countdown UI for timed rounds — `frontend/src/components/Games/QuestionCard/QuestionCard.tsx`
- ✅ "Unlimited" label when no-timer is enabled

**Reconnect**

- ☐ Clean rehydration of showcase state after a STOMP reconnect

## 6. Round-end & post-showcase data view

- ✅ Round result overlay reveals correct answer + per-player outcome — `frontend/src/components/Games/RoundResult/RoundResult.tsx`
- ✅ Game-over screen shows final placements — `frontend/src/components/Games/GameOver/GameOver.tsx`
- ✅ **Post-showcase review mode** — `GET /api/showcases/{roomCode}/review` returns per-round aggregates; `ReviewPanel.tsx` paginates through each round
  - ✅ bar chart for MCQ option counts — `components/Common/Charts/BarChart/`
  - ✅ frequency list for TEXT_INPUT submissions (placeholder for future word cloud) — `components/Common/Charts/FrequencyList/`
  - ☐ histogram for guess-the-number (when that type ships)
  - ☐ ranked list for re-order / ranking / scales (per-position averages)
  - ☐ dixit-style "who guessed what" matrix where applicable
- ✅ Per-round answer distribution surfaces in the post-game review (live round-result overlay distribution is still a separate ☐)
- ✅ Scores-on / scores-off toggle on review mode (driven by `scoringEnabled`)
- ☐ Export results (CSV / JSON) for the host
- ☐ Live in-round distribution on the RoundResult overlay (separate from post-game review)

## 7. Content authoring (My Decks)

> The pack editor is being **rebuilt** against the polymorphic `DeckElement` model. `PackEditorPage.tsx` is currently a placeholder stub; the new authoring surface (`CreateDashboard` under `/decks/$deckId/view`) is in flight. Backend element CRUD (`DeckService.addElement / updateElement / deleteElement / moveElement`) is already in place and operates on `Deck.elements` directly.

- ✅ Create / edit / delete user-owned decks — `frontend/src/pages/MyPacksPage/`
- ✅ List view shows owned + system decks — `MyPacksPage.tsx`
- ✅ Immediate refresh after creating a deck (RTK `refetchOnMountOrArgChange`)
- ✅ Backend element CRUD endpoints (add / update / delete / move within `Deck.elements`)
- 🚧 Editor dashboard — `CreateDashboard` scaffold landed under `/decks/$deckId/view`; per-kind forms still being authored
- ☐ **Element ordering** — drag-to-reorder using `DeckService.moveElement`
- ☐ **Slide authoring** — slide form (title + body + media + optional `hostNotes`) and `slideKind` picker
- ☐ **Section element** — a non-rendering organizational marker in the editor tree; groups elements for the author's clarity. No mid-run rendering unless followed by an explicit transition slide.
- ☐ **Element validation / draft state** — incomplete elements can't be included in a playable showcase; "ready" indicator in the editor
- ☐ **Type selector for the 9 element kinds** — each opens its own field set
- ☐ Image upload per question (for IMAGE_CHOICE + as decoration)
- ☐ YouTube URL + audio-clip attachment per question
- ☐ Per-deck `coverImageUrl` and `backgroundImageUrl`
- ☐ Per-deck `themeId` picker + `defaultSettings` editor
- ☐ Deck tags + multi-tag categorization
- ☐ Deck visibility selector (PRIVATE / UNLISTED / ORG / PUBLIC)
- ☐ "Fork this template" workflow (clones a deck for editing; sets `parentDeckId`)
- ☐ Bulk import (CSV / JSON paste)
- ☐ Deck sharing (org-scoped or invite-link)
- ☐ **Presentation-style builder** (future) — full PPT-like layout authoring, multi-element slides, transitions

## 8. Auto-generate (AI)

All ☐. Spec: user provides a topic / theme, or uploads a PDF / webpage / document; system generates questions.

- ☐ Frontend: form in Auto-Generate mode (topic input + file upload)
- ☐ Backend endpoint: `POST /api/decks/auto-generate` returns a draft deck
- ☐ LLM integration (Claude API — see `claude-api` skill for prompt-caching defaults)
- ☐ Document ingestion (PDF / webpage / docx → text extraction)
- ☐ Review-and-edit step before generated deck is saved

## 9. Templates

- 🚧 System decks are seeded (`backend/src/main/resources/seed/decks.json` + `questions.json`) and used by Template mode
- ☐ Template metadata beyond deck: theme (color/imagery), suggested settings, "play time" hint
- ☐ Template categories (general knowledge / movies / science / pop culture…)
- ☐ Template marketplace / community-submitted templates

## 10. Pulse (audience polling preset)

Spec: same authoring + runtime as a Game, but scoring off and focus on data tracking.

- 🚧 Stub page at `/pulse/create` — `frontend/src/pages/PulsePage/PulseCreatePage.tsx`
- 🚧 ActionCard for Pulse on MainPage (marked "Soon", disabled for non-registered)
- ✅ Backend plumbing for the `scoringEnabled` preset (default true; Pulse will create with false)
- ☐ Pulse creation form — defaults to `scoringEnabled: false`, hides game-mode toggle
- ☐ Live response aggregation (counts per option, word cloud for text-in, histogram for guess-the-number)
- ☐ Pulse results view (no leaderboard; shares the §6 review mode without scores)
- ☐ Anonymous responder mode (no account required, just a join code)

## 11. Cross-cutting

- ✅ All new components registered in `frontend/src/pages/DesignSystemPage/DesignSystemPage.tsx`
- ✅ Design tokens enforced (no hardcoded colors / spacing)
- ✅ Backend tests cover polymorphic element flow — `ShowcaseServiceTest` (16) exercises MCQ + TEXT + NUMBER scoring, redaction, per-element `displaySeconds` precedence, and timeout handling; 49 backend tests pass
- ☐ Tests for the new editor (waiting on the rebuilt editor surface)
- ☐ E2E happy-path: create deck → start showcase → answer round → see result → finish

---

## Design principles (still hold)

- Use existing components first; only build new ones when reuse would distort.
- Every new component must render in the design-system page.
- Quick-start beats configurability — defaults must be sensible enough that the user can ship a showcase in one click. Customization is discoverable, not mandatory.
- Per-element settings live on the `DeckElement` record. Per-showcase settings live on `Showcase.settings`.
- Games and Polls share authoring, gameplay, and the review surface. Only scoring + leaderboard differ, and that difference is a single `scoringEnabled` flag on the showcase.

### Specific architectural conventions

- **Background image cascade**: element → deck → host's theme → Lorem Picsum placeholder. Every renderer respects this order so authors can override progressively without losing the fallback.
- **Scoring opt-out is per-element**: a Game-preset showcase can still contain non-scored elements (Q&A, icebreakers). `DeckElement.scoringEnabledOverride` (when added) always wins over `Showcase.settings.scoringEnabled`.
- **Host-only payloads**: speaker notes (`hostNotes`) and the correct-answer preview are sent only to the host's principal queue, never on the public `/topic/showcase/{code}/round` broadcast. Mirror the per-user-error queue pattern.
- **Coordinate spaces are normalized**: pin-on-image uses 0–1 normalized coordinates so the question works at any rendered scale. Pin-on-map uses lat/lng + km tolerance.
- **Polymorphic answer payloads**: every submission is a sealed `AnswerPayload`. New element kinds add a new payload record alongside their scorer/redactor case rather than overloading existing ones.
- **Slides participate in deck order but not in scoring or round-result aggregation**: the server already enforces this; new types should follow the same "element is in the timeline; not all elements are scored" pattern.
