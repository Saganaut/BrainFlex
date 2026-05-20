# Features

Per-feature design docs. One subfolder per feature; each owns its `README.md` plus any supporting notes, mockups, or migration scripts.

## Existing

- [Auth](auth/README.md) — Google OAuth + guest sessions, cookie + Redis session store.
- [Games](games/README.md) — InteractiveSessions & decks implementation checklist and notes.
- [Membership](membership/README.md) — Organization membership semantics and flows.

## Adding a new feature

1. Create `z-docs/features/<feature-name>/` with a `README.md` describing scope, models, endpoints, and an implementation checklist.
2. Add a row above so the linter can reach it.
3. Drop any supporting docs into the same folder and link them from that feature's `README.md`.
