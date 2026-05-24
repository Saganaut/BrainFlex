Settings, referred to by Deck
@backend/src/main/java/cephadex/brainflex/model/session/InteractiveSessionSettings.java

Rightsidebarcontent
@frontend/src/components/DeckEditor/RightSidebar/RightSidebarContent.tsx

@frontend/src/components/DeckEditor/RightSidebar/ParticipantsPanel.tsx

- Add the following: reactionsEnabled, allowLateJoin, allowReJoin, allowGuests, anonymousMode, maxPlayers, chatEnabled

@frontend/src/components/DeckEditor/RightSidebar/EditSlidePanel.tsx
@frontend/src/components/DeckEditor/RightSidebar/ThemePanel.tsx
Move the content image to the edit slide panel

@frontend/src/components/DeckEditor/RightSidebar/EditSlidePanel.tsx
Add the following:

- autoAdvance
- showResponses
- showScoresIMmediately
- speedBonus
- time per question

Make a list of the defaultSettings that are not yet handled anywhere, out of those which ones are repetitive

---

## Status — done

Backend: `UpdateDeckRequest` now carries `defaultSettings` (an `InteractiveSessionSettings`);
`DeckService.updateDeck` replaces `deck.content.settings` wholesale when non-null. The RTK Query
client was regenerated, so `updateDeck` can write deck-author defaults.

Frontend (all write `deck.defaultSettings` through the new `RightSidebar/useDeckSettings` hook —
read-modify-write of the whole settings object, since `defaultSettings` replaces wholesale):

- **ParticipantsPanel** — `maxPlayers`, `allowGuests`, `allowLateJoin`, `allowReJoin`,
  `anonymousMode`, `chatEnabled`, deck-wide `reactionsEnabled`. Kept the existing per-slide
  reactions override (`chrome.reactionsEnabled`).
- **EditSlidePanel** — new `SessionPacingSection` (`timePerQuestion`, `autoAdvance`, `speedBonus`,
  `showScoresImmediately`, `showResponses`) + the per-slide **content image** moved here from
  ThemePanel (`SlideImageSection`). The shared tile chrome was extracted to `RightSidebar/ImagePicker`.
- **ThemePanel** — keeps the deck background image (a styling concern); content image removed.

## `defaultSettings` audit

`InteractiveSessionSettings` has 28 fields. "Handled" = an editable control exists. There are two
distinct surfaces: the **deck editor** right panel (author *defaults*, this work) and
**CreateGamePage** / `useGameSettings` (host *session-launch* values, seeded from `deck.defaultSettings`).

### Now editable in the deck-editor right panel (this work)

`maxPlayers`, `allowGuests`, `allowLateJoin`, `allowReJoin`, `anonymousMode`, `chatEnabled`,
`reactionsEnabled`, `timePerQuestion`, `autoAdvance`, `speedBonus`, `showScoresImmediately`,
`showResponses`.

### Editable only at session-launch (CreateGamePage), still not in the deck editor

`totalRounds`, `answerSubmissionMode`, `teamMode`, `teamCount`, `autoBalanceTeams`,
`shuffleQuestions`, `shuffleAnswers`, `podiumDuration`, `lobbyCountdownSeconds`,
`requireFullName`, `spectatorsAllowed`.

### Not handled anywhere (no editable control on either surface)

- `scoringEnabled` — only *read* (SessionSummary / ReviewPanel); flipped implicitly by the "Pulse"
  preset / `SessionFormat`, never via a control.
- `lobbyMusicAssetId` — referenced only in mock data.
- `deckCoverImageUrl`, `deckBackgroundImageUrl`, `themeId` — denormalized snapshots copied from the
  deck at session-create; not standalone knobs.

### Repetitive / redundant

- **`showResponses` (×3).** `defaultSettings.showResponses` duplicates `content.showResponses`
  (`defaultShowResponses`), and the resolver cascade only reads `defaultShowResponses` — so the
  settings copy is effectively dead at the deck level. A third copy lives per-element (BehaviorSection).
- **`deckCoverImageUrl` / `deckBackgroundImageUrl` / `themeId`.** Pure duplicates of
  `content.cover` / `content.background` / `content.themeId` (the real, editable fields). The
  settings copies are frozen snapshots, redundant as *deck defaults*.
- **`timePerQuestion` ↔ per-element `displaySeconds`.** Same "how long is a question" in two places
  (deck-wide fallback vs per-element override) — intentional, but duplicative.
- **`autoAdvance` ↔ per-element `autoAdvance` / `autoAdvanceSeconds` + `podiumDuration`.** Overlapping
  pacing knobs across deck-wide and per-slide scopes.
- **`reactionsEnabled` (settings) ↔ `chrome.reactionsEnabled` (per-slide).** Master switch + per-slide
  override — same concept, two layers.
- **`scoringEnabled` ↔ per-element `chrome.scored` + `SessionFormat` + `speedBonus`.** Several
  overlapping ways to express "is this scored".
- **`totalRounds`.** Redundant with the deck's actual count of question elements.
- **`requireFullName` ↔ `allowGuests` / `anonymousMode`.** Overlapping (and partly contradictory)
  identity policy.
- **`spectatorsAllowed` ↔ `allowGuests`.** Overlapping audience-access semantics.

**Recommendation:** drop the snapshot duplicates (`deckCoverImageUrl`/`deckBackgroundImageUrl`/
`themeId`) and the dead `defaultSettings.showResponses` from the deck-author defaults, and decide a
single owner for scoring (`SessionFormat` + per-element `scored`) and for question timing
(per-element `displaySeconds` with `timePerQuestion` as the only fallback).
