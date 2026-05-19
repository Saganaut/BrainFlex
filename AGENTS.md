# BrainFlex — Agent Guide

A full-stack web app for competitive brain games. Learning project focused on MongoDB, Java, and Spring Boot. Built as a paired-down version of Cephadex Games.

> **DO NOT TAKE SHORTCUTS.** Always follow the established rules and conventions. Do not bypass testing, documentation, or code review processes for expediency. Quality and maintainability are paramount.

---

## Project Layout

```
brainflex/
├── frontend/          # React 19 + TypeScript + Vite
│   ├── src/
│   │   ├── components/    # Reusable UI components
│   │   │   ├── Common/    # Shared components (e.g., Buttons)
│   │   │   ├── Forms/     # Form-related components
│   │   │   ├── Leaderboard/
│   │   │   └── PlayerInfo/
│   │   ├── hooks/         # Custom React hooks
│   │   ├── routes/        # TanStack Router routes
│   │   ├── store/         # Redux store and API client
│   │   ├── types/         # TypeScript type guards and utilities
│   │   ├── utils/         # Utility functions
│   │   ├── assets/        # Static assets (icons/, images/) — see ICONS_RULES.md
│   │   ├── index.css      # Global styles and CSS custom properties
│   │   └── main.tsx       # App entry point
│   ├── public/            # Public assets
│   ├── package.json
│   ├── tsconfig.json
│   ├── vite.config.ts
│   └── openapi-config.cts # API codegen config
├── backend/           # Java 26 + Spring Boot 4
│   ├── src/main/java/cephadex/brainflex/
│   │   ├── controller/    # REST endpoints
│   │   ├── service/       # Business logic
│   │   ├── repository/    # MongoDB repositories
│   │   ├── model/         # MongoDB documents
│   │   ├── dto/           # API data transfer objects
│   │   └── config/        # Security, CORS, SampleDataSeeder
│   ├── src/test/          # Unit tests
│   ├── pom.xml
│   └── mvnw               # Maven wrapper
├── compose.yaml       # Docker Compose (MongoDB + Redis)
├── .env               # Shared environment variables
├── z-docs/            # All project documentation (except README) — see Documentation section below
└── README.md
```

---

## Documentation

All project documentation other than the top-level `README.md` lives in **`z-docs/`** at the repo root. When you need background on conventions, infrastructure, or supplemental rules — or when you need to add new documentation — go there first.

Current contents (subject to growth):

| File / dir                        | Purpose                                                                |
| --------------------------------- | ---------------------------------------------------------------------- |
| `z-docs/RULES.md`                 | **Signpost only** — index into the topic-specific rule files below     |
| `z-docs/rules/GENERAL-RULES.md`   | Cross-cutting project rules (consistency, env vars, git, etc.)         |
| `z-docs/rules/BACKEND-RULES.md`   | Java / Spring Boot conventions and testing rules                       |
| `z-docs/rules/FRONTEND-RULES.md`  | React / TypeScript conventions and testing rules                       |
| `z-docs/rules/STYLE-RULES.md`     | CSS modules, tokens, and design-system styling rules                   |
| `z-docs/rules/ICONS-RULES.md`     | Icon/SVG naming, folder, and import conventions                        |
| `z-docs/INFRASTRUCTURE.md`        | Infrastructure notes (Docker, MongoDB, Redis, deployment)              |
| `z-docs/membership.md`            | Organization membership semantics and flows                            |
| `z-docs/NOTES.MD`                 | Working notes / scratchpad                                             |

**Conventions:**

- New design documents, rule docs, architecture notes, and any other long-form documentation belong in `z-docs/` — not at the repo root.
- The repo root keeps only `README.md`, `AGENTS.md`, and `CLAUDE.md` as top-level docs.
- All rule files (`*-RULES.md`) live under `z-docs/rules/`. `z-docs/RULES.md` is just the index.
- Inside rule files, cross-references use the `@FILENAME.md` syntax (e.g. `@BACKEND-RULES.md`); resolution is relative to `z-docs/rules/`.
- Keep filenames kebab-case or SCREAMING-KEBAB for rule docs (matching existing convention).

---

## Running the Project

### Infrastructure (required first)

```bash
docker compose up -d   # starts MongoDB (27017) and Redis (6379)
```

### Backend

```bash
cd backend
./mvnw spring-boot:run
# Runs at http://localhost:8080
# OpenAPI/Swagger UI: http://localhost:8080/swagger-ui/
# OpenAPI schema: http://localhost:8080/v3/api-docs
```

