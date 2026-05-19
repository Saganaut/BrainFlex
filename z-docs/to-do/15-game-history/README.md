# 15 — Game history

**Status:** Not started
**Depends on:** 13 (`PlayerAnswer.timeTakenMs`, streak fields)
**Unblocks:** 16 (analytics), 17 (achievements trigger from history events)

## Scope

Per-user history of every showcase they've played in. Drives the "Recent games" UI on the user dashboard, achievement triggers (chunk 17), and feeds into the rolled-up `PlayerStats` numbers.

Today, `ShowcaseResult` exists but it's per-game, not per-user. Listing "games this user has played" requires a collection scan. `GameHistoryEntry` is the per-user denormalized index.

## New models

```text
GameHistoryEntry                       @Document("game_history")
  @Id String id
  @Indexed String userId
  @Indexed String showcaseId
  String deckId, deckName              // denorm — survives deck deletion
  String hostUserId, hostName
  int finalScore, placement
  int totalQuestions, correctAnswers
  int longestStreak, currentStreakAtEnd
  double accuracy
  int reactionsSent
  long durationMs                      // showcase.endedAt - showcase.startedAt
  String teamId, teamName              // nullable
  boolean wasHost                      // true when the user was the host (counts as a "game I ran")
  boolean wasGuest                     // joined as guest
  LocalDateTime playedAt               // = showcase.endedAt
```

Indexes:

- `(userId, playedAt DESC)` — primary list query
- `(showcaseId)` — for backfill / reconciliation
- `(userId, deckId, playedAt DESC)` — "every time I played this deck"

## Backend changes

- `GameHistoryRepository`
- `GameHistoryService.recordFinish(Showcase)` — called from `ShowcaseService.finish()`:
  - One `GameHistoryEntry` per player (including guests)
  - One additional entry for the host with `wasHost=true` (even if the host didn't play)
  - Also update `User.stats` (`PlayerStats`) for non-guest players: `gamesPlayed++`, `totalPoints += finalScore`, `highScore = max(highScore, finalScore)`, `currentStreak` (game-level streak — consecutive games played; reset if >7 days between games)
- Endpoints:
  - `GET /api/users/me/history?page=&size=` — paginated
  - `GET /api/users/{userId}/history?page=&size=` — public profile view (only registered, non-private)
  - `GET /api/decks/{deckId}/history/mine` — every time the caller played this deck (for "your best score" surfacing)
- Update `Membership` quota tracking: if quotas are in place, bump `monthlyShowcaseCount` when the host's entry writes

## Frontend changes

- `/profile` (or `/account`) gains a "History" tab with a paginated list
- Each entry: deck cover, deck name, placement (badge), score, host, played-at (relative time)
- Deck detail page shows "Your best: 1,240 (rank #3 of 12)" pulled from `/api/decks/{deckId}/history/mine`
- Stats summary cards above the history list: total games, best placement, total points, average accuracy

## Cross-cutting concerns

- **Guests:** write history rows for guests too (use the guest's `userId`). When a guest converts to a registered user via Google sign-in, the existing convert flow updates the row's `wasGuest` field or just stops writing new `wasGuest=true` entries.
- **Deck deletion:** denorm `deckName` so history rows remain readable after the deck is gone.
- **Backfill:** one-time script that walks existing `ShowcaseResult` + `Showcase` documents and writes history rows. Idempotent: skip if a row with `(userId, showcaseId)` already exists.

## Checklist

- [ ] `GameHistoryEntry` model + repo + indexes
- [ ] `GameHistoryService.recordFinish` writes per-player + host entries
- [ ] `PlayerStats` updates wired in
- [ ] Backfill script
- [ ] Endpoints + tests
- [ ] History tab on profile page
- [ ] "Your best" widget on deck detail page
- [ ] Stats summary cards
- [ ] Frontend codegen + lint
- [ ] Backend tests pass
