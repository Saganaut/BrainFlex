# 12 — Showcase teams

**Status:** Backend + codegen landed (2026-05-19). Lobby team picker grid, in-game team leaderboard, and team podium are deferred to chunk 13, which is the holistic player-UI pass (same precedent as chunk 11).
**Depends on:** Nothing strict; pairs with chunk 13 (settings flag `teamMode`)
**Unblocks:** 13 (`ShowcasePlayer.teamId`)

## Scope

Team mode — Kahoot's signature classroom feature. Players join a team in the lobby; team score is the sum of member scores; leaderboards and reveals show team rankings instead of (or alongside) individual rankings.

Teams are embedded in the Showcase document — they don't outlive the game.

## New embedded model

```text
Team (embedded in Showcase.teams)
  String id                            // server-assigned UUID
  String name                          // host-set or auto-generated (e.g. "Red Lions")
  String color                         // oklch token name (red | orange | yellow | green | teal | blue | violet | pink)
  String captainUserId                 // nullable; defaults to first joiner
  int score                            // sum of member.score; recompute on each PlayerAnswer write
  int memberCount                      // denorm; convenience for lobby UI
```

## Updates to existing models

- **Showcase**
  - `boolean teamMode` — default `false`
  - `List<Team> teams` — empty unless `teamMode`
  - `boolean autoBalanceTeams` — distributes new joiners round-robin
- **ShowcaseSettings**
  - `boolean teamMode` — default `false` (host configures at create time, copied to Showcase)
  - `int teamCount` — default `2` for new shows in team mode; ignored when manual teams are pre-defined
- **ShowcasePlayer** (covered also in chunk 13)
  - `String teamId` — nullable when not in team mode

## Backend changes

- `ShowcaseService.createShowcase` — if `settings.teamMode`, generate `teamCount` default teams with sequential names + color tokens
- `ShowcaseService.joinShowcase` — if `teamMode`:
  - If `autoBalanceTeams=true`, pick the smallest team
  - Else require `teamId` in the join payload; reject if missing
- `ShowcaseService.leaveShowcase` — decrement `team.memberCount`; if captain leaves, reassign or clear
- `ShowcaseService.recordAnswer` — after writing `PlayerAnswer`, recompute `team.score = sum(member.score)`. Use a Mongo aggregation or maintain incrementally.
- New endpoints:
  - `POST /api/showcases/{roomCode}/teams` — host creates a custom team; body `{ name, color }`
  - `PUT  /api/showcases/{roomCode}/teams/{teamId}` — rename/recolor
  - `DELETE /api/showcases/{roomCode}/teams/{teamId}` — host only; players reassigned to other teams round-robin
  - `PUT  /api/showcases/{roomCode}/players/{userId}/team` — host moves a player; body `{ teamId }`
- WebSocket — emit `TeamUpdateMessage` over `/topic/showcase/{roomCode}` whenever team membership or score changes

## Frontend changes

- Lobby:
  - Team picker grid (each team is a card with color, name, current member list)
  - "Auto-assign" button if `autoBalanceTeams`
  - Host can add/rename/delete teams
- Player view during play:
  - Team badge under the player's score
  - Team leaderboard alternates with individual leaderboard between rounds
- Host view:
  - Team leaderboard during reveals
  - Per-team progress indicator during SUBMIT phase
- Results / podium:
  - Top 3 teams on the podium; individual MVP from each team shown underneath

## Cross-cutting concerns

- Don't pre-create a `Team` collection — keeping teams embedded means `ShowcaseResult` doesn't need a separate FK and the team disappears with the game (as it should)
- `PlayerPlacement` (chunk 13 additions) gets `teamId` so the post-game results page can re-render team standings
- Team scoring runs alongside individual scoring; both `team.score` and `player.score` are authoritative

## Checklist

- [x] `Team` embedded record + `Showcase.teams` + `Showcase.teamMode` + `autoBalanceTeams`
- [x] `ShowcaseSettings.teamMode`, `teamCount`, `autoBalanceTeams`
- [x] `ShowcasePlayer.teamId` + `PlayerPlacement.teamId` (for the post-game team standings render in chunk 13)
- [x] `createShowcase` seeds default teams when team mode and auto-assigns the host
- [x] `joinShowcase` auto-balance or require teamId (manual mode); `JoinShowcaseRequest` body
- [x] `submitAnswer` + Best-Answer reveal recompute `team.score`; broadcasts `TeamUpdateMessage`
- [x] Host team CRUD endpoints (`POST/PUT/DELETE /api/showcases/{code}/teams`, `PUT /players/{userId}/team`) + tests
- [x] STOMP `TeamUpdateMessage` broadcasts on `/topic/showcase/{roomCode}/teams`
- [ ] Lobby team picker grid *(deferred to chunk 13)*
- [ ] Team leaderboard between rounds *(deferred to chunk 13)*
- [ ] Team podium on results *(deferred to chunk 13)*
- [x] Frontend codegen + lint
- [x] Backend tests pass (258/258, including 11 new team-mode tests)