### Frontend

```bash
cd frontend
npm install
npm run dev
# Runs at http://localhost:5173
```

### Regenerate the API client (after backend changes)

```bash
cd frontend
npx @rtk-query/codegen-openapi openapi-config.cts
# Overwrites src/store/BrainFlexApi.ts — do not edit that file manually
```

### Seed sample data

```bash
./scripts/seed-sample-data.sh
```

Manually populates MongoDB with the LOTR sample dataset (users, orgs, themes, decks). Safe to re-run — idempotent per collection per user, never deletes existing data. Stop any running backend first (the script boots its own short-lived Spring Boot process).

---

## Architecture

### Frontend

| Concern      | Tool                                         |
| ------------ | -------------------------------------------- |
| Framework    | React 19 with React Compiler enabled         |
| Language     | TypeScript (strict mode)                     |
| Build        | Vite                                         |
| Routing      | TanStack Router (file-based, code-splitting) |
| State / Data | Redux Toolkit + RTK Query                    |
| API client   | Auto-generated from OpenAPI schema           |
| Styling      | CSS Modules + CSS custom properties          |
| Icons        | Heroicons                                    |
| Rich text    | TipTap (`@tiptap/react` + StarterKit)        |

**Routes** (`frontend/src/routes/`):

- `/` — Home, renders the Leaderboard component and shared auth bar
- `/register` — New-user registration; receives `googleId`, `email`, `name`, `picture`, and optional `returnUrl` as query params after OAuth redirect
- `/about` — About page

**Key conventions:**

- `BrainFlexApi.ts` is auto-generated — never edit it directly.
- API hooks come from RTK Query: `useGetLeaderboardQuery`, `useGetCurrentUserQuery`, etc.
- The root layout is `__root.tsx`; TanStack Router Devtools are mounted there.
- Global styles and CSS custom properties are in `src/index.css` (oklch color space, IBM Plex Mono font). Design tokens (colors, spacing, font sizes, borders) are defined in `src/tokens.css` — always use those tokens, never hardcode values.
- Use semantic HTML/JSX elements and build all components with accessibility in mind (proper ARIA attributes, keyboard navigation).
- Strictly adhere to both ESLint and Stylelint rules. Resolve all linting issues before committing.

**State management:**

- **Avoid RTK for generic global state.** Minimize use of the Redux Toolkit core store for non-server state.
- **Prefer RTK Query caching** as the primary mechanism for server-side data and associated UI state.

**Fullscreen mode:**

App-level fullscreen state lives in `LayoutProvider` (`frontend/src/context/LayoutProvider.tsx`) and is read via `useFullScreen()` (`frontend/src/context/useFullScreen.tsx`). The hook returns `{ isFullScreen, enterFullScreen, exitFullScreen, toggleFullScreen }`.

- **Triggering**: wire `enterFullScreen` / `toggleFullScreen` to whatever UI you want (button, menu, keybinding). There's no per-page convention — each surface adds its own trigger.
- **Exiting**: handled globally. `LayoutProvider` listens for the ESC key while fullscreen is active and renders a floating `ArrowsPointingInIcon` button fixed top-right (styled in `LayoutProvider.module.css`). Pages don't need to render their own exit control.
- **Styling**: there is **no global CSS hook** (e.g. no `body.fullscreen` selector). Components that need to react to fullscreen — collapse the nav, expand a canvas, hide a sidebar — read `isFullScreen` from the hook and toggle a class on themselves in their own stylesheet. Example: `NavBar.tsx` adds `styles.isCollapsed` when fullscreen, and `NavBar.module.css` defines `.isCollapsed { display: none }`. Add a sibling class in any other module that needs to participate.
- The browser Fullscreen API (`element.requestFullscreen()`) is intentionally **not** used — this is app-level layout only, so ESC behavior, mobile Safari, and gesture-trust caveats don't apply.

**Rich text editing:**

