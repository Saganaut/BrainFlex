# Project Rules and Conventions for BrainFlex

This document outlines essential rules and conventions for consistency, maintainability, and quality across the BrainFlex project.

## 1. General Project Rules

1.  **Consistency:** Maintain consistent naming, formatting, and architectural patterns throughout the codebase.
2.  **Documentation:** Keep `README.md`, `AGENTS.md`, `GEMINI.md`, and `RULES.md` up-to-date with significant project changes.
3.  **Environment Variables:** Manage all sensitive data (API keys, connection strings) via `.env` file at the project root.
4.  **Docker Compose:** Always use `docker compose up -d` for infrastructure services (MongoDB, Redis). Do not rely on Spring Boot's auto-start.
5.  **Git Practices:** Use clear, concise commit messages, focusing on the _why_. Follow conventional commit guidelines.
6.  **DO NOT TAKE SHORTCUTS:** Always follow the established rules and conventions. Do not bypass testing, documentation, or code review processes for expediency. Quality and maintainability are paramount.

## 2. Backend Rules (Java Spring Boot)

1.  **Java/Spring Boot Standards:** Adhere to standard Java and Spring Boot coding conventions and best practices.
2.  **Lombok:** Use Lombok for boilerplate reduction in DTOs and models.
3.  **Layered Architecture:** Strictly maintain the `controller/`, `service/`, `repository/`, `model/`, `dto/`, `config/` separation.
4.  **API Documentation:** Document all REST endpoints using Springdoc OpenAPI for updated `/swagger-ui/` specifications.
5.  **Testing:**
    - Write comprehensive unit/integration tests for new features and bug fixes.
    - Utilize Spring Boot test starters and `@MockitoBean` for mocking.
    - Run tests under the "test" profile with `TestSecurityConfig`.
    - Never change a test to make it pass without addressing the underlying issue. Always fix the code or the test to ensure correctness.
6.  **API Client Regeneration:** Regenerate the frontend API client after any backend API changes affecting the OpenAPI schema.

## 3. Frontend Rules (React/TypeScript)

1.  **TypeScript First:** All new frontend code MUST be TypeScript, prioritizing strict typing over `any`.
2.  **Semantic HTML/JSX & Accessibility:** Use semantic HTML/JSX elements where possible and ensure all components are built with accessibility in mind (e.g., proper ARIA attributes, keyboard navigation).
3.  **React 19 & React Compiler:** Develop compatible with React 19 and its Compiler.
4.  **ESLint & Stylelint:** Strictly adhere to configured ESLint and Stylelint rules. Resolve all linting issues pre-commit.
5.  **CSS Modules & Custom Properties:** Use lower camelCase for selector names. Utilize tokens defined in `frontend/src/tokens.css` for colors, spacing, font sizes, borders, etc.
6.  **TanStack Router:** Implement all client-side routing using TanStack Router's file-based convention (`frontend/src/routes/`).
7.  **State Management:**
    *   **Avoid RTK for Global State:** Minimize the use of Redux Toolkit's core store for generic global state.
    *   **Prefer Cached RTK Query:** Leverage RTK Query's caching and data fetching capabilities as the primary mechanism for managing server-side data and associated UI state.
8.  **Auto-generated API Client:** **NEVER manually edit** `frontend/src/store/BrainFlexApi.ts`. Regenerate it via `npm run generate-api` after backend API changes (backend must be running).
9.  **Testing:** Implement frontend tests following best practices for React/TypeScript.
10. **File Structure:** Adhere to the established frontend file structure. Avoid placing hand-written files in `frontend/src/store/` due to ESLint exclusion.
