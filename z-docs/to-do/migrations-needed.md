Use this to track refactors for which we will need to create migrations, update seed-file, update-tests, and update mock data on the front and backend.

---

## BillingState extraction — 3c of [java-model-issues.md](java-model-issues.md#3c-membership-and-organizationplan-are-90-of-the-same-struct--resolved)

The seven Stripe-shaped fields (`tier`, `status`, `startedAt`, `currentPeriodEnd`, `cancelAtPeriodEnd`, `stripeCustomerId`, `stripeSubscriptionId`) were lifted off `Membership` and `OrganizationPlan` into an embedded `BillingState billing` sub-document. The application reads/writes them through `getBilling().getTier()` etc., so existing documents that still store the fields flat under `membership.*` / `plan.*` will deserialize with `billing.tier = FREE` and `billing.status = NONE` (the value-object defaults), silently dropping any pre-existing billing state.

**Affected collections.** Two writes per affected document; both are nest-into-sub-doc moves with the same source/destination shape.

- `users` — flatten `membership.{tier,status,startedAt,currentPeriodEnd,cancelAtPeriodEnd,stripeCustomerId,stripeSubscriptionId}` into `membership.billing.*`.
- `organizations` — flatten `plan.{tier,status,startedAt,currentPeriodEnd,cancelAtPeriodEnd,stripeCustomerId,stripeSubscriptionId}` into `plan.billing.*`.

**Mongo shell script (idempotent — skip docs that already have `membership.billing` / `plan.billing`).**

```javascript
// 1. Users
db.users.find({
  "membership": { $exists: true },
  "membership.billing": { $exists: false }
}).forEach(function(u) {
  var m = u.membership || {};
  var billing = {
    tier: m.tier || "FREE",
    status: m.status || "NONE",
    startedAt: m.startedAt || null,
    currentPeriodEnd: m.currentPeriodEnd || null,
    cancelAtPeriodEnd: m.cancelAtPeriodEnd != null ? m.cancelAtPeriodEnd : false,
    stripeCustomerId: m.stripeCustomerId || null,
    stripeSubscriptionId: m.stripeSubscriptionId || null
  };
  db.users.updateOne(
    { _id: u._id },
    {
      $set: { "membership.billing": billing },
      $unset: {
        "membership.tier": "",
        "membership.status": "",
        "membership.startedAt": "",
        "membership.currentPeriodEnd": "",
        "membership.cancelAtPeriodEnd": "",
        "membership.stripeCustomerId": "",
        "membership.stripeSubscriptionId": ""
      }
    }
  );
});

// 2. Organizations
db.organizations.find({
  "plan": { $exists: true },
  "plan.billing": { $exists: false }
}).forEach(function(o) {
  var p = o.plan || {};
  var billing = {
    tier: p.tier || "FREE",
    status: p.status || "NONE",
    startedAt: p.startedAt || null,
    currentPeriodEnd: p.currentPeriodEnd || null,
    cancelAtPeriodEnd: p.cancelAtPeriodEnd != null ? p.cancelAtPeriodEnd : false,
    stripeCustomerId: p.stripeCustomerId || null,
    stripeSubscriptionId: p.stripeSubscriptionId || null
  };
  db.organizations.updateOne(
    { _id: o._id },
    {
      $set: { "plan.billing": billing },
      $unset: {
        "plan.tier": "",
        "plan.status": "",
        "plan.startedAt": "",
        "plan.currentPeriodEnd": "",
        "plan.cancelAtPeriodEnd": "",
        "plan.stripeCustomerId": "",
        "plan.stripeSubscriptionId": ""
      }
    }
  );
});
```

**Wire shape.** Unchanged — `MembershipDTO` and `OrganizationPlanDTO` still expose the seven fields flat, projected through `getBilling()`. No frontend or codegen changes required.

**Seed data.** `SampleDataSeeder` does not write `Membership` / `OrganizationPlan` fields directly today (users are seeded with the default `new Membership()`, which now contains the default `BillingState`). No seeder changes needed.

**Tests.** `AuthoritiesServiceTest` and `ModelDefaultsTest` updated in-tree. No fixture files or JSON mocks reference the flat `tier`/`status` keys on `Membership` / `OrganizationPlan`.

---

## `UserResponse.RegisteredUser` — `googleId` → `externalIdentity{provider,id}` — 5e of [java-model-issues.md](java-model-issues.md#5e-userdtoregistereduser-exposes-googleid-but-not-discordid--microsoftid)

The flat `googleId` field on `UserResponse.RegisteredUser` was replaced with an `ExternalIdentity(provider, id)` sub-record. The DTO now reports whichever provider id column on `User` is populated (`google` / `discord` / `microsoft`) instead of silently dropping Discord and Microsoft accounts. The underlying `User` document is unchanged — `googleId`, `discordId`, and `microsoftId` remain separate fields on the model, and the projection happens in `UserResponse.externalIdentityOf(user)`.