- The deck/slide authoring UI uses **TipTap** (`@tiptap/react`, `@tiptap/starter-kit`, `@tiptap/pm`) for any formatted-text field — slide bodies, question explanations, host notes, etc.
- When a field needs more than a plain string (bold, italic, lists, headings), use the project's `RichTextInput` (`components/Common/Input/RichTextInput.tsx`), which wraps `useEditor` + `<EditorContent>` and the supporting TipTap extensions in a single form-input-shaped component with a focus-triggered floating toolbar.
- Toolbar capabilities today: bold / underline / strike / link (inline URL editor, no `window.prompt`) / 6-swatch color / 4-step font size. Extensions in use: `@tiptap/starter-kit`, `@tiptap/extension-text-style` (TextStyle + Color + FontSize), `@tiptap/extensions` (Placeholder).
- No `window.alert` / `window.prompt` / `window.confirm` anywhere. In-editor sub-controls (links, colors, sizes) use inline popovers under the toolbar — follow that pattern for any future toolbar additions.

**Icons & graphics:**

- All SVGs live in `frontend/src/assets/` and are consumed as React components via `vite-plugin-svgr` (already wired in `vite.config.ts`).
- Two top-level lanes: `assets/icons/<category>/` for monochrome UI icons, `assets/images/<category>/` for multi-color illustrations / mascots / brand artwork. Filenames are kebab-case.
- Import pattern (the only pattern): `import TrashIcon from "@/assets/icons/action/trash.svg?react"` → render as `<TrashIcon className={...} />`. Don't `<img src=...>` SVGs and don't write inline `<svg>` markup for new artwork.
- `@heroicons/react` is still installed but is a **placeholder** — designer-shipped icons replace heroicon usages one site at a time.
- Full naming + folder + workflow rules are in **[`ICONS_RULES.md`](../ICONS_RULES.md)** at the repo root.

**Deck editor (`/decks/$deckId/view`):**

The deck-authoring dashboard lives under `frontend/src/components/CreateDashboard/`:

- `CreateDashboard.tsx` — three-column layout (slide rail | active slide | inspector) and the editor navbar with an inline-editable deck title that commits via `updateDeck` on blur/Enter.
- `LeftSidebar.tsx` — the slide rail. Pulls deck elements live via RTK Query, supports drag-to-reorder (optimistic + `moveElement` mutation), and the "New Slide" button opens a modal containing `NewElementPicker` for choosing which kind of element to add.
- `NewElementPicker.tsx` — modal body that lists the 10 element kinds using the existing `SlideTypeGraphics` icons. Click → close modal → add element. Modal uses the shared `useModal` context (`frontend/src/context/useModal.tsx`).
- `SlideThumbnail.tsx` — thumbnail tile with right-click dropdown (delete for now). Carries an HTML `id={elementId}` so the create flow can `getElementById(...).scrollIntoView(...)`; also self-scrolls into view when it becomes the active slide.
- `SlideDisplay.tsx` — dispatches to the correct content editor based on `element.kind`.
- `SlideContentTypes/` — one kind-specific editor per `DeckElement` kind: `SlideContent`, `McqSlideContent`, `TextSlideContent`, `NumberSlideContent`, `RankingSlideContent`, `ScalesSlideContent`, `QAndASlideContent`, `GridSlideContent`, `PlaceOnImageSlideContent`. Shared CSS in `SlideContentTypes.module.css`. MCQ options now carry images, so there is no separate image-choice editor.

**Editor commit pattern:**

Field edits in any slide-content editor follow the same path:

1. Local `useState` mirror of the server value. Re-sync only when `element.id` changes (slide switch), using the "set state during render when prev differs" pattern — no `setState`-in-`useEffect`.
2. `useDebouncedCommit(commitFn, 500)` (from `frontend/src/hooks/useDebouncedCommit.ts`) buffers writes. `schedule(patch)` debounces, `flush()` runs immediately (on blur), `cancel()` drops. Unmount auto-flushes so slide-switches don't lose in-flight edits.
3. Structural changes (add/remove option, toggle correct, etc.) `flush()` any pending text edit first, then `commit()` synchronously.
4. The boilerplate is centralised in `SlideContentTypes/useElementEditor.ts`: each editor calls `useElementEditor<KindType>(isKindType)` to get `{ element, schedule, flush, commit, syncedFromId, markSynced }`.

**Mutation → query cache sync:**

`frontend/src/store/apiEnhancements.ts` is a side-effect-imported file (imported from `store.ts`) that layers `onQueryStarted` handlers onto the auto-generated mutations (`addElement`, `moveElement`, `deleteElement`, `updateElement`, `updateDeck`). Each handler upserts the mutation response into the `getDeck` cache so subscribed components re-render without a manual refetch or tag config. Always go through `enhanceEndpoints` for this — don't edit `BrainFlexApi.ts`.

**Optimistic deck create:**

