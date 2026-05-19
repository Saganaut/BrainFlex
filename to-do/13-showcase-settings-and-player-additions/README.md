# 13 — Showcase settings & player additions

**Status:** Not started
**Depends on:** 11 (reactions counter), 12 (teamId)
**Unblocks:** 15 (`PlayerAnswer.timeTakenMs` needed for history), 16 (analytics)

## Scope

Catch-all chunk for the field additions that make a live show feel polished: shuffle, auto-advance, podium duration, lobby polish, custom room codes, lobby music, plus the per-player streak/accuracy/timing fields needed for analytics and Kahoot-style streak bonuses.

## Updates to existing models

### Showcase

- `boolean anonymousMode` — hide real names on the leaderboard (use `avatarKey + colorTag` only)
- `String customRoomCode` — host-set; falls back to the auto-generated `roomCode` if blank
- `String hostName, hostAvatarUrl` — denorm so the lobby header doesn't need a user lookup
- `boolean allowReJoin` — disconnected/kicked players can come back
- `int spectatorCount` — viewers without a player slot
- `LocalDateTime lobbyOpenedAt`
- `String exportedReportUrl` — populated when a CSV/PDF report is generated (see chunk 16)

### ShowcaseSettings

- `boolean shuffleQuestions` — default `false`
- `boolean shuffleAnswers` — default `true`; per-element override via the question's own `shuffleOptions`
- `boolean autoAdvance` — host doesn't have to click "Next"; default `false`
- `int podiumDuration` — seconds; default `15`
- `int lobbyCountdownSeconds` — default `5`; "Game starts in N..."
- `String lobbyMusicAssetId` — `MediaAsset` reference (chunk 19); nullable
- `boolean requireFullName` — disallow nicknames
- `boolean spectatorsAllowed` — default `false`
- `boolean reactionsEnabled` (if not added in chunk 11)
- `boolean chatEnabled` (if not added in chunk 11)
- `boolean teamMode` (if not added in chunk 12)
- `int teamCount` (if not added in chunk 12)

### ShowcasePlayer

- `String avatarKey` — Kahoot-style preset avatar id (e.g. `"fox-orange"`); distinct from real `pictureUrl`
- `String colorTag` — assigned in lobby (color token name)
- `String teamId` — coordinate with chunk 12
- `int longestStreak, currentStreak` — track across rounds
- `double accuracy` — `correctAnswers / answeredQuestions`, recomputed on each answer
- `int reactionsSent` — coordinate with chunk 11
- `boolean lateJoin` — joined after the show started
- `boolean disconnected` — currently disconnected
- `LocalDateTime lastSeenAt` — updated by PresenceService heartbeats
- `int speedBonusTotal` — total speed bonus points accumulated

### PlayerAnswer

- `long timeTakenMs` — answer arrival time minus `roundStartedAt`. **Critical** for history + analytics — measure now, even if speed bonus is off.
- `int streakBeforeAnswer` — for "5x streak!" displays at reveal
- `int speedBonusAwarded` — separate from base points
- `boolean usedPowerUp` — placeholder for future power-up chunk
- `String powerUpId` — nullable

### PlayerPlacement

- `String teamId`
- `int longestStreak`
- `double accuracy`
- `int reactionsSent`

## Backend changes

- `ShowcaseService.startRound` — when `settings.shuffleQuestions`, pre-shuffle `Showcase.deckSnapshot` once at game start (not per-round, so the elementId order is stable). When `settings.shuffleAnswers`, pass a per-player deterministic shuffle to `ElementRedactor` (see chunk 10).
- `ShowcaseService.recordAnswer`:
  - Compute `timeTakenMs = answeredAt - roundStartedAt`
  - Compute `speedBonus` from `settings.speedBonus` if true: `bonus = round(pointValue * 0.5 * (1 - timeTakenMs / timePerQuestionMs))`, clamped to ≥ 0
  - Update `currentStreak` (increment on correct, reset to 0 on incorrect); update `longestStreak = max(longestStreak, currentStreak)`
  - Update `accuracy`
- `ShowcaseService.advanceRound`:
  - If `settings.autoAdvance`, schedule a `roundStartedAt + timePerQuestion + podiumDuration` advance via a Spring `@Scheduled` task or per-show `ScheduledFuture` in a session-scoped cache
- `ShowcaseService.createShowcase`:
  - If `customRoomCode` is set and unique, use it; else generate
  - Validate avatar pool (a constant list of preset avatar keys; ship 12–20)
- New service: `AvatarService` — list available preset avatars; each is `{ key, displayName, imageUrl, colorTag }`
- `PresenceService` — update `ShowcasePlayer.lastSeenAt` + `disconnected` on WS disconnect / reconnect (chunk dependency on existing `PresenceEventListener`)

## Frontend changes

- Showcase create form:
  - All new toggles in an "Advanced" section
  - Avatar pool dropdown for the player lobby
  - Lobby music picker (chunk 19 dependency)
- Lobby:
  - Avatar grid players pick from (collapse to "Random" if `requireFullName=true` and host wants Kahoot vibes)
  - Custom room code in big text
- Player view:
  - Streak indicator ("3x streak 🔥")
  - Accuracy in the player tile
- Host view:
  - Auto-advance progress ring per phase
  - Podium duration countdown
- Results:
  - Per-player accuracy + longestStreak + speedBonusTotal on the placement card

## Cross-cutting concerns

- **Backwards compat** — every Showcase field gets a default; old documents read as defaults. Maven `@Default` annotation via Lombok or service-layer fallback.
- **Persisting timing** — `roundStartedAt` already exists on Showcase. Use it as the reference time.
- **Streaks reset across rounds, not games** — actually, no — streaks persist across questions but break on a wrong answer. Don't reset between rounds.

## Checklist

- [ ] All field additions on Showcase / ShowcaseSettings / ShowcasePlayer / PlayerAnswer / PlayerPlacement
- [ ] `customRoomCode` validation + collision check
- [ ] `shuffleQuestions` at game start
- [ ] `shuffleAnswers` per-player deterministic
- [ ] `speedBonusAwarded` calculation
- [ ] `currentStreak` / `longestStreak` updates
- [ ] `accuracy` recompute
- [ ] `autoAdvance` scheduling
- [ ] `AvatarService` + preset pool
- [ ] Lobby avatar picker
- [ ] Streak indicator in player view
- [ ] Host autoAdvance countdown ring
- [ ] Per-player accuracy + streak in placement card
- [ ] Frontend codegen + lint
- [ ] Backend tests pass
