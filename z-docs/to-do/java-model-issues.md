# Java Model / DTO Audit

Audit of every `.java` file under `backend/src/main/java/cephadex/brainflex/model/` (incl. `element/`, `answer/`, `enums/`) and `backend/src/main/java/cephadex/brainflex/dto/`. Findings are concrete, file-cited, and ordered roughly by category. Each entry: *what's there → why it's an issue → suggested direction*.

The codebase is mid-migration in several places (chunk 10/12/13/24, the `mode` → `answerSubmissionMode` rename, `bestAnswerBonus` → `bestAnswerPoints`, the `recommendedPreset` → `defaultSessionFormat` move), so several findings are migration-cleanup tasks rather than bugs.

---

## 1. Repetition / boilerplate worth consolidating

### 1a. The 11 scoreable `DeckElement` records repeat the same ~30-field "shared chrome" block ✅ resolved (chunk 25)

Resolution: introduced `model/element/ElementChrome` carrying the 30 shared fields. Every record now declares a single `ElementChrome chrome` component plus its kind-specific fields; the `DeckElement` interface exposes the old flat accessors via default methods that delegate to `chrome()`. `DeckElementCloner`, `ElementRedactor`, `ElementShuffler`, `DeckImageMapper`, `SampleDataSeeder`, and `SlideBlocksMigrationRunner` were rewritten against the new shape; tests and frontend `MockData.ts` updated to mirror the nested layout. Issue 6b (Slide-only display options that should be shared) and 9n (mixed `Image`/`MediaAsset` references) are still open.

Files (all in `backend/src/main/java/cephadex/brainflex/model/element/`):

- `McqQuestion.java`
- `TextQuestion.java`
- `NumberQuestion.java`
- `RankingQuestion.java`
- `ScalesQuestion.java`
- `QAndAQuestion.java`
- `GridQuestion.java`
- `PlaceOnImageQuestion.java`
- `WordCloudQuestion.java`
- `AllocationQuestion.java`
- `MatchingQuestion.java`
- `DrawingQuestion.java`

Every one of these records re-declares the same components in the same order: `id, publicKey, privateKey, title, styledTitle, prompt, …, pointValue, difficulty, scored, survey, multipleSelections, responseMode, bestAnswerMode, bestAnswerTitle, @Field("bestAnswerBonus") int bestAnswerPoints, bestAnswerScoring, explanation, displaySeconds, speakerNotes, background, image, videoUrl, audioUrl, videoAssetId, audioAssetId, mediaPosition, …, createdByUserId, lastEditedByUserId, createdAt, updatedAt, tagIds, mediaCaption, altText, reactionsEnabled, Integer version`.

- Why it's an issue: ~30 components × 12 records = ~360 lines of identical record components. Adding one new chrome field (e.g. the `videoAssetId` you just added) requires editing 12 files. Slide.java is the same plus its own extras. `DeckElement.java` already has `default` methods covering most of these, but every record shadows the defaults by redeclaring the component, so the defaults are dead code for everything except `Slide`.
- Suggested direction: either (a) introduce an embedded `ElementChrome` value type / record that every question record holds as one component (`ElementChrome chrome`) and expose its fields via interface default methods that read from it; or (b) lean into the interface and remove the redeclarations on records, letting the interface defaults supply the value, with kind-specific overrides only where they differ. (a) is mechanical; (b) requires Jackson serialization tweaks.

### 1b. `@Field("bestAnswerBonus") int bestAnswerPoints` literally pasted in 11 files — **DONE**

Resolved by:
- `BestAnswerBonusRenameMigration` (gated on `--migrate.best-answer-bonus=true`) renames `bestAnswerBonus` → `bestAnswerPoints` inside every deck's `elements[]` via an aggregation-pipeline `updateMany` on the raw `decks` collection.
- `scripts/migrate-best-answer-bonus.sh` triggers it the same way as the other one-shot migrations.
- `@Field("bestAnswerBonus")` and the back-compat `Field` import dropped from all twelve scoreable question records (`McqQuestion`, `TextQuestion`, `NumberQuestion`, `RankingQuestion`, `ScalesQuestion`, `QAndAQuestion`, `GridQuestion`, `PlaceOnImageQuestion`, `WordCloudQuestion`, `AllocationQuestion`, `MatchingQuestion`, `DrawingQuestion`).

### 1c. Eight paginated response envelopes with identical shape

- `backend/src/main/java/cephadex/brainflex/dto/DeckExploreResponse.java`
- `backend/src/main/java/cephadex/brainflex/dto/DeckFavoritesPage.java`
- `backend/src/main/java/cephadex/brainflex/dto/DeckCollectionsPage.java`
- `backend/src/main/java/cephadex/brainflex/dto/DeckCommentsPage.java`
- `backend/src/main/java/cephadex/brainflex/dto/DeckRatingsPage.java` (extra `averageRating` / `ratingCount` / `starDistribution`)
- `backend/src/main/java/cephadex/brainflex/dto/GameHistoryPage.java`
- `backend/src/main/java/cephadex/brainflex/dto/NotificationPage.java`
- `backend/src/main/java/cephadex/brainflex/dto/UserAchievementsPage.java` (diverges — see 4d)

Every one carries `List<T> items, int page, int size, long totalElements, boolean hasMore`.

- Why it's an issue: zero variation across seven of the eight; the comments literally say "Mirrors the shape of {@link DeckFavoritesPage}". Generated TS client gets eight separate types for what is functionally `Page<T>`.
- Suggested direction: introduce a single generic `Page<T>(List<T> items, int page, int size, long totalElements, boolean hasMore)` record and use it everywhere; keep `DeckRatingsPage` separate (or as a wrapper) because it carries the rating histogram.

### 1d. `createdAt` / `updatedAt` field pair on every top-level `@Document`

Files containing both `createdAt` and `updatedAt` (often with `= LocalDateTime.now()` defaults): `Deck.java`, `DeckCollection.java`, `DeckRating.java`, `DeckComment.java`, `Tag.java`, `ScheduledInteractiveSession.java`. Files with `createdAt` only: `Achievement.java`, `User.java`, `Organization.java`, `Theme.java`, `GalleryImage.java`, `MediaAsset.java`, `UserAchievement.java`, `Notification.java`, `Membership.java`, `DeckCollaborator.java`, `DeckFavorite.java`, `EmailSuppression.java` (uses `Instant`), etc.

- Why it's an issue: no central audit-field wiring. Some fields auto-initialize via field-default `= LocalDateTime.now()`, some don't, some have matching `updatedAt`, some don't. Spring Data's `@CreatedDate` / `@LastModifiedDate` annotations + `@EnableMongoAuditing` would do this uniformly.
- Suggested direction: introduce a base `Auditable` superclass or interface (composed via `@Embedded` or via Lombok mixin) and enable Spring Data Mongo auditing. Drop the manual `= LocalDateTime.now()` initializers — they fire on object construction even when loading from Mongo and can mask real values during odd deserialization paths.

### 1e. "Denormalized author/actor display" fields repeated across collections — **DONE**