`/my-decks/create` (a TanStack Router file route) mints a UUID, seeds an empty `DeckDto` into the `getDeck` cache via `BrainFlex.util.upsertQueryData`, navigates to `/decks/$deckId/view` immediately (replace), and fires `POST /api/decks` with that same `id` in the background. The backend accepts a client-supplied id and the operation is idempotent. Same pattern for new elements: the frontend generates the option/slide/item UUID up front so optimistic UI works.

**Image placeholders:**

Until the media-library picker ships, image fields (MCQ option images, Grid backing image, PlaceOnImage target image, Slide media) render a **Lorem Picsum** placeholder seeded on the element/option id (`https://picsum.photos/seed/${id}/...`). Each editor also exposes a raw URL input so authors with a hosted URL can paste it. When the library lands, replace the URL field + `picsum.photos` placeholder with the real picker — search for `TODO: Get more specs` / `placeholderImageUrl` to find every site.

**Component design:**

- Declare components as `const ComponentName = ({ prop }: ComponentNameProps) => {}`. **Never use `function ComponentName()` declarations or `export function` for components** — arrow-function `const` form only.
- Export components with a standalone named export at the bottom of the file: `export { ComponentName }`. Never use default exports or inline `export const`.
- Route files are for routing only — they must delegate to a `RouteNamePage` component in `src/pages/`.
- Place component-specific data (JSON, constants) in a `data.ts` file in the same directory as the component.
- Never use `index.tsx` files — use explicit file names (e.g., `MyComponent.tsx`).
- Favor `interface` over `type`. Props interfaces must be named `ComponentNameProps`.
- Logic should go into a custom hook (e.g., `useComponentName`) in the same directory, and the component should call that hook for all data and behavior.
- Logic that will be used in many components (e.g., auth, theme) should go into a hook in `src/hooks/`.

### Backend

| Concern         | Tool                               |
| --------------- | ---------------------------------- |
| Language        | Java 26                            |
| Framework       | Spring Boot 4                      |
| Build           | Maven (`./mvnw`)                   |
| Database        | MongoDB (Spring Data)              |
| Cache / Pub-Sub | Redis                              |
| Auth            | Spring Security + Google OAuth 2.0 |
| API docs        | SpringDoc OpenAPI v2               |
| Boilerplate     | Lombok                             |

**Package**: `cephadex.brainflex`

**Layers:**

```
controller/   ← REST endpoints
service/      ← (business logic, if added)
repository/   ← MongoRepository interfaces
model/        ← MongoDB documents (@Document)
dto/          ← API shapes (sealed UserDTO with GuestUser/RegisteredUser records)
config/       ← Security, CORS, SampleDataSeeder (manual)
```

---

## API Endpoints

All endpoints are prefixed `/api`.

| Method | Path                     | Auth      | Description                                 |
| ------ | ------------------------ | --------- | ------------------------------------------- |
| GET    | `/api/health`            | GuestUser | MongoDB + Redis health check                |
| GET    | `/api/users/leaderboard` | GuestUser | Paginated leaderboard; `?page=0&size=10`    |
| GET    | `/api/users/{id}`        | Required  | Full user profile                           |
| GET    | `/api/auth/me`           | Optional  | Authenticated user or guest session         |
| GET    | `/api/auth/login`        | Public    | Starts Google OAuth and preserves returnUrl |
| POST   | `/api/auth/guest`        | Public    | Create a guest session with a username      |
| POST   | `/api/auth/logout`       | Required  | Logout                                      |

**CORS**: only `http://localhost:5173` is allowed, with credentials.

### Theme Endpoints (`/api/themes`) — requires `ROLE_USER`

| Method | Path                          | Description                                            |
| ------ | ----------------------------- | ------------------------------------------------------ |
| GET    | `/api/themes`                 | All themes owned by caller + org-shared themes         |
| POST   | `/api/themes`                 | Create a theme (name, huePrimary, hueAccent, mode)     |
| PUT    | `/api/themes/{id}`            | Update name/colors/scope/mode (owner only)             |
| DELETE | `/api/themes/{id}`            | Delete theme (owner only)                              |
| POST   | `/api/themes/{id}/background` | Upload background image (multipart, max 5 MB, 2000 px) |
| POST   | `/api/themes/{id}/logo`       | Upload logo image (multipart, max 2 MB, 400×400 px)    |

### Organization Endpoints (`/api/organizations`) — requires `ROLE_USER`

