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

Captures the field gaps surfaced in the audit. These are the minimum schema additions needed before the listed checklist items below can ship cleanly.

### Deck — fields to add

- ✅ `position`-supporting ordering for elements (now on `Question`; runtime sorts by position)
- ✅ `coverImageUrl` — thumbnail shown on template tiles and the My Decks list (Lorem Picsum placeholder when null)
- ✅ `backgroundImageUrl` — deck-level background; cascade implemented at deck → Lorem Picsum, theme tier still pending
- ☐ `themeId` — link to the host's `Theme` so deck inherits color scheme during play
- ☐ `tags: List<String>` — multi-tag categorization (movies / science / icebreaker / etc.); replaces the single `category` long term
- ☐ `recommendedPreset: GAME | PULSE | PRESENTATION` — advisory; sets defaults in the create flow
- ☐ `defaultSettings: ShowcaseSettings` — author-suggested showcase settings auto-applied at create time
- ☐ `organizationId` — org-scoped sharing (parallels `User.organizationId`)
- ☐ `estimatedDurationMinutes` — "~10 min" hint for template browsing
- ☐ `parentDeckId` + `version` — for fork-this-template + history
- ☐ `updatedAt`
- ☐ `visibility: PRIVATE | UNLISTED | ORG | PUBLIC` — replaces boolean `isPublic`

### Question / Slide — fields to add

- ✅ `position: double` — explicit element order; runtime sorts by position; new inserts auto-assign max+10; legacy docs backfilled on startup
- ☐ `videoUrl: String` — YouTube embed (and later, direct video hosts)
- ☐ `audioUrl: String` — audio clip URL
- ☐ `backgroundImageUrl: String` — per-element background override
- ☐ `mediaPosition: TOP | BOTTOM | BACKGROUND` — author chooses where media renders
- ☐ `mediaCaption / altText: String` — accessibility + caption support
- ☐ `slideKind: TITLE | SECTION | CALLOUT | CONTENT | END` — layout variant for slides
- ☐ `bodyMarkdown: String` — richer slide formatting
- ☐ `hostNotes: String` — speaker notes; sent only to the host
- ☐ `explanation: String` — post-answer "Here's why" copy
- ☐ `scoringEnabledOverride: Boolean` — per-element opt-out from scoring (icebreakers, Pulse-style elements in a Game showcase)
- ☐ `bestAnswerMode: Boolean` + `bestAnswerBonus: int` — Best Answer two-phase round modifier (see §3a-bis)
- ☐ `updatedAt`
- ☐ **Type-specific fields** (see §3a) — `orderedItems`, `scaleStatements`/`scaleMin`/`scaleMax`/`correctRatings`, `numericAnswer`/`numericTolerance`/`numericUnit`, `gridRows`/`gridCols`/`correctCells`, `pinTargetImageUrl`/`correctX`/`correctY`/`pinTolerance`, `pinTargetLat`/`pinTargetLng`/`mapZoomDefault`. Wide-table approach: nullable fields scoped by `type`.

### Existing enums to extend

- ✅ `QuestionType` already extended with `SCALES`, `RANKING`, `Q_AND_A`, `NUMBER_INPUT`, `GRID`, `PLACE_ON_IMAGE` — none implemented yet, but the values are reserved so backend renderers can be wired in without another enum migration.
- ☐ Consider a `SlideKind` enum for the new slide layout variants.

### New collections needed

- ☐ `AudienceSubmission` — for Q&A (Slido-style) and Best Answer mode. `{ id, showcaseId, questionId, userId, text, status (PENDING/PINNED/DISMISSED), upvotes, submittedAt }`. Lives in its own collection because submissions span all participants and the host moderates them live.
- ☐ `BestAnswerVote` (or embed in `AudienceSubmission.votes`) — for the second phase of Best Answer rounds.
- ☐ Polymorphic `responsePayload` on `PlayerAnswer` for pin coordinates / numeric guesses / grid selections / ordered submissions. Today `selectedOption` + `textAnswer` covers MCQ + TEXT_INPUT; future types need a structured payload.

### Cross-cutting