Resolved in [`UserSnapshot.java`](../../backend/src/main/java/cephadex/brainflex/model/UserSnapshot.java).

The duplicated `userId / name / pictureUrl / guest` snapshot is now a single value-type record. Every model that previously open-coded a denormalized "author / actor / participant" tuple embeds a `UserSnapshot` under a single, role-appropriate field (`author`, `actor`, `user`, `host`):

- `InteractiveSessionChatMessage.author`
- `DeckComment.author`
- `Reaction.user`
- `AudienceSubmission.user`
- `Notification.actor`
- `InteractiveSessionPlayer.user`
- `PlayerPlacement.user`
- `GameHistoryEntry.host`

The matching DTOs (`DeckCommentDTO`, `InteractiveSessionChatMessageDTO`, `ReactionBroadcastMessage`, `DeckRatingDTO`, `DeckCollaboratorDTO`, `NotificationDTO`, `GameHistoryDTO`, `InteractiveSessionDTO.InteractiveSessionPlayerDTO`) carry the same `UserSnapshot` in place of the flat fields. The wire shape changed accordingly — clients must read `author.userId` instead of `authorUserId`, etc.

Legacy accessors (`getAuthorUserId()`, `getActorName()`, `getUserId()`, `getUserName()`, `getPictureUrl()`, `getHostName()`, …) are preserved as `@JsonIgnore` convenience getters that delegate to the snapshot, so the Java surface stays familiar inside services and tests; the snapshot is the single source of truth.

Not in scope (follow-ups):
- A one-shot Mongo migration that lifts the flat fields into the nested snapshot for documents written before this refactor (parallel to [`DeckAnalyticsBackfillMigration`](../../backend/src/main/java/cephadex/brainflex/config/DeckAnalyticsBackfillMigration.java)). Re-seeding via `scripts/seed-sample-data.sh` is the dev workaround until that lands.
- The wider `DeckCollaboratorDTO.userName` vs `name` question — see 5g, which is unchanged.

### 1f. `Image` overrides via `withXxx` pattern repeated, but inconsistently

`Image.java:38` has `withVariants`. `McqOption.java:22` has `withImage`. `RankingItem.java:13` has `withImage`. `MatchingPair.java:19-25` has `withLeftImage` and `withRightImage`. `ImageBlock.java:23` has `withImage`. `GridCellsConfig.java:17` has `withBackingImage`.

- Why it's an issue: every record that owns an `Image` declares its own one-off "with" copier so the read-time hydrator (`DeckImageMapper`) can rebuild it. The pattern is fine, but the records are missing matching wither methods for *other* image fields — e.g. `Slide` has `background` and `image` and `videoAssetId` but no withers; `McqQuestion` has `image` and `background` but no withers. Hydration must be using reflection or a giant switch.
- Suggested direction: pick one approach — either every Image-bearing record has a uniform `withImageField(name, image)` (matched by name) helper, or extract the hydration responsibility to a builder pattern. Worth checking `DeckImageMapper` (out of scope) to see how it's currently coping.

### 1g. `mode = "system"` and similar primitive-string fields where an enum exists

- `Theme.java:32`: `private String mode = "system";` with comment `"light" | "dark" | "system"`. No enum; values are a string contract.
- `Tag.java`: `slug`-style ids are plain `String` (intentional — slugs).
- `Stroke.java:21`: `color` is `String` accepting hex OR a design-token name.

- Why it's an issue: `Theme.mode` is an obvious enum candidate (`ThemeMode { LIGHT, DARK, SYSTEM }`); typo-safe and self-documenting.
- Suggested direction: lift `Theme.mode` to a `ThemeMode` enum. Stroke `color` and Team `color` are arguably correct as strings (mixed conventions) but worth a validator that whitelists known token names.

---

## 2. Fields/structures that may belong on a different model

### 2a. Host display denorm duplicated between `InteractiveSession` and `ScheduledInteractiveSessionDTO`

- `InteractiveSession.java:76-77` carries `hostName`, `hostAvatarUrl` as denormalized fields (frozen on create).
- `ScheduledInteractiveSession.java` does NOT carry them — `ScheduledInteractiveSessionDTO.java:18,20` joins host name and deck name in at serialization time via `.of(s, hostName, deckName)`.

- Why it's an issue: two different strategies for the same problem. The scheduled-session path is arguably better (no stale denorm if the user renames themselves between schedule and play), but `InteractiveSession` froze for the lobby header.
- Suggested direction: decide one strategy. If you want frozen host display on the live session, also freeze it on `ScheduledInteractiveSession` so the schedule list view doesn't pay the join cost. If you don't, drop `InteractiveSession.hostName`/`hostAvatarUrl` and join at DTO time.

### 2b. `InteractiveSessionSettings.deckCoverImageUrl` / `deckBackgroundImageUrl` / `themeId` are deck concerns

`InteractiveSessionSettings.java:88-91` carries `deckCoverImageUrl`, `deckBackgroundImageUrl`, `themeId` — explicitly noted as "denormalised presentation assets copied from the source deck".

- Why it's an issue: these are deck-snapshot data, not host-configurable settings. They share a struct with `maxPlayers`, `timePerQuestion`, etc. The shape is also incoherent — every other image in the system uses the `Image` record (with variants), but these are bare URL strings.
- Suggested direction: move to a `DeckSnapshot` sub-record on `InteractiveSession` (alongside `deckSnapshot`), holding `Image cover`, `Image background`, `String themeId`, and `String deckName` (currently nowhere — see 2g). Keeps `InteractiveSessionSettings` to actual settings.

### 2c. Player chrome stats (`currentStreak`, `longestStreak`, `accuracy`, `reactionsSent`) duplicated between `InteractiveSessionPlayer` and `PlayerPlacement`

`InteractiveSessionPlayer.java:57-60` and `PlayerPlacement.java:28-30` both carry `longestStreak`, `accuracy`, `reactionsSent`. `InteractiveSessionPlayer` has `currentStreak`; `PlayerPlacement` doesn't (intentional — game over).

- Why it's an issue: `PlayerPlacement` is supposed to be a copy at game-end so the InteractiveSession document can be deleted later without losing the post-game card data. That's fine, but the model would be clearer if `PlayerPlacement` embedded a `PlayerEndStats` record rather than open-coding three fields.
- Suggested direction: extract a `PlayerEndStats(int longestStreak, double accuracy, int reactionsSent)` value type and reuse it in both, plus in `GameHistoryEntry.java:55-60` which also has these.

### 2d. `User.stats.currentStreak` is per-game but lives on a lifetime-stats object

`PlayerStats.java:14` carries `currentStreak`. `PlayerStats` is the lifetime stats holder on `User.java:49`. `InteractiveSessionPlayer.currentStreak` is the per-game value.

- Why it's an issue: a lifetime "current streak" makes no obvious sense in this model — there's no global "consecutive correct answers across all my sessions" use case mentioned. Worth confirming, but it looks like a copy-paste leftover.
- Suggested direction: confirm with usage; if no caller reads it, drop it.

