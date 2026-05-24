# Session logic audit

> Audit of `frontend/src/pages/SessionPage/useSession.ts` and the surrounding
> Gen-2 interactive-session logic (slice, WebSocket hook, connection provider,
> board, controls, chat). Captured 2026-05-24. Nothing here is fixed yet — this
> is a backlog of findings with file:line references and a priority-ordered
> action list at the bottom.

Scope reviewed:

- `pages/SessionPage/useSession.ts`, `SessionConnectionProvider.tsx`, `SessionConnectionContext.ts`
- `hooks/useInteractiveSession.ts`, `hooks/useInteractiveSessionWebSocket.ts`, `hooks/useStartInteractiveSession.ts`
- `store/interactiveSessionSlice.ts`, `store/enhancements/interactiveSession.ts`, `store/enhancements/chat.ts`
- `components/Session/**` (Board, Controls, Chat, Header, RoundTracker, PlayerList, content/*)
- Backend `controller/InteractiveSessionWebSocketController.java` + `service/InteractiveSessionService.java` broadcast topics (for the coverage diff)

---

## 1. Hook code smells

### 1a. `useSession` is a leaky partial-merge abstraction (the central smell)

`mergeSessionView` (`useSession.ts:38-66`) promises "one session view," but it only
overlays *some* live fields onto the REST snapshot. Consumers still reach past it
into the slice for the rest:

| Live datum | In merged view? | Also read directly from slice by |
| --- | --- | --- |
| `status`, `phase`, `round`, `players`, `teams`, `viewerPlayerId` | ✅ | — |
| `timerPaused`, `revealedElementIds` | ✅ **and** also read raw | `SessionControls.tsx:52-53` |
| `roundResult` | ❌ (not merged) | `SessionBoard.tsx:29`, `SessionControls.tsx:60`, `McqBoardContent.tsx:70` |
| `myAnswer`, `submissionsClosing` | ❌ | `McqBoardContent.tsx:69`, `useFlushOnClosing.ts:29` |

Two concrete problems fall out of this:

- **Two sources for the same datum that can disagree.** `mergeSessionView` only uses
  `live.timerPaused` when `seeded` (`useSession.ts:45,63`), otherwise falls back to the
  snapshot — but `SessionControls.tsx:52` *always* reads `live.timerPaused`. Pre-seed
  (or right after `resetSession`) the merged value and the direct selector diverge.
- **Stale comments mark the seam as unfinished when it isn't.** `SessionControls.tsx:50-51`
  ("until then they read the slice defaults") and `useFlushOnClosing.ts:11-13`
  ("content components are not yet wired to `sendAnswer`") are both false now —
  `useSession` *is* wired to Redux and `McqBoardContent.tsx:84` *does* call `sendAnswer`.
  These read like a half-finished migration: components written against the raw slice
  were never moved onto the merged view.

### 1b. The merge is hand-maintained and will silently drift

Every field is mapped by hand, including null→undefined bridging
(`timerRemainingMillis: live.timerRemainingMillis ?? undefined`, `useSession.ts:64`)
and a `||` that swallows a legitimate `0`
(`totalRounds: live.totalRounds || snapshot.totalRounds`, `useSession.ts:58`).
Nothing enforces the merge stays complete when a field is added to the slice or
DTO — `roundResult`/`myAnswer` are already proof it didn't keep up.

### 1c. `mergeSessionView` throws on no-snapshot — dead defense

`useSession.ts:47-49` throws if `!snapshot`, but `SessionConnectionProvider.tsx:52-58`
already gates children on `data`, so the throw can't fire. It just lets the function
lie about its non-optional return type.

### 1d. `useInteractiveSessionWebSocket` is a ~290-line monolith

`hooks/useInteractiveSessionWebSocket.ts` does three jobs: connection lifecycle, 17
inline topic subscriptions, and 14 send-action callbacks. The subscription block is
17× the identical shape
`client.subscribe(suffix, msg => dispatch(action(JSON.parse(msg.body) as T)))`
(`:65-209`). That repetition is what makes "is every topic subscribed?" hard to verify
by eye.

### 1e. `send()` silently drops when disconnected — correctness gap

`useInteractiveSessionWebSocket.ts:222-230`: `if (client?.connected)` — a host clicking
*Start* during the 3 s reconnect window (`reconnectDelay: 3000`) loses the action with
zero feedback. For host game control this is a real correctness gap, not cosmetic.
At minimum it should surface failure or queue the send.

### 1f. Minor: magic `round: 0` in `sendRevealNow`

`useInteractiveSessionWebSocket.ts:293` dispatches `responsesRevealed({ round: 0, ... })`.
Harmless only because the reducer ignores `round` for `responsesRevealed` — but it's a
fake field value.

---

## 2. Centralization / organization opportunities

- **Pick one read path and enforce it.** Either make `useSession` the *complete* live
  view (add `roundResult`, `myAnswer`, `submissionsClosing`, and a derived
  `viewerIsHost`) and ban direct `state.interactiveSession` reads in components — or
  drop the merge entirely and let `SessionConnectionProvider` seed the slice (it already
  dispatches `setSession`, `SessionConnectionProvider.tsx:41-43`) so the slice *is* the
  single source and components use scoped selectors. The current half-and-half is the
  worst of both.
- **Derive `viewerIsHost` once.** `viewerPlayerId === hostPlayerId` is recomputed in
  `SessionBoard.tsx:30-32` and `SessionControls.tsx:63`, then passed into
  `resolveBoardStage`. Expose it from `useSession` (or a selector).
- **Table-drive the WS subscriptions.** A `Record<suffix, (body) => Action>` map collapses
  ~140 lines (`useInteractiveSessionWebSocket.ts:65-209`) to a loop, and makes the
  topic-coverage audit a one-glance diff against the backend.

---

## 3. Missing wiring — generated API & STOMP for game control

### 3a. STOMP coverage

Diffed backend `@MessageMapping`s (`InteractiveSessionWebSocketController.java`) and
broadcast topics (`InteractiveSessionService.java`) against the hook:

- **All 15 broadcast topics are subscribed** (lobby, round, roundResult, ended, answered,
  votePhase, voted, wordCloud, reaction, chat, teams, summary, responsesRevealed,
  submissionsClosing, timerState) + `/topic/presence` + `/user/queue/errors`. ✅ No
  missing receivers.
- **Three client→server destinations have no send function:**
  - **`/reaction`** — backend handler exists; slice has `reactionReceived` /
    `liveReactions` / `reactionConsumed`; but there is **no `sendReaction`**.
    `SessionChat`'s `onReact` only appends to local mock state (`SessionChat.tsx:270-272`).
    Reactions never leave the device.
  - **`/chat`** — backend handler + slice `chatMessageReceived` / `chatHistoryLoaded`
    exist, but no STOMP `sendChat`, and `SessionChat` calls neither that nor the REST
    mutation.
  - **`/chat/moderate`** — backend handler exists; no host moderation send wired.

### 3b. Generated REST endpoints that exist but are never called

- `useSendChatMutation`, `useModerateChatMutation`, `useSendReactionMutation` —
  **0 usages.** The optimistic cache patches in `store/enhancements/chat.ts` are written
  and registered but can never fire because nothing invokes the mutations.
- `useListChatQuery` — **0 real usages** (only named in a slice comment,
  `interactiveSessionSlice.ts:219`). Chat history is never seeded.
- Team mutations (`useCreateTeam` / `useUpdateTeam` / `useDeleteTeam` /
  `useMovePlayerToTeam`) — 0 usages, though `store/enhancements/interactiveSession.ts:73-88`
  wires their cache-sync. No team-management UI in Gen-2 yet.

### 3c. Orphaned live pipelines (subscriptions ahead of renderers)

The WS hook dispatches into slice state that *no Gen-2 component reads*:

- `chat` → `SessionChat` ignores it (uses `mockFellowshipChat`).
- `liveReactions` → no `ReactionRain` component exists in Gen-2 (only in comments);
  nothing reads it.
- `offlineUserIds` → the slice's own comment admits the indicators "silently no-op"
  (`interactiveSessionSlice.ts:189-195`).
- `wordCloudCounts`, and the entire **VOTE phase**
  (`votePhaseStarted` / `voteProgressReceived` / `sendVote`) → `resolveBoardStage` has no
  VOTE branch and `BoardQuestion` only renders MCQ (everything else →
  `BoardContentPlaceholder`, `BoardQuestion.tsx:64-67`). **Best Answer rounds are
  currently unrenderable.**

This cluster is expected mid-migration (the board is built kind-by-kind, MCQ only so
far), but it's worth tracking explicitly: the receive side is complete while the send
side and the renderers are not.

### 3d. Backend DTO gap: no `deckName` on the session

`useSession` fires a *second* authenticated fetch — `useGetDeckQuery`
(`useSession.ts:73-76`) — purely to get the deck name for the header
(`SessionHeader.tsx:19`). `InteractiveSessionResponse` carries
`deckId` / `deckVersion` / `deckSnapshot` but **no `deckName`** (`BrainFlexApi.ts:2105-2149`).
A non-host participant may lack read access to the deck, so the title silently blanks for
them. Adding `deckName` to the session DTO removes the round-trip and the permission
coupling.

---

## Action list (priority order)

- [x] **Collapse to one read path** — `useSession` is now the complete live view
  (`roundResult`, `myAnswer`, `submissionsClosing`, derived `viewerIsHost` exposed
  alongside the merged `interactiveSession`), and `SessionControls` / `SessionBoard` /
  `McqBoardContent` / `useFlushOnClosing` read everything through it — no component
  touches `state.interactiveSession` directly. The only remaining slice read is the
  base `useInteractiveSession` selector that `useSession` itself consumes (the
  slice→useSession seam). Stale "until useSession is wired" comments removed. (§1a, §1b, §2)
- [~] **Wire reactions + chat end-to-end** (§3a, §3b, §3c) — **live core done** via the
  slice+STOMP architecture (Option A): `sendChat` / `sendReaction` added to the connection
  hook (the server persists + echoes both back on `/chat` and `/reaction`, so the sender
  sees their own through the normal slice path — no optimistic echo); `SessionChat` now
  reads `chat` + `liveReactions` through `useSession` (reactions render inline as
  chrome-less rows) instead of `mockFellowshipChat`; the dead `store/enhancements/chat.ts`
  optimistic patches (which targeted the unread `listChat` cache via never-called
  mutations) were **deleted** — STOMP is canonical, matching the backend's "STOMP
  preferred" stance. **Deferred (by scope):** chat-history seed via `useListChatQuery` →
  `chatHistoryLoaded`, host moderation UI → `sendChatModerate`, and a `ReactionRain`
  overlay. The generated `useSendChat`/`useModerateChat`/`useSendReaction` mutations remain
  intentionally unused (REST is the fallback path).
- [x] **Surface/queue dropped sends on disconnect** — `send()` now buffers a publish when
  the STOMP client is mid-reconnect (in a `pendingRef`) and flushes the queue in
  `onConnect`, so a host action fired during the 3 s `reconnectDelay` window publishes on
  reconnect instead of being silently dropped. The buffer is cleared on teardown so it
  can't leak onto a different room's socket. (§1e)
- [ ] **Add `deckName` to `InteractiveSessionResponse`** (backend) and drop the second
  `useGetDeckQuery` fetch in `useSession`. Regenerate `BrainFlexApi.ts`. (§3d)
- [x] **Table-drive the WS subscription block** — the ~15 near-identical inline
  subscriptions are now a `SESSION_TOPICS` map (suffix → slice action) iterated in a single
  loop in `onConnect`; the two non-session-scoped destinations (`/topic/presence`,
  `/user/queue/errors`) stay explicit. Topic coverage is now a one-glance diff against
  `InteractiveSessionService`'s broadcast topics. (§1d, §2)
- [x] Remove the dead no-snapshot throw in `mergeSessionView`; fix `||` on
  `totalRounds`. (§1b, §1c) — `mergeSessionView` now takes a required snapshot (no
  internal throw); the provider-contract guard moved up to the `useSession` boundary,
  mirroring `useSessionConnection`. `totalRounds` reads `live.totalRounds` directly
  (the slice owns it once seeded, so 0 is no longer swallowed by a snapshot fallback).
- [ ] (Tracked elsewhere, noted for completeness) VOTE-phase board stage + per-kind board
  content (WordCloud, etc.) so the orphaned slice pipelines have renderers. (§3c)
