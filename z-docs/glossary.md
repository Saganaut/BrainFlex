# Glossary

Domain terms used throughout BrainFlex. Add new entries here when you introduce a concept that isn't self-evident from the name.

| Term | Meaning |
| ---- | ------- |
| **Deck** | A collection of `DeckElement`s (slides + questions) authored together. Plays as a sequence in a Showcase. Persisted in the `decks` MongoDB collection. |
| **Element** | A single item in a deck: a `Slide` or a `Question`. Sealed Java hierarchy under `model/element/`; embedded in the parent `Deck` document. |
| **Element kind** | The polymorphic discriminator for `DeckElement` (`SLIDE`, `MCQ`, `TEXT`, `NUMBER`, `IMAGE_CHOICE`, `RANKING`, `SCALES`, `Q_AND_A`, `GRID`, `PLACE_ON_IMAGE`). |
| **Slide** | A `DeckElement` with no scoring — title/section/callout/content/end variants. Used for presentation framing inside a deck. |
| **Question** | A scoreable `DeckElement` (sealed sub-interface). Each kind carries its own correctness shape and accepts a typed `AnswerPayload`. |
| **MCQ** | Multiple-choice question with `List<McqOption> options` and `correctOptionIds: List<String>` (multi-correct). Empty `correctOptionIds` is allowed but marks the slide unscoreable. |
| **AnswerPayload** | Sealed interface for typed player answers (`McqAnswer`, `TextAnswer`, `NumberAnswer`, …, `TimeoutAnswer`). Players submit one per round. |
| **Showcase** | A live, hosted play session of a Deck. Snapshots the deck's elements at create time so author edits mid-game don't desync clients. Tracks players, scores, and current phase. |
| **Showcase phase** | `SUBMIT` → `VOTE` → `REVEAL`. Only Best-Answer-mode questions enter `VOTE`. |
| **Organization** | A user-created group. Themes and decks can be scoped to an org (visible to all members). Users may belong to multiple orgs via `User.organizationIds`. |
| **Theme** | Per-user (or per-org) palette: `huePrimary`, `hueAccent`, light/dark/system mode, optional background + logo. Lives in `themes` collection. |
| **Galaxy/Best Answer mode** | Question flag (`bestAnswerMode = true`) that adds a voting phase after submission. The most-voted submission gets a `bestAnswerBonus`. |
| **Pulse mode** | Showcase preset with `scoringEnabled = false`. Audience-feedback style — submissions collected and visualized, not graded. |
| **Garage** | Self-hosted S3-compatible object store running in Docker Compose. Backs all image uploads (avatars, theme logos/backgrounds, gallery images). |
| **Auto-memory** | Persistent agent state under `memory/` (not project docs). Excluded from `doc-lint`. |