| Method | Path                              | Description                                                |
| ------ | --------------------------------- | ---------------------------------------------------------- |
| GET    | `/api/organizations/mine`         | All organizations the caller belongs to (may be empty)     |
| POST   | `/api/organizations`              | Create org and add the caller as owner + member            |
| POST   | `/api/organizations/join`         | Join an org by ID; body: `{ organizationId }` (idempotent) |
| DELETE | `/api/organizations/{id}/leave`   | Remove the caller from one specific org                    |

---

## Data Models

### User (MongoDB document, collection: `users`)

```
id               String   (ObjectId)
email            String   (unique index)
name             String
userName         String
isGuest          Boolean
googleId         String
pictureUrl       String
organizationIds  List<String> (orgs this user belongs to; empty = personal-only)
activeThemeId    String   (nullable — ID of the user's active custom theme)
stats            PlayerStats (embedded)
lastLogin        LocalDateTime
createdAt        LocalDateTime
```

Compound index on `stats.totalPoints DESC` for leaderboard sorting.

### PlayerStats (embedded)

```
gamesPlayed     int
highScore       int
totalPoints     int
currentStreak   int
```

### Organization (MongoDB document, collection: `organizations`)

```
id          String   (ObjectId)
name        String
ownerId     String   (userId of creator)
createdAt   LocalDateTime
```

Users may belong to multiple organizations simultaneously. `User.organizationIds` is the list of memberships; joining/leaving an org adds/removes an id from that list. A theme's or deck's `organizationId` is still a single string — content is scoped to one org at a time. Theme create/update rejects an `organizationId` that isn't in the caller's `organizationIds`.

### Theme (MongoDB document, collection: `themes`)

```
id                  String   (ObjectId)
name                String
ownerId             String   (userId)
organizationId      String   (nullable — if set, all org members can view it)
huePrimary          int      (0–360, oklch hue for the primary palette)
hueAccent           int      (0–360, oklch hue for the accent palette)
mode                String   ("light" | "dark" | "system")
backgroundImageUrl  String   (nullable, S3 presigned URL)
logoImageUrl        String   (nullable, S3 presigned URL)
createdAt           LocalDateTime
```

### DTOs

- `UserDTO.GuestUser` — id, userName, isGuest, pictureUrl, stats (safe for leaderboard)
- `UserDTO.RegisteredUser` — all fields including email, googleId, organizationIds, activeThemeId, timestamps (authenticated only)

### Image Processing Tiers

Images are validated by byte-header MIME detection (not Content-Type), resized with Scrimage, and stored as WebP in S3.

| Tier       | Endpoint                           | Max size | Max dimension         | Allowed types        |
| ---------- | ---------------------------------- | -------- | --------------------- | -------------------- |
| Avatar     | `POST /api/users/me/profile-image` | 1 MB     | 500×500 px            | JPEG, PNG, WebP, GIF |
| Logo       | `POST /api/themes/{id}/logo`       | 2 MB     | 400×400 px            | JPEG, PNG, WebP, GIF |
| Background | `POST /api/themes/{id}/background` | 5 MB     | 2000px (longest side) | JPEG, PNG, WebP      |

S3 keys follow deterministic patterns so re-uploading overwrites the same object:

- `profile-images/{userId}/avatar.webp`
- `theme-logos/{themeId}/logo.webp`
- `theme-backgrounds/{themeId}/bg.webp`

---

## Authentication Flow

1. User hits `/api/auth/login` → redirected to Google OAuth. The request preserves the current page via `returnUrl`.
2. On success, Spring Security calls the OAuth2 success handler:
   - **Existing user** (googleId match) → redirect back to the original page
   - **New user** → redirect to `/register?googleId=...&email=...&name=...&picture=...&returnUrl=...`
   - **Guest user who signs in with Google** → convert the guest record to a registered account and redirect back to the original page
3. The `/register` route in the frontend handles new-user form submission and redirects to `returnUrl` after registration.
4. Guests can also start a session via `POST /api/auth/guest` with a username. This creates a guest account and allows play without Google auth.
5. Sessions are cookie-based. The cookie is named `BRAINFLEX_SESSION` and backed by Spring Session Data Redis (`SessionConfig.java`): session attributes (including the `SecurityContext`) are persisted to the Redis container declared in `compose.yaml`, so a backend restart no longer logs everyone out and the app can scale horizontally. Sessions idle for 14 days before expiring. The cookie is HttpOnly, SameSite=Lax (so OAuth redirects from accounts.google.com still carry it), and Secure is auto-detected from the request — http://localhost dev gets Secure=false, HTTPS deploys get Secure=true. `SessionConfig` is gated to `@Profile("!test")` so the test profile keeps using the servlet container's in-memory session map.

