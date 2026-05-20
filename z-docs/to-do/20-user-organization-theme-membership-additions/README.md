# 20 — User / Organization / Theme / Membership additions

**Status:** In progress (UserRole + admin gating slice landed)
**Depends on:** Nothing strict; field additions stand alone
**Unblocks:** UX polish, real role-gating, plan tiers

## Scope

Catch-all for the field additions on the four foundational models that didn't justify their own chunk. Mostly profile polish, org branding, theme expressiveness, and plan-tier feature flagging. This chunk can be split inline into smaller PRs as you do other chunks — there's no single migration.

## User additions

```text
User (additions only)
  String displayName                   // distinct from name (legal) and userName (handle); shown in-game
  String customAvatarUrl               // own upload; falls back to pictureUrl (Google)
  String bio                           // markdown, max 500 chars
  String location, websiteUrl
  String locale                        // BCP-47 (default "en")
  String timezone                      // IANA (e.g. "America/Los_Angeles")
  Set<UserRole> roles                  // USER, MODERATOR, ADMIN
  Set<String> tagInterests             // tagIds from chunk 01; used for Explore personalization
  NotificationPrefs notificationPrefs  // embedded
  LocalDateTime emailVerifiedAt
```

```text
NotificationPrefs (embedded record)
  Map<NotificationKind, Boolean> inApp      // default true for all
  Map<NotificationKind, Boolean> email      // default false except INTERACTIVE_SESSION_INVITE, COLLAB_INVITE, ORG_INVITE
  boolean weeklyDigestEmail                 // default true
  boolean marketingEmail                    // default = User.newsletter
```

```text
UserRole (enum)
  USER, MODERATOR, ADMIN
```

Note: `displayName` is the new authoritative display field. Migrate existing display sites to use `displayName ?: userName ?: name`.

## PlayerStats additions (embedded in User)

```text
PlayerStats (additions only)
  int longestStreak                    // best streak in any single game
  int perfectGames                     // count of games with accuracy = 1.0
  int totalReactionsSent
  Map<String, Integer> presentedByKind  // ElementKind name -> count
  Map<String, Integer> correctByKind
  int weeklyPoints, monthlyPoints      // reset by a Spring @Scheduled cron
  LocalDateTime weeklyPointsResetAt, monthlyPointsResetAt
  LocalDateTime lastPlayedAt
```

A weekly cron at 00:00 UTC Monday resets `weeklyPoints` to 0; monthly cron on the 1st resets `monthlyPoints`. These power the "weekly leaderboard" view in a future feature.

## Organization additions

```text
Organization (additions only)
  String description                   // markdown, max 1000 chars
  Image logoImage                      // separate from Theme.logo
  String websiteUrl, location
  String emailDomain                   // e.g. "stanford.edu" — auto-join on Google login
  String inviteCode                    // join without explicit ID share; rotatable
  boolean allowPublicJoin              // default false; if true, anyone with the inviteCode can join
  int memberCount                      // denorm
  String defaultThemeId                // theme applied to new members' InteractiveSessions unless overridden
  LocalDateTime updatedAt
```

Endpoint additions:

- `PUT /api/organizations/{id}` — update profile fields (owner only)
- `POST /api/organizations/{id}/invite-code/rotate` — regenerate the join code
- `POST /api/organizations/join-by-code` — body `{ inviteCode }`

OAuth flow: after Google sign-in, if the user's email domain matches an `emailDomain`, auto-add to that org (idempotent).

## Theme additions

```text
Theme (additions only)
  String fontFamily                    // body font; default null = inherit
  String headingFontFamily             // default null = inherit
  String soundThemeId                  // MediaAsset id (kind=AUDIO) for answer-correct / answer-wrong SFX bundle
  Map<String, String> tokenOverrides   // power-user CSS variable overrides; e.g. {"--radius-md": "0.5rem"}
```

Frontend `useTheme.ts` reads `tokenOverrides` and writes them onto `:root` as inline CSS variables (after the design-system defaults).

## Membership / OrganizationPlan additions

```text
Membership / OrganizationPlan (additions only)
  Set<String> featureFlags             // "reactions", "team-mode", "analytics-pro", "ai-generation", ...
  int monthlyInteractiveSessionCount             // bumped when a InteractiveSession finishes
  int monthlyInteractiveSessionLimit             // 0 = unlimited; soft quota
  LocalDateTime quotaResetsAt          // first of next month UTC
```

`MembershipService.canStartInteractiveSession(userId)` returns false if `monthlyInteractiveSessionCount >= monthlyInteractiveSessionLimit > 0`. Surface the quota in the UI ("3 / 10 games this month — upgrade for unlimited").

## GalleryImage additions (if not migrated to MediaAsset in chunk 19)

```text
GalleryImage (additions only)
  Integer width, height
  String altText, attribution, sourceUrl
  String mimeType, originalFileName
  long sizeBytes
```

If chunk 19 lands first, skip — `MediaAsset` already has all of these.

## BestAnswerVote addition

```text
BestAnswerVote (additions only)
  int weight                           // default 1; host vote = 2x in some game modes
```

## AudienceSubmission additions

```text
AudienceSubmission (additions only)
  int downvotes                        // currently only upvotes
  String moderatedByUserId
  LocalDateTime moderatedAt
  String moderationReason              // free-text, surfaced to moderators only
```

## Cross-cutting concerns

- **Roles** — once `UserRole` lands, every admin endpoint (tag CRUD, achievement seeding, system deck flagging) gates on `roles.contains(ADMIN)`. Add `@PreAuthorize("hasRole('ADMIN')")` on the controller methods.
- **Locale + timezone** — display dates in the user's TZ; default to browser TZ on first login.
- **Migration** — set `roles = {USER}` for every existing user. Set `notificationPrefs` to defaults. Set `displayName = name` if blank.

## Checklist

- [x] `UserRole` enum + `User.roles` field + `ROLE_ADMIN` / `ROLE_MODERATOR` authorities + role-backed `AdminProperties` (env-var allowlist removed) + `UserRoleBackfillMigration` (`scripts/migrate-user-roles.sh`)
- [ ] Remaining `User` profile additions: `displayName`, `customAvatarUrl`, `bio`, `location`, `websiteUrl`, `locale`, `timezone`, `tagInterests`, `emailVerifiedAt`
- [ ] `NotificationPrefs` embedded record + endpoints
- [ ] Migrate remaining `adminProperties.isAdmin(caller)` *forbid-or-allow* sites to `@PreAuthorize("hasRole('ADMIN')")` (the inline-branching sites like `TagController.createTag`'s curated-flag shaping stay on the helper)
- [ ] `PlayerStats` extensions + weekly/monthly reset cron
- [ ] `Organization` additions + endpoints + email-domain auto-join
- [ ] `Theme` additions + frontend token override application
- [ ] `Membership` / `OrganizationPlan` feature flags + quota
- [ ] `MembershipService.canStartInteractiveSession` + UI quota surface
- [ ] `GalleryImage` additions (if not migrated to `MediaAsset`)
- [ ] `BestAnswerVote.weight`
- [ ] `AudienceSubmission` moderation fields
- [ ] Frontend profile page expansions (display name, bio, locale, avatar upload)
- [ ] Org settings page (description, logo, invite code rotation)
- [ ] Notification preferences page
- [ ] Frontend codegen + lint
- [ ] Backend tests pass
