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
│   │   ├── assets/        # Static assets
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
│   │   └── config/        # Security, CORS, DataSeeder
│   ├── src/test/          # Unit tests
│   ├── pom.xml
│   └── mvnw               # Maven wrapper
├── compose.yaml       # Docker Compose (MongoDB + Redis)
├── .env               # Shared environment variables
└── README.md
```

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

**Component design:**

- Declare components as `const ComponentName = ({ prop }: ComponentNameProps) => {}`. **Never use `function ComponentName()` declarations or `export function` for components** — arrow-function `const` form only.
- Export components with a standalone named export at the bottom of the file: `export { ComponentName }`. Never use default exports or inline `export const`.
- Route files are for routing only — they must delegate to a `RouteNamePage` component in `src/pages/`.
- Place component-specific data (JSON, constants) in a `data.ts` file in the same directory as the component.
- Never use `index.tsx` files — use explicit file names (e.g., `MyComponent.tsx`).
- Favor `interface` over `type`. Props interfaces must be named `ComponentNameProps`.

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
config/       ← Security, CORS, DataSeeder
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

| Method | Path                          | Description                                     |
| ------ | ----------------------------- | ----------------------------------------------- |
| GET    | `/api/organizations/me`       | Caller's current organization (404 if none)     |
| POST   | `/api/organizations`          | Create org and set caller as owner/first member |
| POST   | `/api/organizations/join`     | Join an org by ID; body: `{ organizationId }`   |
| DELETE | `/api/organizations/me/leave` | Leave current org (sets organizationId to null) |

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
organizationId   String   (nullable — set when user joins an org)
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

One user belongs to at most one organization at a time. Joining a new org requires leaving the current one first.

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
- `UserDTO.RegisteredUser` — all fields including email, googleId, organizationId, activeThemeId, timestamps (authenticated only)

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
5. Sessions are cookie-based (Spring Security default).

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

Service tests added:

- `UserServiceTest.java` - Tests user creation, registration, and username validation logic

When adding backend tests, use the test starters already present in `pom.xml` — no new dependencies needed for standard Spring test slices. Use `@MockitoBean` for mocking in Spring Boot 4 tests. Tests use the "test" profile with `TestSecurityConfig` that permits all requests to avoid authentication redirects.

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
| `frontend/openapi-config.cts`                         | Config for API codegen                                            |
| `backend/.../config/SecurityConfig.java`              | Auth, CORS, public routes                                         |
| `backend/.../config/DataSeeder.java`                  | Seeds 15 LOTR test users on first startup                         |
| `backend/.../dto/UserDTO.java`                        | Sealed DTO interface (GuestUser / RegisteredUser)                 |
| `backend/.../repository/UserRepository.java`          | MongoDB queries                                                   |
| `backend/.../controller/ThemeController.java`         | REST endpoints at `/api/themes`                                   |
| `backend/.../controller/OrganizationController.java`  | REST endpoints at `/api/organizations`                            |
| `compose.yaml`                                        | Docker services (MongoDB, Redis)                                  |
| `.env`                                                | All secrets and connection strings                                |

---

## Gotchas

- `BrainFlexApi.ts` is regenerated from `http://localhost:8080/v3/api-docs` — the backend must be running when you run codegen.
- `spring.docker.compose.enabled=false` — Spring does **not** auto-start Docker; run `docker compose up -d` yourself.
- The DataSeeder only runs when the `users` collection is empty. To reseed, drop the collection.
- WebSocket support is included as a dependency but no WebSocket endpoints are implemented yet.