---

## Environment Variables

Defined in `.env` at project root. Backend loads it via `spring.config.import=optional:file:../.env[.properties]`. Frontend accesses them with the `VITE_` prefix.

| Variable                                       | Used By                                        |
| ---------------------------------------------- | ---------------------------------------------- |
| `GOOGLE_CLIENT_ID`                             | Backend (OAuth)                                |
| `GOOGLE_CLIENT_SECRET`                         | Backend (OAuth)                                |
| `MONGO_URI`                                    | Backend                                        |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Backend                                        |
| `VITE_API_BASE_URL`                            | Frontend (defaults to `http://localhost:8080`) |

---

## Testing

**Backend**: Spring Boot test slice in `BrainflexApplicationTests.java` (context load test). Test starters for MongoDB, Redis, Security, and WebMVC are on the classpath.

Controller tests added:

- `HealthControllerTest.java` - Tests `/api/health` endpoint with mocked MongoDB and Redis connections
- `UserControllerTest.java` - Tests leaderboard, user profile, and username check endpoints
- `AuthControllerTest.java` - Tests auth endpoints including guest login and registration
- `GameControllerTest.java` - Tests game session create, join, cancel, and results endpoints

Service tests added:

- `UserServiceTest.java` - Tests user creation, registration, and username validation logic
- `GameServiceTest.java` - Tests game session lifecycle logic

When adding backend tests, use the test starters already present in `pom.xml` — no new dependencies needed for standard Spring test slices. Use `@MockitoBean` for mocking in Spring Boot 4 tests. Tests use the "test" profile with `TestSecurityConfig` that permits all requests.

**Test environment variables**: The app normally loads secrets from `dev.env` at runtime via `DotenvEnvironmentPostProcessor`, but that file is not present during test execution. All required values are instead provided in `src/test/resources/application-test.properties` with test-safe defaults (real local Docker credentials for Mongo/Redis, dummy values for Google OAuth and S3). **Do not add real OAuth or S3 credentials to that file** — dummy values are sufficient because tests do not perform real OAuth or S3 operations.

**Security config in tests**: `SecurityConfig` is annotated `@Profile("!test")` so it is excluded during test runs. Only `TestSecurityConfig` is active, which permits all requests and provides the `SecurityContextRepository` bean that `AuthController` requires. If you add new beans to `SecurityConfig` that other components depend on, you must also provide them in `TestSecurityConfig`.

**Mocking `MongoTemplate` in `@SpringBootTest`**: If a test uses `@MockitoBean MongoTemplate`, all Spring Data repositories and `GridFsTemplate` must also be mocked (`@MockitoBean`) — otherwise their initializers call `mongoTemplate.getConverter()` which returns null on a Mockito mock and causes NPE. See `HealthControllerTest` for the full list of required mocks. Additionally, `@MockitoBean RedisConnectionFactory` must be paired with `spring.autoconfigure.exclude=...DataRedisReactiveAutoConfiguration` in `application-test.properties`, because the mock only implements the non-reactive interface but Spring Boot's reactive auto-config expects the same bean to satisfy `ReactiveRedisConnectionFactory`.

**Never change a test to make it pass without addressing the underlying issue. Always fix the code or the test to ensure correctness.**

### CI

GitHub Actions runs both test suites on every push to `main` and every PR targeting `main`. Workflow: `.github/workflows/ci.yml`. Both jobs run in parallel; the push/merge is blocked if either fails.

To enforce this at the repository level, enable branch protection on `main` in GitHub repo Settings → Branches → Require status checks (select `Frontend tests` and `Backend tests`).

### Pre-push hook (local)

Two hook scripts are committed in `scripts/`. Install both once per clone:

```bash
ln -sf ../../scripts/pre-commit .git/hooks/pre-commit
ln -sf ../../scripts/pre-push   .git/hooks/pre-push
```

- **pre-commit** — runs `lint:all` (ESLint + Stylelint) on every commit. The commit is blocked if any lint error is reported.
- **pre-push** — runs both test suites only when pushing to `main`. Pushes to other branches are unaffected.

**Frontend**: Vitest + jsdom + React Testing Library.

