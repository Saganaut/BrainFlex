# 14 — Scheduled showcases & invites

**Status:** Not started
**Depends on:** Nothing strict; chunk 18 (notifications) integrates nicely
**Unblocks:** Future "recurring class" features

## Scope

Two complementary features:

1. **Scheduled showcases** — host picks a future date/time; the system boots a Showcase at that time and notifies invitees.
2. **Invites** — host invites specific people by email; they get a single-use token to join.

This adds the first dependency on outbound email. Use a thin abstraction (`EmailService` interface) so the implementation can be SMTP today and SendGrid/Postmark later.

## New models

```text
ScheduledShowcase                      @Document("scheduled_showcases")
  @Id String id
  String hostUserId, deckId
  ShowcaseSettings settings            // copied to the live Showcase at boot time
  LocalDateTime scheduledStartAt
  LocalDateTime scheduledEndAt         // estimate — for calendar exports
  String reminderEmailTemplate         // nullable — host-customized reminder copy
  List<String> invitedEmails           // raw emails; resolved into ShowcaseInvite rows on create
  String createdShowcaseId             // null until the show boots
  ScheduleStatus status                // SCHEDULED | LIVE | COMPLETED | CANCELLED
  LocalDateTime createdAt, updatedAt
```

```text
ShowcaseInvite                         @Document("showcase_invites")
  @Id String id
  @Indexed String showcaseId           // null until parent ScheduledShowcase boots
  @Indexed String scheduledShowcaseId  // points back at the parent if scheduled
  String email                         // case-normalized
  String invitedByUserId
  @Indexed(unique=true) String inviteToken  // single-use, ~24 bytes base64url
  String resolvedUserId                // populated when the invitee logs in / accepts
  LocalDateTime sentAt
  LocalDateTime redeemedAt             // when the invitee actually joined
  LocalDateTime expiresAt              // default = scheduledStartAt + 2h
```

```text
ScheduleStatus (enum)
  SCHEDULED, LIVE, COMPLETED, CANCELLED
```

Indexes:

- `ScheduledShowcase`: `(status, scheduledStartAt)` — for the cron sweep
- `ShowcaseInvite`: `(email, scheduledShowcaseId)` to dedupe

## Backend changes

- New `EmailService` interface + `SmtpEmailService` impl using Spring Boot's `JavaMailSender` (already on the classpath via Spring starters). Inject SMTP credentials via env vars; ship sane defaults.
- New `ScheduledShowcaseService`:
  - `schedule(hostUserId, deckId, scheduledStartAt, settings, invitedEmails)` — create rows + send initial invite emails
  - `cancel(scheduledShowcaseId, hostUserId)` — sets `status = CANCELLED`, voids invites
  - `boot(scheduledShowcaseId)` — creates the live Showcase, copies settings, sets `createdShowcaseId`, transitions to `LIVE`, sends reminder emails with the join link
  - `complete(scheduledShowcaseId)` — called when the live Showcase transitions to `FINISHED`
- Spring `@Scheduled` cron sweep (every 30s): pick up `ScheduledShowcase` rows with `status=SCHEDULED && scheduledStartAt <= now`, boot them
- Endpoints:
  - `GET    /api/scheduled-showcases/mine`
  - `GET    /api/scheduled-showcases/{id}`
  - `POST   /api/scheduled-showcases` — body `{ deckId, scheduledStartAt, settings, invitedEmails[] }`
  - `PUT    /api/scheduled-showcases/{id}` — host edits before boot
  - `POST   /api/scheduled-showcases/{id}/cancel`
  - `POST   /api/scheduled-showcases/{id}/invite` — body `{ email }` (add later)
  - `POST   /api/invites/{token}/redeem` — invitee hits this from email link; if user is logged in, marks redeemed and returns the joinable showcase URL
- Add `String inviteCode` (existing on Showcase, called `inviteToken`) usable for ad-hoc shares too

## Email templates

Two transactional templates:

- **Initial invite** — sent on schedule creation. Subject: "{HostName} invited you to play {DeckName}". Body has scheduled time + Add to Calendar (ICS attachment).
- **Reminder / boot** — sent when the show boots. Subject: "{DeckName} is starting now". Body has a one-tap join link with `inviteToken`.

Use plain Thymeleaf templates under `backend/src/main/resources/templates/email/`.

## Frontend changes

- Deck detail page gains a "Schedule" button → opens scheduling modal
- Modal:
  - Date/time picker (use a lightweight library — `react-day-picker` is already in many React stacks)
  - Settings preview (read-only summary of the showcase settings the deck will use)
  - Invite email tagged-input (chip per email)
- New `/scheduled` route — host's list of upcoming + past scheduled showcases
- Invite redemption: a new `/invite/$token` route that calls the redeem endpoint and routes the user into the lobby

## Cross-cutting concerns

- Time zones: store everything in UTC; render in the host's local TZ. Host's TZ comes from `User.timezone` (chunk 20).
- Don't email more than once per (email, scheduledShowcase) pair without an explicit "Resend" action.
- Invites resolve to a `userId` lazily — if the invitee isn't registered when they click the link, route through registration with `returnUrl = /invite/{token}`.

## Checklist

- [ ] `ScheduledShowcase` + `ShowcaseInvite` models + repos + indexes
- [ ] `ScheduleStatus` enum
- [ ] `EmailService` interface + SMTP impl
- [ ] Email templates (Thymeleaf)
- [ ] `ScheduledShowcaseService` schedule/cancel/boot/complete
- [ ] `@Scheduled` cron sweep
- [ ] Endpoints + tests (mock `EmailService` in tests)
- [ ] Invite redeem flow + `/invite/$token` route
- [ ] Schedule modal on deck detail page
- [ ] `/scheduled` host list page
- [ ] Frontend codegen + lint
- [ ] Backend tests pass