- ☐ **Background image cascade** documented and implemented: element → deck → theme → Lorem Picsum placeholder
- ☐ **Theme integration into Showcases** — the host's active theme drives colors / fonts for every participant during a showcase
- ☐ **Element pool sampling** when `totalRounds < deck.size` — currently first-N; future: always include title slide, optionally stratify by difficulty
- ☐ **Real-time host preview** — host's QuestionCard shows the correct answer during play (it doesn't today)
- ☐ **Element validation / "ready" flag** — draft elements with missing required fields can't be included in a playable showcase
- ☐ **Question banks / cross-deck reuse** — `Question.deckId` is single-valued today; long term we may want a question pool
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

A Deck contains an ordered list of elements. Today only **Question** is implemented.

| Element | Status | Notes |
|---------|--------|-------|
| Question | ✅ (see §3a) | scoring + per-player submission cycle |
| Slide | ✅ runtime | non-interactive content (title / section divider / callout). Backend model + gameplay rendering shipped. Authoring UI is being built by you separately. Seed slides included in both system decks so the flow is demo-able end-to-end. |
| Section | ☐ | tree-organization in the editor (groups questions into chapters). No mid-run rendering unless an explicit transition slide is authored |

### 3a. Question types

Each new type needs the listed Question fields plus a renderer/editor on the frontend. Enum values live in `QuestionType.java`.

| Type | Enum | Status | Required Question fields | Notes |
|------|------|--------|--------------------------|-------|
| Multiple choice | `MULTIPLE_CHOICE` | ✅ | `options`, `correctAnswer` | original surface |
| Type in (free text) | `TEXT_INPUT` | ✅ | `correctAnswerText` | case-insensitive trim match |
| Number input | `NUMBER_INPUT` | ☐ | `numericAnswer: Double`, `numericTolerance: Double`, `numericUnit: String` | numeric variant of TEXT_INPUT; tolerance-based scoring; results render as a histogram |
| Image choice | `IMAGE_CHOICE` | 🚧 enum exists | `options` (text or image refs), `correctAnswer`, `imageUrl` per option (future) | choose-from-images variant of MCQ |
| Ranking | `RANKING` | ☐ | `orderedItems: List<String>` (correct order) | covers both "re-order chronologically" and "rank low→high" — same payload, differs only in framing |
| Scales (Likert) | `SCALES` | ☐ | `scaleStatements: List<String>`, `scaleMin: int`, `scaleMax: int`, optional `correctRatings: List<Integer>` | Pulse: shows distribution. Game variant: "guess the average rating". Not suitable for vanilla game mode without a correct answer. |
| Q&A (Slido-style) | `Q_AND_A` | ☐ | (no answer fields) + new `AudienceSubmission` collection | audience submits questions; host moderates pinned/dismissed; upvote support optional. Not scored. |
| Grid | `GRID` | ☐ | `gridRows: int`, `gridCols: int`, `correctCells: List<Integer>`, optional `gridLabels: List<String>` or `gridImageUrl` | choose-the-right-cells; can overlay an image |
| Place on image / map | `PLACE_ON_IMAGE` | ☐ | `pinTargetImageUrl: String`, `correctX: double` (0–1), `correctY: double` (0–1), `pinTolerance: double` (0–1) | normalized coordinates so the question works at any image scale. Future: a separate `PLACE_ON_MAP` enum for lat/lng-driven variants. |

### 3a-bis. "Best Answer" mode (modifier on any free-form question)

A two-phase round modifier that adds social voting on top of any free-form question type — analogous to Dixit / Secret Hitler:

1. **Submission phase** — every player submits an answer normally (text, drawing, pin, etc.).
2. **Vote phase** — all submissions are shown anonymously; each player votes on which one they think is best.
3. **Reveal** — submissions are de-anonymized; the player whose submission won the vote receives bonus points (`bestAnswerBonus`).

