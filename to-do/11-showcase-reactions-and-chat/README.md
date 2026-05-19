# 11 — Showcase reactions & chat

**Status:** Not started
**Depends on:** Nothing strict; pairs well with chunk 13 (settings flags) and chunk 18 (notifications hooks for `@mention`)
**Unblocks:** 13 (`ShowcasePlayer.reactionsSent` counter)

## Scope

Two engagement features that live inside an active Showcase:

1. **Reactions** — players send single-emoji bursts during a round; they fly across the host screen. Stored for analytics + replays.
2. **Chat** — audience chat sidebar during the show, with host moderation.

Both flow over the existing STOMP WebSocket. Both are gated behind per-Showcase settings flags so hosts can turn them off.

## New models

```text
Reaction                               @Document("reactions")
  @Id String id
  @Indexed String showcaseId
  String elementId                     // which round it landed on (snapshot)
  String userId, userName              // userName is denorm for display
  boolean isGuest
  String emoji                         // single emoji codepoint, validated
  long offsetMs                        // ms since `roundStartedAt`
  LocalDateTime sentAt
```

```text
ShowcaseChatMessage                    @Document("showcase_chat")
  @Id String id
  @Indexed String showcaseId
  String authorUserId, authorName, authorPictureUrl
  boolean fromHost
  boolean isGuest
  String body                          // max 500 chars, plain text
  LocalDateTime sentAt
  boolean moderated                    // host hid the message
  String moderatedByUserId
  LocalDateTime moderatedAt
```

Indexes for both: compound `(showcaseId, sentAt DESC)`.

## Backend changes

- New STOMP destinations:
  - Client → server: `/app/showcase/{roomCode}/reaction` and `/app/showcase/{roomCode}/chat`
  - Server → topic: `/topic/showcase/{roomCode}/reaction` and `/topic/showcase/{roomCode}/chat`
- New REST fallbacks:
  - `POST /api/showcases/{roomCode}/reactions` — body `{ emoji }`; for clients without an open WS
  - `POST /api/showcases/{roomCode}/chat` — body `{ body }`
  - `GET  /api/showcases/{roomCode}/chat?page=` — replay chat history (for late-joiners)
  - `PUT  /api/showcases/{roomCode}/chat/{messageId}/moderate` — host only; flip `moderated=true`
- Service rules:
  - `ShowcaseService.acceptReaction(...)` — validates `settings.reactionsEnabled` and the current element's `reactionsEnabled` flag (chunk 10). Validates emoji length and a unicode-emoji-regex allow-list. Rate-limit per player (e.g. max 10 reactions / 5s).
  - `ShowcaseService.acceptChat(...)` — validates `settings.chatEnabled`, length ≤ 500, rate-limits per player. Auto-flags the host's messages with `fromHost=true`.
- Aggregation:
  - Reactions: cached count per emoji per element in `ShowcaseCacheService` (Redis HINCRBY); persisted to Mongo asynchronously.
  - Chat: stream the latest 50 messages on join (replay) and append new ones live.

## Frontend changes

- `ReactionBar` component pinned to the player view — six default emojis (configurable later) with a long-press for the full picker. Wires `useSendReactionMutation` (or the STOMP send directly).
- `ReactionRain` component on the host view — listens to the `/topic/.../reaction` subscription and animates emoji flying across the screen. Use CSS transforms + `requestAnimationFrame`.
- `ChatPanel` sidebar — toggleable on host + player views. Shows the latest messages, host messages styled distinctively. Optimistic local append on send; reconcile when the STOMP echo arrives.
- Host moderation: hover a message → "Hide" button → flips `moderated=true`. Hidden messages render as a placeholder "(hidden by host)" for non-hosts.

## Settings flags

Add these to `ShowcaseSettings` (also covered in chunk 13 but list here so the dependency is obvious):

- `boolean reactionsEnabled` — default `true`
- `boolean chatEnabled` — default `true`

Plus the existing per-slide `reactionsEnabled` from chunk 10 lets hosts mute reactions on, e.g., a moment-of-silence slide.

## Rate limiting

Per-player per-minute caps, enforced via Redis sorted-set sliding window:

- Reactions: 30 / minute
- Chat: 20 / minute, with a token-bucket cool-down after that

Over-limit submissions get a `429` HTTP response (REST) or a STOMP error frame.

## Checklist

- [ ] `Reaction` + `ShowcaseChatMessage` models + repos + indexes
- [ ] STOMP destinations registered in `WebSocketConfig`
- [ ] `ShowcaseService` accept methods + rate limiting + emoji validation
- [ ] REST fallback endpoints + tests
- [ ] Redis-backed aggregation for reaction counts
- [ ] `ShowcaseSettings.reactionsEnabled`, `chatEnabled` flags (coordinate with chunk 13)
- [ ] `ReactionBar` (player) + `ReactionRain` (host) components
- [ ] `ChatPanel` (host + player) component with host moderation
- [ ] Optimistic chat send + STOMP reconcile
- [ ] Frontend codegen + lint
- [ ] Backend tests pass
