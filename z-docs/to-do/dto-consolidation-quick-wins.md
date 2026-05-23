# DTO / Type Consolidation — Quick Wins

## 2. Question/Answer parallel hierarchies — see `java-model-issues.md` §1a, §6a

The `ElementChrome` extraction (chunk 25) already resolved the 30-field repetition across `*Question` records. What's still open:

- `model/answer/*Answer.java` (14 files) — no shared base; each is an independent record. Mirror the `ElementChrome` pattern with an `AnswerChrome` (or just a tagged `AnswerPayload` sealed interface, which already exists).
- No compile-time link between `ElementKind` and the matching `AnswerPayload` subtype (§6a). Consider a parameterized `Scorer<Q extends DeckElement, A extends AnswerPayload>` to enforce pairing.

This is the **largest remaining duplication** but also the riskiest refactor. Defer until after #1 and #4 land.

---

## 3. WebSocket `*Message` DTOs — see `java-model-issues.md` §7a–§7e

13 broadcast messages with no common envelope. §7a recommends a `BroadcastEnvelope<T>(roomCode, round, sentAtEpochMs, payload)`. Concrete bugs §7a–§7e flag:

- Only 1 of 13 carries `roomCode` (`PresenceMessage` leaks across sessions — §7e).
- Only 3 of 13 carry a timestamp (§7c).
- `RoundResultMessage.format` is the only broadcast field that duplicates session-level state (§7b).

Doing this well requires touching the frontend STOMP consumers in lockstep, so it's a multi-PR effort. Worth scheduling its own chunk.
