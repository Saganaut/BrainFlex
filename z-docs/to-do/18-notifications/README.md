# 18 — Notifications

**Status:** Not started
**Depends on:** Nothing strict; consumes events from 04 (comments), 06 (collab invites), 14 (showcase invites), 17 (achievements)
**Unblocks:** A future in-app inbox / email digest

## Scope

In-app notification stream. One row per notification, marked read when the user opens it. Surfaces via a bell icon dropdown in the navbar, with realtime push over WebSocket. Email + push (later) consume the same model.

## New models

```text
Notification                           @Document("notifications")
  @Id String id
  @Indexed String userId
  NotificationKind kind
  String title                         // short — fits in a dropdown row
  String body                          // expanded copy; markdown
  String link                          // in-app route, e.g. /decks/abc123
  String iconUrl                       // nullable — uses a default per kind otherwise
  Map<String, String> meta             // small kv map: {showcaseId, deckId, commentId, achievementId, ...}
  String actorUserId                   // who triggered it; nullable for system notifications
  String actorName, actorPictureUrl    // denorm
  boolean read
  LocalDateTime createdAt, readAt
```

Indexes:

- `(userId, createdAt DESC)`
- `(userId, read)` — for unread badge count

```text
NotificationKind (enum)
  SHOWCASE_INVITE          // chunk 14
  SHOWCASE_STARTING_SOON   // chunk 14, scheduled reminder
  DECK_COMMENT             // chunk 04 — new top-level comment on your deck
  DECK_COMMENT_REPLY       // chunk 04 — reply to your comment
  DECK_RATING              // chunk 04 — new rating on your deck
  DECK_FAVORITED           // chunk 03 — someone favorited your deck (throttled)
  COLLAB_INVITE            // chunk 06
  COLLAB_ACCEPTED          // chunk 06
  ACHIEVEMENT              // chunk 17
  ORG_INVITE               // org join request
  ORG_JOINED               // someone joined your org
  MENTION                  // future — @-mention in chat or comments
  SYSTEM                   // platform announcements
```

## Backend changes

- `NotificationRepository`, `NotificationService`
- `NotificationService.send(userId, NotificationKind, title, body, link, meta, actor)`:
  - Insert the row
  - Push over STOMP `/user/{userId}/queue/notifications` (use `convertAndSendToUser`)
  - Check `User.notificationPrefs` (chunk 20) — if the user has muted this kind, **still write the row** but skip the push and the email
  - Hand off to `EmailService` if `notificationPrefs.emailFor.{kind}` is true (chunk 14 dependency for `EmailService`)
- Endpoints:
  - `GET    /api/notifications?page=&size=` — paginated, newest first
  - `GET    /api/notifications/unread-count` — small response, called every 60s by the frontend
  - `PUT    /api/notifications/{id}/read` — mark one
  - `PUT    /api/notifications/read-all`
  - `DELETE /api/notifications/{id}` — dismiss
- Wiring (event listeners — keep notification side-effects out of the primary service path; emit a Spring `ApplicationEvent` and let `NotificationEventListener` handle the side effect async):
  - Chunk 03: `DeckFavoritedEvent` → `DECK_FAVORITED` (throttle: max 1 per actor per deck per day)
  - Chunk 04: `DeckCommentCreatedEvent` → `DECK_COMMENT` or `DECK_COMMENT_REPLY`
  - Chunk 04: `DeckRatingCreatedEvent` → `DECK_RATING`
  - Chunk 06: `DeckCollaboratorInvitedEvent` → `COLLAB_INVITE`
  - Chunk 14: `ShowcaseInviteSentEvent` → `SHOWCASE_INVITE` (in-app row complements the email)
  - Chunk 14: `ScheduledShowcaseBootingEvent` → `SHOWCASE_STARTING_SOON` to all invitees, 5 min before boot
  - Chunk 17: `AchievementEarnedEvent` → `ACHIEVEMENT`

## Frontend changes

- Bell icon component in `NavBar` showing unread badge count
- Bell click opens a dropdown panel with:
  - "Mark all as read" link
  - List of notifications grouped by day (Today / Yesterday / This week / Earlier)
  - Click → mark read + navigate to `link`
- Realtime: subscribe to `/user/queue/notifications` over the existing STOMP client; prepend new rows and bump the badge
- Polling fallback: `useGetUnreadNotificationCountQuery` with `pollingInterval: 60000` for when the WS is closed
- Toast on receive (optional — controlled by a user pref `showNotificationToasts`)

## Cross-cutting concerns

- **Don't notify the actor about their own action** — skip `userId === actorUserId`.
- **Throttling** — `DECK_FAVORITED` is the most spammy. Add a Redis-backed dedupe key `notif:favorited:{deckOwnerId}:{actorId}:{deckId}` with 24h TTL.
- **Retention** — keep notifications for 90 days, then TTL-delete via a Mongo TTL index on `createdAt`.

## Checklist

- [ ] `Notification` model + repo + TTL index
- [ ] `NotificationKind` enum
- [ ] `NotificationService.send` writes row + STOMP push
- [ ] STOMP user-destination configured in `WebSocketConfig`
- [ ] Spring `ApplicationEvent` listeners for chunks 03/04/06/14/17
- [ ] Throttle `DECK_FAVORITED` via Redis dedupe key
- [ ] Endpoints + tests
- [ ] Bell icon + dropdown component
- [ ] STOMP subscription + polling fallback
- [ ] Toast on receive (gated by user pref)
- [ ] Frontend codegen + lint
- [ ] Backend tests pass