| Tool                          | Role                                          |
| ----------------------------- | --------------------------------------------- |
| `vitest`                      | Test runner and assertions                    |
| `jsdom`                       | DOM environment for component rendering       |
| `@testing-library/react`      | Component rendering and querying              |
| `@testing-library/user-event` | Realistic user interaction simulation         |
| `@testing-library/jest-dom`   | Custom DOM matchers (toBeInTheDocument, etc.) |
| `msw`                         | API mocking at the network layer              |

Setup file: `frontend/src/test-setup.ts` — imports `@testing-library/jest-dom` to register custom matchers.

Run tests: `npm test` (watch mode) or `npm run test:run` (single pass).

Co-locate test files with the component they test (e.g., `Btn.test.tsx` next to `Btn.tsx`). Test files must follow the same naming and comment conventions as source files.

---

## Key Files

| File                                                  | Purpose                                                           |
| ----------------------------------------------------- | ----------------------------------------------------------------- |
| `frontend/src/store/BrainFlexApi.ts`                  | Auto-generated RTK Query API — **do not edit**                    |
| `frontend/src/store/store.ts`                         | Redux store config                                                |
| `frontend/src/routes/__root.tsx`                      | Root layout with shared AuthBar                                   |
| `frontend/src/routes/register.tsx`                    | New-user registration route                                       |
| `frontend/src/components/Common/AuthBar.tsx`          | Login / logout / guest play UI                                    |
| `frontend/src/components/Leaderboard/index.tsx`       | Leaderboard UI                                                    |
| `frontend/src/components/PlayerInfo/index.tsx`        | Player info UI component                                          |
| `frontend/src/hooks/useCurrentUser.ts`                | Custom hook for current user authentication                       |
| `frontend/src/hooks/useTheme.ts`                      | Light/dark mode + huePrimary/hueAccent, persisted to localStorage |
| `frontend/src/types/typeguards.ts`                    | TypeScript type guards for user types                             |
| `frontend/src/utils/utils.ts`                         | Utility functions (e.g., camelToNormalCase)                       |
| `frontend/src/pages/AccountPage/ThemeSection.tsx`     | Theme settings UI (presets + custom themes)                       |
| `frontend/src/pages/AccountPage/ThemeEditor.tsx`      | Create/edit theme form with image upload                          |
| `frontend/src/pages/AccountPage/ThemeCard.tsx`        | Single theme card with activate/edit/delete                       |
| `frontend/src/pages/AccountPage/OrgSection.tsx`       | Organization create/join/leave UI                                 |
| `frontend/src/pages/DesignSystemPage/ThemePicker.tsx` | Hue sliders for live design-system exploration                    |
| `frontend/src/pages/DesignSystemPage/FormsSection.tsx` | Form-primitives showcase (incl. `RichTextInput`)                 |
| `frontend/src/components/Common/Input/RichTextInput.tsx` | TipTap-backed input with focus toolbar + inline link editor   |
| `frontend/src/components/CreateDashboard/CreateDashboard.tsx` | Top-level deck editor layout (navbar + 3-col canvas)      |
| `frontend/src/components/CreateDashboard/LeftSidebar.tsx` | Slide rail: add-via-picker, drag-reorder, live deck.elements  |
| `frontend/src/components/CreateDashboard/NewElementPicker.tsx` | Modal body with 10 element-kind tiles                    |
| `frontend/src/components/CreateDashboard/SlideThumbnail.tsx` | Slide tile (right-click menu, scrolls into view on select) |
| `frontend/src/components/CreateDashboard/SlideDisplay.tsx` | Routes to the right `<KindSlideContent>` by `element.kind`    |
| `frontend/src/components/CreateDashboard/SlideContentTypes/useElementEditor.ts` | Shared deck-query + debounced commit hook       |
| `frontend/src/components/CreateDashboard/useCreateDashboard.ts` | Hook for sidebar state: drag end, add element, build defaults |
| `frontend/src/hooks/useDebouncedCommit.ts`            | Generic schedule / flush / cancel debouncer for server commits    |
| `frontend/src/context/ModalProvider.tsx` / `useModal.tsx` | App-wide modal: `openModal({ title, content })` / `closeModal()` |
| `frontend/src/context/LayoutProvider.tsx` / `useFullScreen.tsx` | App-level fullscreen state + global ESC handler + floating exit button |
| `frontend/src/store/apiEnhancements.ts`               | `onQueryStarted` cache-sync for element/deck mutations            |
| `frontend/src/routes/my-decks/create.tsx`             | Optimistic deck-create: UUID + cache seed + navigate              |
| `frontend/openapi-config.cts`                         | Config for API codegen                                            |
| `backend/.../config/SecurityConfig.java`              | Auth, CORS, public routes                                         |
| `backend/.../config/SampleDataSeeder.java`            | Manual sample data seeder (run via `scripts/seed-sample-data.sh`) |
| `scripts/seed-sample-data.sh`                         | Trigger manual MongoDB sample-data seed (idempotent per collection)|
| `backend/.../dto/UserDTO.java`                        | Sealed DTO interface (GuestUser / RegisteredUser)                 |
| `backend/.../repository/UserRepository.java`          | MongoDB queries                                                   |
| `backend/.../controller/ThemeController.java`         | REST endpoints at `/api/themes`                                   |
| `backend/.../controller/OrganizationController.java`  | REST endpoints at `/api/organizations`                            |
| `compose.yaml`                                        | Docker services (MongoDB, Redis)                                  |
| `dev.env`                                             | Local dev secrets (not committed to git — copy from `example.env`) |
| `backend/src/test/resources/application-test.properties` | Test-profile env var overrides (test-safe values, no real secrets) |