**No MongoDB migration required.** The model still stores the three id columns flat under `users.{googleId,discordId,microsoftId}`. Only the wire shape changed.

**Redirect URL.** `SecurityConfig.OAuth2SuccessHandler` no longer emits the legacy `googleId` query param to `/register` — only `provider` + `providerId`. The frontend route (`frontend/src/routes/register.tsx`) and `RegisterSearch` interface (`frontend/src/components/Forms/useRegister.ts`) were updated in this change to read the new params.

**Deferred follow-ups (do these before merging anything that depends on the new wire shape).**

- **Regenerate the API client.** With the backend running, `cd frontend && npx @rtk-query/codegen-openapi openapi-config.cts`. This rewrites `frontend/src/store/BrainFlexApi.ts` and is what gives TypeScript the new `externalIdentity: { provider, id }` field on `RegisteredUser`. Until this is run, `MockData.ts` will not type-check against the regenerated DTO.
- **Update frontend mocks.** `frontend/src/utils/MockData.ts:100` and `:128` set `googleId: "google_frodo_001"` / `"google_gandalf_001"` on `mockFrodoUser` / `mockGandalfUser`. Replace with `externalIdentity: { provider: "google", id: "google_frodo_001" }` (and same for Gandalf). Other mock users (e.g. `mockAragornUser`) carry no provider id today — leave those alone, or give them an `externalIdentity` if you want them to look fully registered.
- **Update backend tests.** No DTO-level test references `googleId` on `UserResponse.RegisteredUser` today (`UserServiceTest.register_WhenValid_CreatesUser` and `NotificationServiceTest.registeredUser` both assert against the `User` model, which is unchanged). If new tests start covering `UserResponse` directly, they should assert `externalIdentity().provider()` / `.id()` instead of a flat `googleId` field.
- **Seed script.** `SampleDataSeeder` does not write `googleId` / `discordId` / `microsoftId` today, and `scripts/seed-sample-data.sh` is a thin wrapper around the seeder — no script changes required. If the seeder ever starts minting OAuth-shaped sample users, populate the underlying model fields (`user.setGoogleId(...)`) and the DTO projection picks it up automatically.
- **Docs.** `z-docs/features/auth/README.md` still references the old `/register?googleId=...` query param and "googleId match" wording in the sequence diagram + prose (lines ~117-200, 327, 364). Sweep those references to the new `provider`+`providerId` pair when next touching the auth docs. `z-docs/features/data-models.md:65` also lists `googleId` as a field on `UserDTO.RegisteredUser`; update to `externalIdentity`.

---

## `ThemeResponse.mode` lowercase wire shim removed — now the `ThemeMode` enum (`LIGHT`/`DARK`/`SYSTEM`) — part of the [DTO-NAMING-RULES](../rules/DTO-NAMING-RULES.md) cleanup

The retired `ThemeDTO.ThemeResponse` wrapper lowercased `mode` on the wire (`theme.getMode().name().toLowerCase()`) as a back-compat shim for a frontend that still compared against `"light"` / `"dark"`. The canonical top-level `ThemeResponse` / `CreateThemeRequest` / `UpdateThemeRequest` records carry `mode` as the raw `ThemeMode` enum, so the wire is now `LIGHT` / `DARK` / `SYSTEM` in **both** directions (read and write). `ThemeController.validateMode(String)` is gone — Jackson parses the enum directly, and an absent `mode` on create defaults to `SYSTEM`.

**Mongo migration — one-shot lowercase → uppercase backfill of `themes.mode`.** The `Theme` model field has been the `ThemeMode` enum for a while (persisted as the enum `name()`), so freshly-written documents are already uppercase and `SampleDataSeeder` (~line 479) already `valueOf(...toUpperCase())`s before saving. Only documents left over from the pre-enum free-form `String mode` era can still hold lowercase values, which now fail to deserialize. Idempotent — only touches lowercase rows.

```javascript
db.themes.find({ mode: { $in: ["light", "dark", "system"] } }).forEach(function (t) {
  db.themes.updateOne({ _id: t._id }, { $set: { mode: t.mode.toUpperCase() } });
});
```

**Frontend — done in this change.** A conversion seam (`frontend/src/utils/themeMode.ts` — `apiToUiMode` / `uiToApiMode`) maps the uppercase API enum to/from the lowercase convention the CSS theme system + editor select use (those lowercase values double as DOM class suffixes and localStorage entries, so they stay lowercase). Applied at every boundary: `useThemePicker`, `useActiveThemeSync`, `useThemeEditor` (seed + save), and the four `MockData.ts` theme literals.

**Tests + seed.** No backend test asserts a theme `mode` wire value and there is no `ThemeController` test, so nothing to update there. The seeder already writes the enum, so seeded themes need no change.