### 2e. `GameHistoryEntry` has a near-complete copy of `PlayerPlacement` + extra

`GameHistoryEntry.java:52-66` repeats `finalScore, placement, totalQuestions, correctAnswers, longestStreak, accuracy, teamId, teamName` from `PlayerPlacement` plus `currentStreakAtEnd, reactionsSent, durationMs, wasHost, wasGuest, playedAt, deckName, hostName, hostUserId`.

- Why it's an issue: deliberate (denorm for the "my history" list), but the placement projection should be the same shape as `PlayerPlacement` plus history-only fields. Today they share no structure.
- Suggested direction: as in 2c, factor out `PlayerPlacement` into `PlayerEndStats + placement + identity` so `GameHistoryEntry` can compose it.

### 2f. `Deck.tags` (legacy) shadows `Deck.tagIds`

`Deck.java:46,52`: both `tagIds` (new canonical) and `tags` (legacy free-form) are persisted. Comment says `tags` is "re-populated from `tagIds` on every save."

- Why it's an issue: documented; one is computed; the dual-write pattern is risky.
- Suggested direction: finish the migration, drop the legacy `tags` field after a one-shot data fix.

### 2g. `InteractiveSession` has no `deckName`

`InteractiveSession.java` carries `deckId` + `deckSnapshot` but no `deckName`. The chat row, the game history, the schedule DTO, and the analytics rollup all denormalize names — but live session writes don't. Reviewers and the lobby header have to derive it from `deckSnapshot` or re-fetch the deck.

- Why it's an issue: minor inconsistency; the lobby has every other field denormed (hostName, hostAvatarUrl) but not the deck name.
- Suggested direction: add `deckName` (and possibly `deckThumbnail`) to `InteractiveSession` if it's read often; or move all denorm to `InteractiveSessionDTO` if you prefer DTO-time hydration.

### 2h. `Membership.monthlyInteractiveSessionCount` lives on `User.membership` but is gameplay state, not billing state

`Membership.java:66-76`: `monthlyInteractiveSessionCount` and `monthlyCountPeriodStart` track session hosting counts inside the embedded `Membership`. Comment is clear about intent (quota gates).

- Why it's an issue: `Membership` is "billing/subscription record". Mixing usage counters in muddies the model — Stripe webhooks write to `tier/status/period`, while session-finish writes to the count fields. Future "I have two paid memberships history rows" would lose the counter.
- Suggested direction: move usage counters into a separate `User.usage` embedded record (with monthly counters / quota stats), keep `Membership` to pure billing state.

### 2i. `User` ↔ `Organization`: ownership/seat fields are split

`Organization.java:18` carries `ownerId`. `User.java:60` carries `organizationIds`. `Membership.java:52` carries `sourceOrganizationId`. There's no explicit "user X is admin of org Y" model — owner is a single field on Organization.