---

## Gotchas

- `BrainFlexApi.ts` is regenerated from `http://localhost:8080/v3/api-docs` — the backend must be running when you run codegen.
- `spring.docker.compose.enabled=false` — Spring does **not** auto-start Docker; run `docker compose up -d` yourself.
- **Seeding sample data is manual.** Nothing runs on startup. Run `scripts/seed-sample-data.sh` to populate MongoDB with LOTR-themed users (from `seed/users.json` when the collection is empty), the two `system` decks (Welcome Tour + General Knowledge), faction-based Organizations, a personal Theme per user, and 1–2 LOTR-themed Decks per user. The seeder is idempotent **per collection per user** — re-running tops up missing pieces without overwriting anything (decks are skipped for any user who already owns ≥1 deck; themes are skipped for any user who already owns ≥1 theme; a user's faction org is added to `organizationIds` only if they're not already a member of it). Nothing is ever deleted. The script runs the Spring Boot app with `--seed.run=true`, which is the only thing that activates `SampleDataSeeder`; a normal `./mvnw spring-boot:run` boot does not seed anything.
- WebSocket support is included as a dependency but no WebSocket endpoints are implemented yet.
- There is no `.env` file in the repo. For local development, copy `example.env` to `dev.env` and fill in real credentials. `DotenvEnvironmentPostProcessor` loads `dev.env` (or `.env`) at runtime but silently skips if neither exists — tests do not rely on it at all.
- Backend tests require Docker to be running (`docker compose up -d`) because `@SpringBootTest` controller tests connect to the real local MongoDB and Redis.
- **MCQ multi-correct**: `McqQuestion.correctOptionIds` is a `List<String>` (any non-empty subset of `options[].id` counts as correct). Older code/data may have used a single `correctOptionId`; the field was renamed when multi-correct landed. MCQs with an empty `correctOptionIds` list are author-allowed but excluded from scored game modes — the editor surfaces a warning ("Not setting a correct answer means this slide is not scoreable in a game showcase"). MCQ options can also carry images (`McqOption.imageUrl` / `galleryImageId`), which replaces the retired `ImageChoiceQuestion` kind.
- **`BrainFlexApi.ts` hand-edits**: avoid them. A one-off hand-edit was needed when MCQ became multi-correct (the codegen file lagged the backend rename until the user could run `npm run generate-api`). The field carries a comment explaining the reason. After any codegen run, re-verify `McqQuestion.correctOptionIds?: string[]`.
- **Lorem Picsum image placeholders**: image fields without an uploaded asset render `https://picsum.photos/seed/${id}/...`. The seed is the element/option id so renders stay stable. Every editor with image fields also has a raw-URL input so authors can paste a hosted URL. Grep for `placeholderImageUrl` to find every site to migrate when the media picker ships.
- **Element-payload primitives**: every backend question/slide record uses primitive `int`/`double`/`boolean` for shared chrome (`displaySeconds`, `pointValue`, `bestAnswerMode`, `bestAnswerBonus`, `multipleCorrect`, `caseSensitive`, etc.). Jackson can't deserialize `null` into a primitive, so every `addElement` payload from the frontend must include defaults for these. `useCreateDashboard.ts:buildNewElement` already does this per kind; copy the same pattern for any new element kind or any payload-construction site.
