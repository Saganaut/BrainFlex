# 17 — Achievements

**Status:** Catalog backend + frontend shipped; game-results surface, toast, and `REACTIONS_SENT` / `WORD_CLOUD_SUBMITTED` triggers deferred (see [to-do README](../README.md#deferred-work--come-back-to-this)).
**Depends on:** 15 (history is the primary trigger source), 13 (streak fields)
**Unblocks:** Nothing critical — pure gamification

## Scope

Badges that users earn through gameplay. Triggered by events from `GameHistoryEntry` writes (game finish), `InteractiveSessionPlayer` writes (in-game streak hits), and `Reaction` writes. Mostly fire-and-forget — when an achievement triggers, write a `UserAchievement` row and a toast notification.

## New models

```text
Achievement                            @Document("achievements")
  @Id String id                        // slug, e.g. "first-game", "streak-10", "perfect-deck"
  String name
  String description
  String iconUrl                       // S3 or static asset
  String category                      // e.g. "starter", "scoring", "social", "creator", "host"
  AchievementTrigger trigger
  int threshold                        // semantics depend on trigger
  int rewardPoints                     // bonus PlayerStats.totalPoints when earned (optional)
  boolean hidden                       // hidden achievements only reveal after unlock
  int displayOrder
  LocalDateTime createdAt
```

```text
UserAchievement                        @Document("user_achievements")
  @Id String id
  @Indexed String userId
  @Indexed String achievementId
  LocalDateTime earnedAt
  String earnedInInteractiveSessionId            // nullable
  String earnedInDeckId                // nullable
  // compound unique index (userId, achievementId)
```

```text
AchievementTrigger (enum)
  FIRST_GAME                           // any finished game
  GAMES_PLAYED                         // threshold = count
  TOTAL_POINTS                         // threshold = lifetime totalPoints
  HIGH_SCORE                           // threshold = single-game finalScore
  STREAK                               // threshold = longestStreak in any game
  PERFECT_GAME                         // accuracy = 1.0 in any game with >= threshold questions
  HOST_GAMES                           // threshold = games hosted
  DECKS_CREATED                        // threshold = decks owned
  DECKS_PUBLISHED                      // threshold = decks with PublishStatus.PUBLISHED
  REACTIONS_SENT                       // threshold = lifetime reactions
  FAVORITES_RECEIVED                   // threshold = sum of favoriteCount across owned decks
  WORD_CLOUD_SUBMITTED                 // threshold = lifetime WordCloudAnswer submissions
  // ... extend as needed
```

## Backend changes

- `AchievementRepository`, `UserAchievementRepository`
- `AchievementService.evaluate(userId, AchievementTrigger, int currentValue)`:
  - Find all `Achievement` rows with this trigger + `threshold <= currentValue`
  - For each, attempt to insert a `UserAchievement` (use unique index to dedupe)
  - On successful insert, emit a notification (chunk 18) and bump `User.stats.totalPoints += rewardPoints`
- Call sites:
  - `GameHistoryService.recordFinish` → `evaluate(GAMES_PLAYED)`, `evaluate(TOTAL_POINTS)`, `evaluate(HIGH_SCORE)`, `evaluate(STREAK)`, `evaluate(PERFECT_GAME)`, `evaluate(HOST_GAMES)`, `evaluate(REACTIONS_SENT)`
  - `DeckService.create` → `evaluate(DECKS_CREATED)`
  - `DeckService.publish` → `evaluate(DECKS_PUBLISHED)`
  - `DeckFavoriteService.favorite` → `evaluate(FAVORITES_RECEIVED, deck.favoriteCount)` for the deck owner
- Endpoints:
  - `GET /api/achievements` — full catalog (anyone)
  - `GET /api/users/me/achievements` — earned + locked (locked includes progress toward threshold)
  - `GET /api/users/{userId}/achievements` — public profile view (only earned, non-hidden)
- Seeding: ship 15–20 achievements via `SampleDataSeeder` or a dedicated migration. Categories: starter (first game, first deck), scoring (10 / 100 / 1000 games, 10k / 100k points), social (100 reactions, 100 favorites received), creator (1 / 10 / 50 decks published), host (10 / 100 games hosted).

## Frontend changes

- New `/achievements` route showing the full catalog with earned/locked state + progress bars
- Achievement toast component — slides in from the bottom-right when one fires. Listen via WebSocket subscription (`/user/queue/notifications` if chunk 18 ships first) or via polling the `userAchievements` query.
- Profile page gets an "Achievements" tab showing earned badges (newest first)
- Game results screen shows newly-earned achievements before the standard placement card

## Cross-cutting concerns

- **Don't block writes** — achievement evaluation should be async (fire-and-forget). If evaluation errors, log it but don't fail the parent write.
- **Idempotency** — the unique compound index on `(userId, achievementId)` guarantees no duplicate awards.
- **Hidden achievements** — surface in the catalog only after unlocked; until then show `???` with a generic icon.

## Checklist

- [x] `Achievement` + `UserAchievement` models + repos + unique index
- [x] `AchievementTrigger` enum (10 values; `REACTIONS_SENT` + `WORD_CLOUD_SUBMITTED` deferred — need a lifetime counter on User.stats)
- [x] `AchievementService.evaluate` (fire-and-forget, idempotent, guest-skip, reward-points bump)
- [x] Trigger call sites wired (`GameHistoryService.recordFinish` for player + host events; `DeckService.createDeck` / `DeckService.publish`; `DeckFavoriteService.favorite` for owner; `InteractiveSessionService.updateStatsAfterGame` for `TOTAL_POINTS`)
- [x] Seed catalog — 18 rows across starter / scoring / host / creator / social categories
- [x] Endpoints + tests (`GET /api/achievements`, `GET /api/users/me/achievements`, `GET /api/users/{userId}/achievements` — public/auth gates, hidden masking, progress against cheap counters)
- [ ] ~~Achievement toast component~~ — deferred to chunk 18 (needs notification stream or a stop-gap `convertAndSendToUser` push)
- [x] `/achievements` catalog route — earned/locked grid grouped by category with progress bars
- [x] Profile "Achievements" tab — earned-only summary newest-first, links out to the catalog
- [ ] ~~Results screen surface for new achievements~~ — deferred to the holistic chunk-13 player-UI pass
- [x] Frontend codegen + lint
- [x] Backend tests pass