- Why it's an issue: no membership table means you can't model "manager (not owner)" or "billing contact". The audit-trail of when a user joined an org is also lost (only org's `createdAt`).
- Suggested direction: out of scope for this audit but worth flagging — a `OrgMembership(userId, organizationId, role, joinedAt)` join doc would make this explicit. (Likely deferred to a chunk.)

---

## 3. Inline classes that could be standalone models

### 3a. Nested records inside DTO container classes

Three DTO files use the `public class Foo { public record FooResponse(…) { } public record CreateFooRequest(…) { } }` container pattern:

- `backend/src/main/java/cephadex/brainflex/dto/GalleryImageDTO.java` — wraps `GalleryImageResponse` + `UpdateGalleryImageRequest`
- `backend/src/main/java/cephadex/brainflex/dto/MediaAssetDTO.java` — wraps `MediaAssetResponse` + `UpdateMediaAssetRequest` + `CreateEmbedRequest`
- `backend/src/main/java/cephadex/brainflex/dto/OrganizationDTO.java` — wraps `OrganizationResponse` + `CreateOrganizationRequest` + `JoinOrganizationRequest`
- `backend/src/main/java/cephadex/brainflex/dto/ThemeDTO.java` — wraps `ThemeResponse` + `CreateThemeRequest` + `UpdateThemeRequest`

Every other DTO in the package is a top-level record. The four "container" classes are odd-ones-out.

- Why it's an issue: codegen produces `GalleryImageDTOGalleryImageResponse`-style names on the TS side. Inconsistent with the dozens of top-level DTO records.
- Suggested direction: flatten to top-level records: `GalleryImageResponse.java`, `UpdateGalleryImageRequest.java`, etc.

### 3b. `RoundResultMessage` nested records could be top-level

`RoundResultMessage.java` declares `PlayerRoundResult`, `BestAnswerOutcome`, `SubmissionTally` as nested. Same for `InteractiveSessionReviewDTO` (`RoundReview`, `PlayerRoundDetail`), `VotePhaseStartMessage` (`AnonymizedSubmission`), `TeamUpdateMessage` (`TeamMembership`), `SessionSummaryMessage` (`RoundSummary`), `DeckExploreRequest` (`Sort` enum).

- Why it's an issue: less critical than 3a because they're tightly coupled. But it leads to codegen names like `RoundResultMessagePlayerRoundResult` on the frontend that are awkward.
- Suggested direction: check generated client; if names are painful, lift to top-level.

### 3c. `Membership` and `OrganizationPlan` are 90% the same struct

`Membership.java` and `OrganizationPlan.java` both carry: `MembershipTier tier`, `MembershipStatus status`, `LocalDateTime startedAt`, `LocalDateTime currentPeriodEnd`, `Boolean cancelAtPeriodEnd`, `String stripeCustomerId`, `String stripeSubscriptionId`. `Membership` adds user-side fields (`sourceOrganizationId`, `monthlyInteractiveSessionCount`, `monthlyCountPeriodStart`); `OrganizationPlan` adds `seatLimit`.

- Why it's an issue: same Stripe handling code paths, same renewal logic, but two different types.
- Suggested direction: extract a `BillingState(tier, status, startedAt, currentPeriodEnd, cancelAtPeriodEnd, stripeCustomerId, stripeSubscriptionId)` value type embedded in both. Stripe webhook handler then writes to `BillingState` regardless of which scope it's targeting.

### 3d. `RoundVote` (embedded in player) vs `BestAnswerVote` (top-level collection)

`RoundVote.java` is embedded inside `InteractiveSessionPlayer.votes`; `BestAnswerVote.java` is a top-level `@Document(collection = "best_answer_votes")`. Both record the same information (`elementId`, `submissionId`, `votedAt` + voter identity).

- Why it's an issue: voting state lives in two places. Embedded `RoundVote` is unique-per-player; top-level `BestAnswerVote` is also "unique per (interactiveSession, element, voter)" per the comment. Either the embedded version is denormalized for read speed (then say so) or it's a duplicate.
- Suggested direction: pick one. If `RoundVote` is the source of truth, drop the collection (it's not indexed for hot reads anyway — only `interactiveSessionId` and `elementId` are indexed singly).

### 3e. Element-shared bag of "submission" identity (`submissionId`, `userId`, `userName`, `payload`)

This appears as:

- `RoundResultMessage.SubmissionTally` (submissionId, userId, userName, payload, voteCount)
- `VotePhaseStartMessage.AnonymizedSubmission` (submissionId, payload)
- `InteractiveSessionReviewDTO.PlayerRoundDetail` (userId, userName, payload, wasCorrect, pointsAwarded)
- `PlayerAnswer.submissionId` lives separately

These could share a base "submission identifier" type plus extras.

- Why it's an issue: minor inconsistency; mostly fine because each is a distinct broadcast shape.
- Suggested direction: low priority — keep separate unless the frontend wants a unified renderer.

---

## 4. Standardization gaps

### 4a. Mixed timestamp types: `LocalDateTime` vs `Instant`

- `EmailSuppression.java:45` uses `private Instant createdAt = Instant.now();`
- Every other document uses `LocalDateTime` (Deck, User, InteractiveSession, …).

- Why it's an issue: `LocalDateTime` is time-zone-blind and the wrong primitive for distributed event-stamps; `Instant` is correct. The codebase has settled on the wrong one almost everywhere — fixing it is a major migration, but at minimum, no new fields should be `LocalDateTime`.
- Suggested direction: long-term, migrate to `Instant`; short-term, at least don't add new `LocalDateTime` fields. Document the convention.

### 4b. ID conventions: slug vs UUID vs ObjectId

- Slugs: `Tag.id` (kebab-case), `Achievement.id` ("first-game" stable slug).
- ObjectId-as-String: every other `@Document` (`@Id String id`).
- Client-supplied UUIDs: `CreateDeckRequest.id`, `CreateDeckCollectionRequest.id` (optional, client-generated).

- Why it's an issue: no convention in the comments distinguishing them. A reader has to know per-document which scheme is used.
- Suggested direction: comment each `@Id` with the scheme (`// slug` / `// ObjectId hex` / `// client UUID`). Mildly useful.

### 4c. Collection types: `List` vs `Set`

- `User.roles` is `Set<UserRole>` (correct — semantic set).
- `DeckComment.upvoterUserIds` is `Set<String>` (correct).
- `InteractiveSession.revealedElementIds` is `Set<String>` (correct).
- `EmailSuppression.blocked` is `Set<EmailCategory>` (correct).
- `Deck.elements` is `List<DeckElement>` (correct — ordered).
- `GridQuestion.correctCellIndexes` is `Set<Integer>` (correct).
- `McqQuestion.correctOptionIds` is `List<String>` — **probably should be `Set`**, since uniqueness matters for scoring and there is no ordering.
- `RankingQuestion.correctOrder` is `List<String>` (correct).
- `Deck.tagIds` / `Deck.tags` are `List<String>` — **probably should be `Set`** unless duplicates have semantic meaning (they shouldn't).

- Why it's an issue: `correctOptionIds` is exactly the case where `Set` is correct.
- Suggested direction: change `McqQuestion.correctOptionIds` and `Deck.tagIds`/`Deck.tags` to `Set<String>` (or `LinkedHashSet` if order matters).

### 4d. `UserAchievementsPage` breaks the standard page envelope

`UserAchievementsPage.java:8-13` uses `items, earnedCount, totalCount` — no `page/size/totalElements/hasMore`. Every other `*Page` DTO has the standard envelope (see 1c).

- Why it's an issue: discusses the same surface (achievements list) but the client has to handle a special shape.
- Suggested direction: when 1c is implemented, decide whether the achievements list is paginated (then use the standard envelope plus `earnedCount/totalCount`), or whether the endpoint really returns everything (then call it `UserAchievementsResponse` not `Page`).

### 4e. DTO suffix is wildly inconsistent

- `DTO` suffix: `DeckDTO`, `UserDTO`, `OrganizationDTO`, `GalleryImageDTO`, `ThemeDTO`, `MediaAssetDTO`, `NotificationDTO`, `GameHistoryDTO`, `TagDTO`, `AchievementDTO`, `UserAchievementDTO`, `DeckCollectionDTO`, `DeckCommentDTO`, `DeckRatingDTO`, `DeckCollaboratorDTO`, `InteractiveSessionDTO`, `InteractiveSessionChatMessageDTO`, `InteractiveSessionInviteDTO`, `InteractiveSessionReviewDTO`, `ScheduledInteractiveSessionDTO`.
- `Response` suffix: `DeckExploreResponse`, `DeckFavoriteResponse`, `HealthCheckResponse`, `RedeemInviteResponse`. Plus inner records `GalleryImageResponse`, `MediaAssetResponse`, `OrganizationResponse`, `ThemeResponse`, `TagResponse`.
- `Request` suffix: `CreateDeckRequest`, `UpdateDeckRequest`, `RegisterRequest`, `JoinInteractiveSessionRequest`, … 28 total.
- `Message` suffix: `RoundResultMessage`, `RoundStartMessage`, `AnswerProgressMessage`, `VotePhaseStartMessage`, `VoteProgressMessage`, `InteractiveSessionEndedMessage`, `InteractiveSessionErrorMessage`, `ReactionBroadcastMessage`, `TeamUpdateMessage`, `WordCloudUpdateMessage`, `PresenceMessage`, `SessionSummaryMessage`, `ResponsesRevealedMessage` — all WebSocket broadcasts.
- `Page` suffix: see 1c (8 files).
- No suffix: `UnreadNotificationCount` (it's a response envelope).

- Why it's an issue: inconsistent, but mostly defensible — `Request` for inbound, `Message` for STOMP broadcasts, `DTO` / `Response` for read shapes. The mixed `DTO` vs `Response` for read shapes is the genuinely unprincipled gap.
- Suggested direction: pick one for read shapes. Easiest: rename `DeckExploreResponse` → `DeckExplorePage` (it is a paginated list), `HealthCheckResponse` keep, `RedeemInviteResponse` keep, and standardize all the `*DTO` names to a single convention (or leave them — but if you're going to leave, document the rule).

### 4f. Nullability conventions: `boolean` vs `Boolean`

- Models use primitive `boolean` (e.g. `Deck.system`, `InteractiveSessionPlayer.isGuest`, `Reaction.guest`). Mongo stores false/null indistinguishably for primitives.
- DTOs/Update requests use boxed `Boolean` to signal "unset/leave alone" (`CreateInteractiveSessionRequest.speedBonus`, `UpdateProfileRequest.newsletter`).
- `User.isGuest`, `User.newsletter`, `User.isClosed` are boxed `Boolean` on the *model* — different from every other model.

- Why it's an issue: `User`'s mix of boxed booleans is unusual. The rest of the codebase treats absent-bool as false; `User` treats it as null.
- Suggested direction: pick a convention. The codebase mostly uses primitive `boolean` on models and boxed on Update requests — `User.java:29,54,56` would benefit from converting to primitive (default false) unless null has meaning ("hasn't picked yet").

### 4g. Inconsistent `@Builder.Default` / field initialization

- Field-level initializers (`= new ArrayList<>()`, `= LocalDateTime.now()`, `= "en"`) are present on many fields. None use `@Builder.Default` because none use `@Builder` — but the question records (immutable records) carry no defaults; primitive fields default to 0 / false on construction.

`Slide.java:14-17` calls out this gotcha explicitly: "New boolean / int fields are declared as primitives, so every payload from the frontend must include defaults (Jackson cannot deserialize null into a primitive — see useCreateDashboard.buildNewElement)."

- Why it's an issue: the frontend has to mirror server defaults in TS. Any mismatch is a silent bug. The DeckElement defaults defined on the interface (`default int bestAnswerPoints() { return 50; }`) are unreachable when the record redeclares them as components (1a).
- Suggested direction: once 1a is addressed, the interface defaults become live; until then, document the default contract somewhere central (or add an `ElementDefaults` constants class).

### 4h. `Map<String, Object> styledTitle` / `participantInformation` are untyped opaque blobs

`Slide.java:33,71`, every question record, all use `Map<String, Object>` for the TipTap doc.

- Why it's an issue: travels to MongoDB and to the wire as an opaque blob. Codegen on the frontend gets `{ [k: string]: unknown }` and loses TipTap's type info entirely. Mongo also has no schema validation on these.
- Suggested direction: low priority — TipTap docs are deep, schema'd by the editor lib. Just document the contract. But: there's no reason BOTH `title` (string) and `styledTitle` (TipTap) need to ship — TipTap can render plain text. Worth checking whether `title` could be derived.

---

## 5. DTO / model alignment issues

### 5a. `DeckDTO` exposes `elements` containing un-redacted `DeckElement`s

`DeckDTO.java:42` includes `List<DeckElement> elements` — which carries correct-answer fields.

- Why it's an issue: documented for `InteractiveSessionDTO` (the comment notes lobby reads are fine but mid-round uses `RoundStartMessage` redaction); `DeckDTO`'s exposure is gated by visibility/ownership at the controller layer. Worth a comment.
- Suggested direction: add a comment noting that callers must already be authorized to see the full deck. Worth checking that `/api/decks/explore` (anonymous-accessible) does NOT return `elements` (preview-only).

### 5b. `DeckDTO` is missing `Deck` fields

`Deck.java` has fields not on `DeckDTO`: none significant — appears comprehensive. **But** `DeckDTO` is also missing `version` (the server-side `Deck.version` counter for optimistic concurrency). The frontend uses `Deck.version` to detect stale edits — this might be an oversight.

- Suggested direction: check whether the editor relies on `version`; if so, expose it.

### 5c. `InteractiveSessionDTO` flattens player fields but `players` is `List<InteractiveSessionPlayerDTO>` (correct), yet `teams` is `List<Team>` (model type)

`InteractiveSessionDTO.java:47` ships raw `Team` records on the wire (which carry `captainUserId` and `memberCount` — internal denorm). There's no `TeamDTO`. `InteractiveSessionPlayerDTO` exists; `TeamDTO` does not.

- Why it's an issue: inconsistent — every other embedded thing has a DTO wrapper. `Team` is small so it's not a big leak, but `captainUserId` is internal state.
- Suggested direction: either add `TeamDTO` or document that `Team` is the wire type.

### 5d. `UserDTO.RegisteredUser` exposes `Membership` raw — including Stripe customer IDs

`UserDTO.java:62`: ships the full `Membership` record on `/me`, which includes `stripeCustomerId`, `stripeSubscriptionId`, `monthlyInteractiveSessionCount`, `monthlyCountPeriodStart`.

- Why it's an issue: `me` is fine (it's the user's own data), but if `RegisteredUser` is ever returned for another user (admin endpoints, public profile), Stripe IDs leak.
- Suggested direction: introduce a `MembershipDTO(tier, status, startedAt, currentPeriodEnd, cancelAtPeriodEnd)` for wire, drop Stripe fields and counters.

### 5e. `UserDTO.RegisteredUser` exposes `googleId` but not `discordId` / `microsoftId`

`UserDTO.java:57`: only `googleId` is on the DTO. `User.java:31-38` also has `discordId` and `microsoftId`.

- Why it's an issue: oversight; the wire DTO predates the multi-provider work.
- Suggested direction: add `discordId` and `microsoftId` to the DTO (or replace with a single `externalIdentity` / `provider` pair).

### 5f. `User.roles` not on `UserDTO`

`User.roles` (Set<UserRole>) doesn't appear on `RegisteredUser`. Frontend may need it for moderator/admin gating in the UI.

- Why it's an issue: possibly intentional (authorities come over via Spring Security session), but worth confirming.

### 5g. `DeckCollaboratorDTO.userName` and `name` are both present

`DeckCollaboratorDTO.java:22-23`: separate `userName` and `name` fields, both populated from `User` (`getUserName()` and `getName()`). No other DTO does this.

- Why it's an issue: the difference between `userName` (handle) and `name` (display name) is real, but every other DTO collapses to one. Possibly intentional for the share modal — worth checking whether the frontend uses both.

### 5h. `CreateInteractiveSessionRequest` fields are not a subset of `InteractiveSessionSettings`

`CreateInteractiveSessionRequest.java` enumerates settings inline (`totalRounds`, `timePerQuestion`, `speedBonus`, …) using boxed types. `InteractiveSessionSettings.java` carries them as primitives. The request DTO is missing `teamCount` validation that matches the settings field (`teamCount` is `@Min(2) @Max(8)` in the request — settings has it as `int teamCount = 2`).

- Why it's an issue: any new settings field has to be added in two places, with the validation kept in sync. The request also omits some settings fields entirely: `requireFullName`, `spectatorsAllowed`, `lobbyMusicAssetId`, `allowReJoin` — the request has the first two but not the latter two.
- Suggested direction: have `CreateInteractiveSessionRequest` carry an `InteractiveSessionSettings settings` directly (with validations on the embedded type) plus `deckId` and `customRoomCode`. Cuts ~25 lines of duplication.

### 5i. `ScheduledInteractiveSessionDTO` carries `InteractiveSessionSettings` raw but `InteractiveSessionDTO` does too

Both ship `InteractiveSessionSettings` directly. That's internally consistent, but `InteractiveSessionSettings` is going to carry the `deckCoverImageUrl/deckBackgroundImageUrl/themeId` (from 2b) on the schedule DTO too — those fields haven't been frozen at schedule time, so they'll be null/stale on schedule reads.

- Suggested direction: separate wire vs model `Settings` if 2b is implemented; or live with the null fields.

### 5j. `Deck.viewCount` is on the model, increments are documented as `$inc`, but no DTO field maps to "did I view this?"

`Deck.viewCount` and `Deck.playCount` are counters. No caller-specific "have I viewed this" — only `isFavorited` / `myRating`. Probably intentional (no per-user view tracking), but the asymmetry is worth noting.

---

## 6. Sealed hierarchy inconsistencies

### 6a. Element records do not consistently include per-kind ergonomics in the same section

Some records group per-kind fields under `// per-kind ergonomics (chunk 10)` (e.g. `McqQuestion.shuffleOptions, allowMultipleSelect, maxSelections`; `TextQuestion.maxLength, trimWhitespace, fuzzyMatch, fuzzyDistance`; `NumberQuestion.minValue, maxValue, allowNegative`). Others (`Allocation`, `Drawing`, `WordCloud`, `Matching`, `Grid`, `Scales`, `Place`, `Ranking`) have their per-kind fields mixed with the prompt block.

- Why it's an issue: no consistent layout; reading the file requires scanning to find what's kind-specific vs shared.
- Suggested direction: enforce field grouping comments consistently. Easy mechanical fix.

### 6b. `Slide` is the only `DeckElement` with `resultsDisplayType`, `multipleSelectionsEnabled`, etc.

`Slide.java:62-71`: declares `resultsDisplayType`, `multipleSelectionsEnabled`, `selectionsPerParticipant`, `showResultsAsPercentage`, `joinType`, `showJoinInformation`, `showQrCode`, `heading`, `participantInformation`. Comment at `Slide.java:11-13` explicitly says "shared with the rest of the deck-element family conceptually but only declared here for now; other kinds will inherit the same controls in a follow-up".

- Why it's an issue: this is the chunk-1a refactor in disguise — the controls clearly belong on every kind. Today only Slides have them, which means survey/MCQ/etc. can't override the display type.
- Suggested direction: lift these to interface defaults on `DeckElement` (returning sensible defaults), then declare them as record components on every record that needs an override. Bundle with 1a.

### 6c. `Slide` carries a legacy `body` field plus the new `blocks` list — both present

`Slide.java:42-46`: dual-write. `effectiveBlocks()` papers over it. SlideBlocksMigrationRunner is referenced.

- Why it's an issue: documented in-flight migration. Until it lands, every Slide carries two representations.
- Suggested direction: finish the migration, remove `body`.

### 6d. `JoinType` is on Slide as "legacy — read-only" but the enum is still in active use

`Slide.java:66`: `JoinType joinType, // legacy — read-only; new writes use showQrCode + showJoinInformation`. The enum lives in `model/enums/JoinType.java` and is exposed on the type.

- Why it's an issue: the comment says legacy but the field is still in the record's signature, so clients still send/receive it.
- Suggested direction: remove the field after a migration to derive `joinType` from `showQrCode`/`showJoinInformation` at read time (or drop the field outright if no caller still uses it).

### 6e. `AnswerPayload` interface declares no methods at all

`AnswerPayload.java:32-36`: sealed interface with no methods. Every variant just `implements AnswerPayload`. `DeckElement` (by contrast) declares ~25 methods.

- Why it's an issue: the lack of methods means scorers have to instanceof-switch on the payload, no shared interface like `String elementId()` or `Optional<…>`. Probably fine — payloads ARE the polymorphic data — but worth noting that this is intentional asymmetry.
- Suggested direction: leave as-is; document that AnswerPayload is intentionally data-only.

### 6f. `AnswerPayload` has no `kind()` method, but the `@JsonTypeInfo` discriminator is `"kind"`

`AnswerPayload.java:14-17`: serialized as `kind`. None of the records expose a `kind()`. Discriminator is driven only by Jackson's class registry. `DeckElement` has both (`ElementKind kind()` accessor AND `@JsonTypeInfo`).

- Why it's an issue: asymmetric with `DeckElement`. No way to ask "what kind of payload is this?" at runtime without instanceof.
- Suggested direction: add `AnswerKind kind()` (a new enum mirroring ElementKind variants), or — minimally — add a default `String typeName()` for runtime introspection.

### 6g. `SlideBlock` family fields are inconsistent across blocks

- `BodyBlock(id, richBody)` — uses `richBody` (rich text)
- `CalloutBlock(id, richBody, tone)` — also `richBody`
- `HeadingBlock(id, text, Integer level)` — uses `text` (plain) plus `level`
- `BulletListBlock(id, items)` — `List<String> items` (plain)
- `ImageBlock(id, image, caption)` — `Image` reference + plain caption

- Why it's an issue: mixed "rich text body" (`richBody`) vs "plain text" (`text`, `items`, `caption`) with no documented rule. Adding a new block type means picking a convention with no guidance.
- Suggested direction: doc the rule. If the editor's plain-text inputs become rich-text later, plan the migration.

### 6h. `ElementKind` and `AnswerPayload` discriminators diverge

`ElementKind` has 13 values (incl. SLIDE). `AnswerPayload` has 12 variants (no SlideAnswer — correct, slides have no answer) plus `TimeoutAnswer` (a sentinel). The pairing is implicit: scorer code maps ElementKind → AnswerPayload subclass.

- Why it's an issue: no compile-time guarantee the answer matches the question kind. Bad mapping would silently fail at runtime.
- Suggested direction: consider a parameterized scorer interface keyed by ElementKind; out of scope for this audit but worth flagging.

---

## 7. WebSocket message DTOs — envelope inconsistency

### 7a. No common base type / envelope

The 13 `*Message` classes share no base record:

- `RoundStartMessage(round, totalRounds, element, startedAt)`
- `RoundResultMessage(round, format, element, playerResults, bestAnswer)`
- `VotePhaseStartMessage(round, element, submissions, timePerVote, phaseStartedAt)`
- `VoteProgressMessage(round, votedUserIds, totalPlayers)`
- `AnswerProgressMessage(round, answeredUserIds, totalPlayers)`
- `ReactionBroadcastMessage(id, elementId, userId, userName, guest, emoji, offsetMs, sentAt)`
- `ResponsesRevealedMessage(round, elementId)`
- `WordCloudUpdateMessage(round, elementId, counts)`
- `SessionSummaryMessage(roundsPlayed, anyScoringEnabled, rounds)`
- `InteractiveSessionEndedMessage(placements)`
- `InteractiveSessionErrorMessage(operation, roomCode, status, message)`
- `TeamUpdateMessage(teams, memberships)`
- `PresenceMessage(userId, online)`

- Why it's an issue: client-side reconciliation logic can't generically extract `round` / `roomCode` / `timestamp`. Of these 13, only ~7 carry `round`, only 2 carry a timestamp, only 1 carries `roomCode` (`InteractiveSessionErrorMessage`).
- Suggested direction: introduce a `BroadcastEnvelope<T>(String roomCode, int round, long sentAtEpochMs, T payload)` and wrap. Or at minimum, mandate `round` + `sentAt` on every gameplay broadcast.

### 7b. `RoundResultMessage` is the only broadcast with `format`

`RoundResultMessage.java:25` carries `SessionFormat format`. Every other broadcast omits it.

- Why it's an issue: comment says "frozen for the duration of the session" — if so, the client gets it once from `InteractiveSessionDTO` and shouldn't need it on every round-end. Possibly defensive duplication.
- Suggested direction: confirm whether the client truly needs format on every round-end; if not, drop and read from session DTO.

### 7c. Inconsistent timestamp on broadcasts

- `RoundStartMessage` has `startedAt` (LocalDateTime).
- `VotePhaseStartMessage` has `phaseStartedAt`.
- `ReactionBroadcastMessage` has `sentAt` + `offsetMs`.
- `RoundResultMessage`, `AnswerProgressMessage`, `VoteProgressMessage`, `WordCloudUpdateMessage`, `SessionSummaryMessage`, `InteractiveSessionEndedMessage`, `ResponsesRevealedMessage`, `TeamUpdateMessage`, `PresenceMessage` carry no timestamp.

- Why it's an issue: clients can't reorder out-of-order messages or measure latency without timestamps.
- Suggested direction: add `Instant sentAt` to every broadcast (or accept via envelope; see 7a).

### 7d. `InteractiveSessionEndedMessage` carries placements but `SessionSummaryMessage` doesn't

`InteractiveSessionEndedMessage.java:12` is `List<PlayerPlacement>` only. `SessionSummaryMessage.java:21` is `roundsPlayed + anyScoringEnabled + rounds`. They're explicitly mutually exclusive (one per format), and the GAME path also writes `InteractiveSessionResult` to the DB.

- Why it's an issue: minor — they're meant to be different shapes. But the contrast means clients have to handle two end-of-session paths.
- Suggested direction: fine as-is; documented.

### 7e. `PresenceMessage` doesn't carry `roomCode` even though it's broadcast on `/topic/presence`

`PresenceMessage.java` comment says it's broadcast on a global `/topic/presence` channel. No `roomCode` — so clients listening from any session see every presence event.

- Why it's an issue: privacy + bandwidth. A spectator in session A sees presence updates for players in session B.
- Suggested direction: scope per-session — `/topic/interactive-session/{roomCode}/presence` and include the room code (or drop the field but scope the topic).

---

## 8. Enum sprawl

### 8a. 32 enum types — but most are tightly scoped and justified

I found no redundant enums per se. Many are tiny (2-3 values) but each is used in distinct contexts: `BestAnswerScoring`, `RankingScoring`, `MatchingScoring`, `PlaceScoring` are all 2-value scoring strategies. They could be merged into one generic `ScoringStrategy { EXACT, PARTIAL, BINARY, LINEAR, ALL_OR_NOTHING, POINTS_PER_VOTE, FLAT_WINNER }`, but that loses kind-coupling.

- Why it's an issue: more of an observation. The 4 separate scoring enums are arguably fine for type-safety.
- Suggested direction: leave alone unless the per-kind scoring duplicates code. Watch for a 5th kind requiring `ALL_OR_NOTHING / PARTIAL` — you'd add a 5th enum.

### 8b. `ResultsDisplayType.HISTOGRAM` is marked `@Deprecated`

`ResultsDisplayType.java:23-25`: deprecated alias for `BAR_VERTICAL`.

- Why it's an issue: dead value present for back-compat. Comment says "read-only for legacy documents".
- Suggested direction: write a one-shot migration that rewrites `HISTOGRAM` → `BAR_VERTICAL` on every Slide, then drop the value.

### 8c. `NotificationKind` has four "reserved — future" values

`NotificationKind.java:31-37`: `ORG_INVITE`, `ORG_JOINED`, `MENTION` (and `SYSTEM` is used). Comment notes they're reserved.

- Why it's an issue: dead enum values are usually fine in NotificationKind because the comment is clear, but the frontend codegen exports them as if they were real cases. Switch statements have to handle them.
- Suggested direction: leave; or move to a separate `FutureNotificationKind` so the active enum is tighter.

### 8d. `SlideKind` is declared but `Slide` records take it as a parameter — frontend coverage uncertain

`SlideKind.java`: `TITLE, SECTION, CALLOUT, CONTENT, END`. `Slide.java:32` has `SlideKind slideKind` as a component.

- Why it's an issue: `SlideKind.CALLOUT` overlaps semantically with `CalloutBlock` — a Content slide could contain a CalloutBlock, but there's also a `SlideKind.CALLOUT` for the whole slide. Possibly intentional (slide-level vs block-level callout).
- Suggested direction: confirm whether `SlideKind.CALLOUT` is still needed once blocks land. Likely redundant with `Slide` containing a `CalloutBlock`.

### 8e. `EmailSuppression.Reason` is an inner enum of a model class — alone in the codebase

`EmailSuppression.java:33`: `public enum Reason { UNSUBSCRIBE, BOUNCE, COMPLAINT, MANUAL }` declared inside the document class.

- Why it's an issue: every other enum lives in `model/enums/`. Spring Mongo will serialize it fine, but it's the only inner enum.
- Suggested direction: move to `model/enums/EmailSuppressionReason.java` for consistency.

### 8f. `DeckExploreRequest.Sort` is also an inner enum

`DeckExploreRequest.java:22-37` defines a nested enum with a `parse` method. Single-purpose inner enum is defensible (only used by this DTO), but inconsistent with the convention.

- Suggested direction: leave as-is unless used elsewhere.

### 8g. `SessionFormat` carries a `@JsonCreator` with hardcoded "PULSE" alias

`SessionFormat.java:33-47`: explicit migration mapping in the enum itself.

- Why it's an issue: documented; correct pattern for value migrations. Worth a one-shot DB rewrite at some point so the alias can be removed.

---

## 9. Other findings

### 9a. `User.isClosed` / `closedAt` exist but no service / DTO references them in this scan

`User.java:56-57`: account-closure flag. No `UserDTO` exposure (correct), no other obvious wiring in models/DTOs. Probably handled in services/controllers.

- Suggested direction: confirm wiring; if dead, drop.

### 9b. `User.pictureUrl` (string) vs `User.pictureVariants` (list) dual representation

`User.java:42-47`: comment says external `pictureUrl` set by OAuth, `pictureVariants` set by user upload. Hydration walks variants first then falls back to `pictureUrl`. `UserDTO` exposes both `pictureUrl` (largest) and `picture` (Image record).

- Why it's an issue: not really an issue — documented. Just noting the dual representation as part of the wider Image-handling story.

### 9c. `Theme` does not have `updatedAt`

`Theme.java:42`: only `createdAt`. Every other top-level user-editable doc has `updatedAt`.

- Suggested direction: add `updatedAt`.

### 9d. `Organization` does not have `updatedAt` either

`Organization.java`: only `createdAt`.

- Suggested direction: add `updatedAt`.

### 9e. `Membership` and `OrganizationPlan` have no audit fields at all

Embedded value types — they're updated whenever the parent doc is saved. Could carry a `lastBillingEventAt` for debugging webhook ingestion.

### 9f. `Reaction.offsetMs` and `ReactionBroadcastMessage.offsetMs` are `long`, but every other duration is `int`

`Reaction.java:51` and `ReactionBroadcastMessage.java:17`: `long offsetMs`. `PlayerAnswer.timeTakenMs` is also `long`. `Notification.iconUrl` etc. don't matter. But `InteractiveSessionSettings.timePerQuestion` is `int` (seconds), `GameHistoryEntry.durationMs` is `long`, `ElementStats.totalTimeMs` is `long`, `ElementStats.averageTimeMs` is `double`.

- Why it's an issue: inconsistent type for "millisecond" fields. `int` overflows at ~24 days; `long` is safer. Mix is harmless but unprincipled.
- Suggested direction: standardize on `long` for any `*Ms` field, `int` for `*Seconds`.

### 9g. `ImageVariant` width/height are `int` (with `0 = unknown`); `MediaAsset.width/height` are `Integer` (boxed, null = unknown)

Inconsistent unknown-marker for the same semantic.

- Suggested direction: pick one. `Integer` with null is more honest.

### 9h. Many embedded value objects lack `equals/hashCode` consideration

`InteractiveSessionPlayer`, `PlayerAnswer`, `PlayerPlacement`, `Team`, `RoundVote` are `@Data` Lombok classes — Lombok generates equals/hashCode by default for `@Data`. Records get value-equality for free. This is fine. **But** Mongo serialization may upcast certain collection types (`HashSet<UserRole>` ↔ `EnumSet<UserRole>` on User) and `EnumSet.equals(HashSet)` works but the hashCode contract is preserved only because both are `Set`. Probably fine — flagging just in case.

### 9i. `InteractiveSession.revealedElementIds` uses fully-qualified types

`InteractiveSession.java:97-99`: `private java.util.Set<String> revealedElementIds = new java.util.HashSet<>();`.

- Why it's an issue: missing imports — looks like a hurry. Inconsistent with the rest of the file.
- Suggested direction: add proper imports.

### 9j. `InteractiveSessionSettings.java:19` carries a `//TODO` admitting Deck/Session boundary is unclear

`//TODO: We don't have a clear delimitation between Deck and InteractiveSession, a interactiveSession uses a deck but the delimitation is blurry`.

- Why it's an issue: in-source admission of the issue 2b is about.
- Suggested direction: resolve as part of 2b.

### 9k. `DeckCollaborator.acceptedAt` semantics overloaded

Comment on `DeckCollaborator.java:51`: "Auto-set to invitedAt when a known user is added." So `acceptedAt = invitedAt` doesn't actually mean accepted — it means "auto-accepted because the invitee was already a user". A future "real" acceptedAt event would overwrite. The field has two meanings depending on context.

- Suggested direction: distinguish: keep `invitedAt`, add a separate `autoAccepted: boolean` so `acceptedAt` is only set by the user.

### 9l. Compound index name collision: two collections use `deck_user_unique_idx`

- `DeckRating.java:26` declares `@CompoundIndex(name = "deck_user_unique_idx", def = "{'deckId': 1, 'userId': 1}", unique = true)`.
- `DeckCollaborator.java:30` declares `@CompoundIndex(name = "deck_user_unique_idx", def = "{'deckId': 1, 'userId': 1}", unique = true)`.

Same name in different collections is fine (index names are scoped per-collection in Mongo), but it makes log lines and `db.collection.getIndexes()` output ambiguous.

- Suggested direction: rename to `deck_rating_user_unique_idx` and `deck_collaborator_user_unique_idx`.

### 9m. `Deck.subjectTagId` vs `Deck.tagIds` — the subject ID may not be in `tagIds`

`Deck.java:46-50`: `subjectTagId` is a single primary tag. No comment confirms whether it's always also in `tagIds`. If it isn't, you have two sources of truth for which tags a deck has.

- Suggested direction: clarify the contract — either guarantee subjectTagId ∈ tagIds (then subjectTagId is just a pointer) or document the divergence.

### 9n. `Slide` and other elements have `image` (component) AND `background` (component) AND optional `videoAssetId` / `audioAssetId` — but no `imageAssetId`

`Slide.java`, every question record: a `videoAssetId` field points at a `MediaAsset` (chunk 19 work), but `image` is still an `Image` record. There's no `imageAssetId`. Suggests inconsistent media-asset migration: video/audio are MediaAsset-aware, images are Image-aware.

- Suggested direction: long-term, unify under `MediaAsset` so every media reference is `MediaAssetReference`. Out of scope.

### 9o. `BodyBlock.richBody` is `String` but TipTap docs elsewhere are `Map<String, Object>`

`BodyBlock.java:11`: `String richBody`. But `Slide.styledTitle` is `Map<String, Object>` (TipTap JSON doc). The comment on BodyBlock says "TipTap/ProseMirror HTML" — so BodyBlock stores HTML (serialized) while styledTitle stores the doc model. Same for `CalloutBlock.richBody`.

- Why it's an issue: two ways to store rich text in the same document. HTML can't be round-tripped through TipTap losslessly (lossy serialization). Worth confirming whether the editor really wants HTML for BodyBlock and not the structured doc.
- Suggested direction: pick one. Structured doc (`Map<String,Object>`) is harder to render server-side but lossless; HTML is the inverse.

---

## Priority shortlist (top ~8 by impact-to-effort)

Ordered roughly by ratio of pain reduction to implementation cost.

1. **Consolidate `*Page` envelopes into a generic `Page<T>` (#1c).** Mechanical, removes 7 near-identical DTOs, simplifies the frontend.
2. **Fix `McqQuestion.correctOptionIds` to `Set<String>` (and `Deck.tagIds` → `Set`) (#4c).** Trivial type fix; matches actual semantics.
3. **Resolve `bestAnswerBonus` Mongo field rename (#1b).** One migration + 11 annotation removals; finishes a half-done rename and prevents future records from forgetting the alias.
4. ~~**Extract a `UserSnapshot` value type and reuse on every "denormalized author/actor" field (#1e).**~~ ✅ Done — see 1e above.
5. **Flatten the four "container class with nested records" DTOs (#3a).** Mechanical rename, removes a TS codegen wart.
6. **Add a `BroadcastEnvelope` (roomCode + round + sentAt) to all `*Message` types (#7a, #7c, #7e).** Resolves multiple WebSocket issues at once. Also fixes the `PresenceMessage` scoping leak.
7. **Lift the `~30 shared chrome fields` off the 12 question records into either an embedded `ElementChrome` record or interface defaults (#1a + #6b).** Highest pain reduction long-term — every new chrome field today requires 12 file edits — but biggest implementation cost. Bundle with cleaning up `Slide`'s "should be shared" controls (#6b).
8. **Extract a `BillingState` value type shared by `Membership` and `OrganizationPlan` (#3c) and move usage counters off `Membership` (#2h).** Improves the billing surface ahead of the Stripe integration.

Lesser but easy wins worth grabbing alongside:

- Standardize on `long` for `*Ms` fields (#9f).
- Rename `Theme.mode` to a `ThemeMode` enum (#1g).
- Add missing `updatedAt` to `Theme` and `Organization` (#9c, #9d).
- Rename one of the colliding `deck_user_unique_idx` compound indexes (#9l).
- Clean up `InteractiveSession.java:97-99` fully-qualified type usage (#9i).