Implementation:
- Modifier flag `bestAnswerMode: boolean` on a Question; works with `TEXT_INPUT`, `Q_AND_A`, `PLACE_ON_IMAGE`, and future types that accept open-ended input.
- `bestAnswerBonus: int` (per-question) for the points awarded to the top-voted submission. Tie-break: shared bonus.
- Needs new collections: `AudienceSubmission { id, showcaseId, questionId, userId, text/payload, status }` + `BestAnswerVote { submissionId, voterUserId, votedAt }`.
- WebSocket round protocol gains two new phases: `SUBMIT → VOTE → REVEAL`. Round result still feeds the post-showcase review surface.

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
- ✅ Per-question point value — `Question.pointValue` (pack editor)
- ✅ Per-question time limit — `Question.timeLimit` (pack editor)
- ✅ Game mode (simultaneous / turn-based) — `gameMode`
- ✅ Allow guests — `allowGuests`
- ✅ Max players — `maxPlayers`
- ✅ Allow late join — `allowLateJoin`
- ✅ Hide scores during play — `showScoresImmediately`
- ✅ Scoring preset — `scoringEnabled` (plumbing only — Game preset sets true; Pulse will set false)
- ✅ Shuffle MCQ answer order — `shuffleMcqOptions` (server picks a stable per-question shuffle at first broadcast; scoring + review use the same order)
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

- ✅ Create / edit / delete user-owned decks — `frontend/src/pages/MyPacksPage/`
- ✅ Add / edit / delete questions — `frontend/src/pages/MyPacksPage/PackEditorPage.tsx`
- ✅ Question-type selector + branching form (MCQ / Type-in) — `PackEditorPage.tsx`
- ✅ List view shows owned + system decks — `MyPacksPage.tsx`
- ✅ Immediate refresh after creating a deck (RTK `refetchOnMountOrArgChange`)
- ☐ **Element ordering** — drag-to-reorder; requires `Question.position` field (see §0)
- ☐ **Slide authoring** — slide form (title + body + media + optional `hostNotes`) and slide-kind picker
- ☐ **Section element** — a non-rendering organizational marker in the editor tree; groups elements for the author's clarity. No mid-run rendering unless followed by an explicit transition slide.
- ☐ **Element validation / draft state** — incomplete elements can't be included in a playable showcase; "ready" indicator in the editor
- ☐ **Type selector for the 8 new question types** (re-order / ranking / scales / Q&A / guess-number / grid / pin-image / pin-map) — each opens its own field set
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
- ☐ Tests for the showcase state machine (`ShowcaseServiceTest` covers core flow; TEXT_INPUT scoring path untested)
- ☐ Tests for the type-in pack-editor branch (`PackEditorPage.test.tsx` does not exist)
- ☐ E2E happy-path: create deck → start showcase → answer round → see result → finish

---

## Design principles (still hold)

- Use existing components first; only build new ones when reuse would distort.
- Every new component must render in the design-system page.
- Quick-start beats configurability — defaults must be sensible enough that the user can ship a showcase in one click. Customization is discoverable, not mandatory.
- Per-question settings live on the `Question` document. Per-showcase settings live on `Showcase.settings`.
- Games and Polls share authoring, gameplay, and the review surface. Only scoring + leaderboard differ, and that difference is a single `scoringEnabled` flag on the showcase.

### Specific architectural conventions

- **Background image cascade**: element → deck → host's theme → Lorem Picsum placeholder. Every renderer respects this order so authors can override progressively without losing the fallback.
- **Scoring opt-out is per-element**: a Game-preset showcase can still contain non-scored elements (Q&A, icebreakers). `Question.scoringEnabledOverride` always wins over `Showcase.settings.scoringEnabled`.
- **Host-only payloads**: speaker notes (`hostNotes`) and the correct-answer preview are sent only to the host's principal queue, never on the public `/topic/showcase/{code}/round` broadcast. Mirror the per-user-error queue pattern.
- **Coordinate spaces are normalized**: pin-on-image uses 0–1 normalized coordinates so the question works at any rendered scale. Pin-on-map uses lat/lng + km tolerance.
- **Polymorphic answer payloads**: as new question types ship, prefer adding a structured `responsePayload` on `PlayerAnswer` over piling type-specific fields onto the base document. Keep `selectedOption` / `textAnswer` as the MCQ + TEXT_INPUT shorthand they already are.
- **Slides participate in deck order but not in scoring or round-result aggregation**: the server already enforces this; new types should follow the same "element is in the timeline; not all elements are scored" pattern.
