# BrainFlex — Agent Guide

A full-stack web app for competitive brain games. Learning project focused on MongoDB, Java, and Spring Boot. Built as a paired-down version of Cephadex Games.

---

## Project Layout

```
brainflex/
├── frontend/          # React 19 + TypeScript + Vite
├── backend/           # Java 26 + Spring Boot 4
├── compose.yaml       # Docker Compose (MongoDB + Redis)
├── .env               # Shared env vars loaded by both frontend and backend
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

| Concern | Tool |
|---|---|
| Framework | React 19 with React Compiler enabled |
| Language | TypeScript (strict mode) |
| Build | Vite |
| Routing | TanStack Router (file-based, code-splitting) |
| State / Data | Redux Toolkit + RTK Query |
| API client | Auto-generated from OpenAPI schema |
| Styling | CSS Modules + CSS custom properties |
| Icons | Heroicons |

**Routes** (`frontend/src/routes/`):
- `/` — Home, renders the Leaderboard component
- `/register` — New-user registration; receives `googleId`, `email`, `name`, `picture` as query params after OAuth redirect
- `/about` — About page

**Key conventions:**
- `BrainFlexApi.ts` is auto-generated — never edit it directly.
- API hooks come from RTK Query: `useGetLeaderboardQuery`, `useGetCurrentUserQuery`, etc.
- The root layout is `__root.tsx`; TanStack Router Devtools are mounted there.
- CSS custom properties are defined in `src/index.css` (oklch color space, IBM Plex Mono font).

### Backend

| Concern | Tool |
|---|---|
| Language | Java 26 |
| Framework | Spring Boot 4 |
| Build | Maven (`./mvnw`) |
| Database | MongoDB (Spring Data) |
| Cache / Pub-Sub | Redis |
| Auth | Spring Security + Google OAuth 2.0 |
| API docs | SpringDoc OpenAPI v2 |
| Boilerplate | Lombok |

**Package**: `cephadex.brainflex`

**Layers:**
```
controller/   ← REST endpoints
service/      ← (business logic, if added)
repository/   ← MongoRepository interfaces
model/        ← MongoDB documents (@Document)
dto/          ← API shapes (sealed UserDTO with Public/Private records)
config/       ← Security, CORS, DataSeeder
```

---

## API Endpoints

All endpoints are prefixed `/api`.

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/health` | Public | MongoDB + Redis health check |
| GET | `/api/users/leaderboard` | Public | Paginated leaderboard; `?page=0&size=10` |
| GET | `/api/users/{id}` | Required | Full user profile |
| GET | `/api/auth/me` | Optional | Authenticated user or guest stub |
| POST | `/api/auth/logout` | Required | Logout |

**CORS**: only `http://localhost:5173` is allowed, with credentials.

---

## Data Models

### User (MongoDB document, collection: `users`)
```
id            String   (ObjectId)
email         String   (unique index)
name          String
userName      String
isGuest       Boolean
googleId      String
pictureUrl    String
stats         PlayerStats (embedded)
lastLogin     LocalDateTime
createdAt     LocalDateTime
```
Compound index on `stats.totalPoints DESC` for leaderboard sorting.

### PlayerStats (embedded)
```
gamesPlayed     int
highscore       int
totalPoints     int
currentStreak   int
```

### DTOs
- `UserDTO.Public` — id, userName, isGuest, pictureUrl, stats (safe for leaderboard)
- `UserDTO.Private` — all fields including email, googleId, timestamps (authenticated only)

---

## Authentication Flow

1. User hits `/api/auth/login` → redirected to Google OAuth.
2. On success, Spring Security calls the OAuth2 success handler:
   - **Existing user** (googleId match) → redirect to `http://localhost:5173/dashboard`
   - **New user** → redirect to `http://localhost:5173/register?googleId=...&email=...&name=...&picture=...`
3. The `/register` route in the frontend handles new-user form submission.
4. Sessions are cookie-based (Spring Security default).

---

## Environment Variables

Defined in `.env` at project root. Backend loads it via `spring.config.import=optional:file:../.env[.properties]`. Frontend accesses them with the `VITE_` prefix.

| Variable | Used By |
|---|---|
| `GOOGLE_CLIENT_ID` | Backend (OAuth) |
| `GOOGLE_CLIENT_SECRET` | Backend (OAuth) |
| `MONGO_URI` | Backend |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Backend |
| `VITE_API_BASE_URL` | Frontend (defaults to `http://localhost:8080`) |

---

## Testing

**Backend**: Spring Boot test slice in `BrainflexApplicationTests.java` (context load test). Test starters for MongoDB, Redis, Security, and WebMVC are on the classpath.

**Frontend**: No test files currently. ESLint is configured for code quality.

When adding backend tests, use the test starters already present in `pom.xml` — no new dependencies needed for standard Spring test slices.

---

## Key Files

| File | Purpose |
|---|---|
| `frontend/src/store/BrainFlexApi.ts` | Auto-generated RTK Query API — **do not edit** |
| `frontend/src/store/store.ts` | Redux store config |
| `frontend/src/routes/__root.tsx` | Root layout |
| `frontend/src/routes/register.tsx` | New-user registration route |
| `frontend/src/components/Leaderboard/index.tsx` | Leaderboard UI |
| `frontend/openapi-config.cts` | Config for API codegen |
| `backend/.../config/SecurityConfig.java` | Auth, CORS, public routes |
| `backend/.../config/DataSeeder.java` | Seeds 15 LOTR test users on first startup |
| `backend/.../dto/UserDTO.java` | Sealed DTO interface (Public / Private) |
| `backend/.../repository/UserRepository.java` | MongoDB queries |
| `compose.yaml` | Docker services (MongoDB, Redis) |
| `.env` | All secrets and connection strings |

---

## Gotchas

- `BrainFlexApi.ts` is regenerated from `http://localhost:8080/v3/api-docs` — the backend must be running when you run codegen.
- `spring.docker.compose.enabled=false` — Spring does **not** auto-start Docker; run `docker compose up -d` yourself.
- The DataSeeder only runs when the `users` collection is empty. To reseed, drop the collection.
- Frontend ESLint excludes `src/store/` (auto-generated) — don't add hand-written files there.
- WebSocket support is included as a dependency but no WebSocket endpoints are implemented yet.
