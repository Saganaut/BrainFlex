# BrainFlex Project Overview

This project, "BrainFlex," is a full-stack application built as a paired-down version of a previous project, with a focus on learning and integrating new technologies such as MongoDB, Java Spring Boot, and modern React/TypeScript development practices.

## Technologies Used

### Backend
*   **Language:** Java 26
*   **Framework:** Spring Boot 4.0.6
*   **Database:** MongoDB (via `spring-boot-starter-data-mongodb`)
*   **Caching/Data Structure Store:** Redis (via `spring-boot-starter-data-redis`)
*   **Security:** Spring Security with OAuth2 Client (via `spring-boot-starter-security-oauth2-client`)
*   **Web:** Spring Web MVC (via `spring-boot-starter-webmvc`)
*   **Real-time Communication:** Spring WebSocket (via `spring-boot-starter-websocket`)
*   **API Documentation:** Springdoc OpenAPI UI (via `springdoc-openapi-starter-webmvc-ui`)
*   **Utility:** Lombok

### Frontend
*   **Language:** TypeScript
*   **Framework:** React
*   **Build Tool:** Vite
*   **State Management:** Redux Toolkit (`@reduxjs/toolkit`, `react-redux`)
*   **Routing:** TanStack React Router (`@tanstack/react-router`)
*   **Styling:** CSS Modules
*   **Icons:** Heroicons (`@heroicons/react`)
*   **API Generation:** RTK Query Codegen OpenAPI (`@rtk-query/codegen-openapi`)

## Building and Running

The project can be run using Docker Compose for a complete environment, or components can be built and run individually.

### Using Docker Compose

The `compose.yaml` file defines the services needed for the application, including MongoDB and Redis.

To start all services:
```bash
docker compose up -d
```

To stop all services:
```bash
docker compose down
```

### Backend (Java Spring Boot)

**Prerequisites:** Java 26 and Maven.

**Build:**
```bash
cd backend
./mvnw clean install
```

**Run:**
```bash
cd backend
./mvnw spring-boot:run
```

### Frontend (React/TypeScript)

**Prerequisites:** Node.js and npm/yarn.

**Install Dependencies:**
```bash
cd frontend
npm install
```

**Generate API Client:**
This command generates the API client based on the OpenAPI specification provided by the backend.
```bash
cd frontend
npm run generate-api
```

**Run Development Server:**
```bash
cd frontend
npm run dev
```

**Build for Production:**
```bash
cd frontend
npm run build
```

**Preview Production Build:**
```bash
cd frontend
npm run preview
```

## Development Conventions

For detailed project rules and conventions, refer to [RULES.md](RULES.md).

### Backend
*   **Code Style:** Adheres to standard Spring Boot conventions.
*   **Lombok:** Used for reducing boilerplate code (e.g., getters, setters, constructors).
*   **Security:** Spring Security is configured for authentication and authorization.
*   **Validation:** `spring-boot-starter-validation` is used for data validation.

### Frontend
*   **Linting:** ESLint is configured with `eslint-plugin-react-x`, `eslint-plugin-react-dom`, `eslint-plugin-react-hooks`, `eslint-plugin-react-refresh` for code quality and consistency.
*   **Styling:** Stylelint is used for CSS linting, following `stylelint-config-standard`. CSS Modules are used for component-scoped styles.
*   **TypeScript:** Strong typing is enforced throughout the codebase.
*   **Routing:** Utilizes TanStack React Router for client-side navigation.
*   **API Integration:** RTK Query is used for declarative data fetching and caching, with code generation from OpenAPI spec.

## MongoDB Access

To access the MongoDB shell when running via Docker Compose:
```bash
docker exec -it brainflex-mongodb-1 mongosh -u admin -p password
```

Useful MongoDB commands:
*   Show databases: `show dbs`
*   Use a database: `use brainflex`
*   Show collections: `show collections`
*   Find documents: `db.collection.find()`
*   Insert a document: `db.collection.insertOne({ key: 'value' })`
