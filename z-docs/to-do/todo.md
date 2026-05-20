# BrainFlex TODO

> **Where things live:** the structured roadmap is in [`README.md`](README.md) (chunks 01–23, each with its own folder). This file is the lightweight catch-all for component-level checkmarks and small finish-work that doesn't justify a full chunk.

## Frontend

### Design / Common Components

- [x] Button (`Common/Buttons/Btn.tsx`)
- [x] Card (`Common/Cards/Card.tsx`)
- [x] Modal / Dialog (`Common/Modal/index.tsx`)
- [x] Input / TextField
- [x] Textarea
- [x] Select / Dropdown (`Common/Input/Dropdown/Dropdown.tsx`)
- [x] Checkbox
- [x] Radio
- [x] Toggle / Switch (`Common/Input/Toggle/Toggle.tsx`)
- [x] Toast / Snackbar (`Common/Toast/Toast.tsx`)
- [x] Tooltip (`Common/Tooltip/Tooltip.tsx`)
- [x] Badge / Tag / Chip
- [x] Avatar (`Common/Avatar/Avatar.tsx`)
- [x] Spinner / Loader (`Common/Loader/Loader.tsx`)
- [x] Skeleton (`Common/Skeleton/Skeleton.tsx`)
- [x] Progress Bar (`Common/ProgressBar/`)
- [x] Tabs (`Common/Tabs/`)
- [ ] Pagination → tracked under [chunk 22](./22-common-component-gaps/README.md)
- [x] Empty State (`Common/EmptyState/`)
- [ ] Alert / Banner (inline feedback) → tracked under [chunk 22](./22-common-component-gaps/README.md)
- [x] Divider (`Common/Divider/Divider.tsx`)
- [x] Icon Button (icon-only button variant)

### Deck editor (DeckEditor)

All four items below are now scoped under [chunk 23 — Deck editor shell polish](./23-deck-editor-shell-polish/README.md):

- [ ] Navbar: wire the existing Preview and Start (interactive session) buttons (currently `console.log` stubs in `DeckEditor.tsx`)
- [ ] Left sidebar: render a first-slide skeleton when a new deck has zero elements (clicking it opens `NewElementPicker`)
- [ ] Right sidebar: vertical icon rail with drawer-on-drawer pattern — Edit slide (pencil) / Theme (palette) / Participant settings (users) / Sharing preferences (share)
- [x] Speaker notes drawer below the main slide area (`SpeakerNotesDrawer.tsx` — uses `RichTextInput`, persists via `schedule(patch)`)

### Game / live-show components

Most player and host surfaces already exist or are owned by other chunks:

| Item                              | Status                                                            |
| --------------------------------- | ----------------------------------------------------------------- |
| Live results display              | Covered by [chunk 13](./13-interactive-session-settings-and-player-additions/README.md); `RoundResult.tsx` / `ScoreBoard.tsx` exist for post-round |
| Player status indicators          | Covered by [chunk 13](./13-interactive-session-settings-and-player-additions/README.md) (`disconnected`, `lastSeenAt` fields) |
| Question display                  | `QuestionCard.tsx` + `PlayPage.tsx` dispatch                      |
| Game lobby                        | `Games/Lobby/Lobby.tsx` + `pages/GamePage/LobbyPage.tsx`          |
| Game over screen                  | `Games/GameOver/GameOver.tsx`                                     |
| Chat / communication panel        | Covered by [chunk 11](./11-interactive-session-reactions-and-chat/README.md) |
| Control panel (host)              | Host view items in [chunk 13](./13-interactive-session-settings-and-player-additions/README.md) |
| Connected player info             | Covered by [chunk 13](./13-interactive-session-settings-and-player-additions/README.md) |
| Connection status                 | Partial — `WsErrorBanner.tsx` covers WS errors; per-player status surface lands with [chunk 13](./13-interactive-session-settings-and-player-additions/README.md) |
| Game/quiz title display           | Flows through `QuestionCard.tsx` context                          |
| Timer display                     | `QuestionCard.tsx` / `VotePanel.tsx`                              |
| Score display                     | `GameOver.tsx` / `ScoreBoard.tsx` / `RoundResult.tsx`             |
| Progress indicator (Q n of N)     | `QuestionCard.tsx`                                                |
| Join game screen                  | `pages/GamePage/JoinGamePage.tsx`                                 |
| Per-kind player surfaces          | Deferred to [chunk 13](./13-interactive-session-settings-and-player-additions/README.md) per the README "Deferred work" section — Drawing canvas, Word Cloud / Allocation / Matching player views, per-kind `PlayPage` dispatch |
| Leaderboard (account)             | `components/Leaderboard/Leaderboard.tsx` exists; per-game leaderboard polish lands with [chunk 13](./13-interactive-session-settings-and-player-additions/README.md) |
| Feedback / response indicators    | `AnswerOptions.tsx` already styles correct/wrong; a unified feedback component would land with [chunk 13](./13-interactive-session-settings-and-player-additions/README.md) |

## Backend

### Image upload validation

Substantially done. Verify on the surfaces below; convert any remaining inline error spans to `Alert` when [chunk 22](./22-common-component-gaps/README.md) lands:

- `GalleryPicker.tsx` validates via `validateImageFile()` and surfaces errors.
- `ThemeEditor.tsx` enforces 5 MB / 2 MB caps with inline error display.
- Avatar upload + any newer image surface — confirm client-side size + MIME validation so multipart `MaxUploadSizeExceededException` doesn't silently fail.

The original symptom — backend warning with no user-visible error:
```
WARN .w.s.m.s.DefaultHandlerExceptionResolver : Resolved [org.springframework.web.multipart.MaxUploadSizeExceededException: Maximum upload size exceeded]
```

If you find a site that still slips through, fix it and tick this section.
