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
| Question | ✅ (see §3a) | the only element type currently authored |
| Slide | ☐ | non-interactive content (title slide, section divider, callout). Authors add explicitly; no auto-transitions |
| Section | ☐ | tree-organization in the editor (groups questions into chapters). No mid-run rendering unless an explicit transition slide is authored |

### 3a. Question types

| Type | Backend model | Pack editor | Showcase gameplay | Notes |
|------|---------------|-------------|-------------------|-------|
| Multiple choice | ✅ `Question.options` + `correctAnswer` | ✅ `PackEditorPage.tsx` | ✅ `components/Games/AnswerOptions/` | original surface |
| Type in (text or number) | ✅ `Question.correctAnswerText` | ✅ `PackEditorPage.tsx` (type selector) | ✅ `components/Games/TextAnswerInput/` | case-insensitive trim match |
| Image choice | 🚧 enum exists (`QuestionType.IMAGE_CHOICE`) | ☐ | ☐ | model has `imageUrl`; need editor upload + render |
| Re-order (oldest→newest, etc.) | ☐ | ☐ | ☐ | drag-to-reorder UI; scoring per-position |
| Scales (Likert rating per statement) | ☐ | ☐ | ☐ | rate statements 1–5; for Pulse, distribution; for Game, "guess the mean" |
| Ranking (sort items, lowest→highest) | ☐ | ☐ | ☐ | analogous to re-order but ranked, not chronological |
| Q&A (Slido-style) | ☐ | ☐ | ☐ | audience submits questions; host promotes / dismisses |
| Guess the number | ☐ | ☐ | ☐ | numeric input; results rendered as a histogram |
| Grid (select cells) | ☐ | ☐ | ☐ | scope to be defined |
| Pin on image | ☐ | ☐ | ☐ | place a marker on an uploaded image |
| Pin on map | ☐ | ☐ | ☐ | place a marker on a geographic map |
| "Dixit" anonymous-guess variant | ☐ | ☐ | ☐ | players submit, then vote on which submission is correct |

### 3b. Question media + background

All ☐. Apply to any question type.

- ☐ Image attachment (existing `imageUrl` on `Question`; needs upload UI in editor)
- ☐ YouTube video embed (paste a URL; render inline during the question)
- ☐ Audio clip (small upload or remote URL; play during the question)
- ☐ Per-question background image — falls back to the active Theme's background, with **Lorem Picsum** as placeholder when no theme is set

## 4. Per-showcase settings

Backend: `backend/.../model/ShowcaseSettings.java`. Frontend exposure: `CreateGamePage.tsx`'s "More options" disclosure.

All settings live in `ShowcaseSettings.java` and are surfaced in the Custom-mode disclosure.

- ✅ Total rounds — `totalRounds`
- ✅ Time per question — `timePerQuestion`
- ✅ Speed bonus — `speedBonus`
- ✅ Per-question point value — `Question.pointValue` (pack editor)
- ✅ Per-question time limit — `Question.timeLimit` (pack editor)
- ✅ Game mode (simultaneous / turn-based) — `gameMode`
- ✅ Allow guests — `allowGuests`
- ✅ Max players — `maxPlayers`
- ✅ No timer — `noTimer` (round ends when all answered / host advances; QuestionCard renders "Unlimited")
- ✅ Allow late join — `allowLateJoin`
- ✅ Hide scores during play — `showScoresImmediately`
- ✅ Scoring preset — `scoringEnabled` (plumbing only — Game preset sets true; Pulse will set false)
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
- ☐ Add non-question elements (slides + section dividers)
- ☐ Reorder elements within a deck (drag-to-reorder)
- ☐ Image upload per question (for IMAGE_CHOICE + as decoration)
- ☐ YouTube URL + audio-clip attachment per question
- ☐ Per-deck background image (theme image or Lorem Picsum placeholder)
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
