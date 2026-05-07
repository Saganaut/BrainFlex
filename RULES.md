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
7.  **CI must pass before merging:** Every PR targeting `main` must have the GitHub Actions CI checks (`Frontend tests` and `Backend tests`) passing before it is merged.

## 3. Frontend Rules (React/TypeScript)

1.  **TypeScript First:** All new frontend code MUST be TypeScript, prioritizing strict typing over `any`.
2.  **Semantic HTML/JSX & Accessibility:** Use semantic HTML/JSX elements where possible and ensure all components are built with accessibility in mind (e.g., proper ARIA attributes, keyboard navigation).
3.  **React 19 & React Compiler:** Develop compatible with React 19 and its Compiler.
4.  **ESLint & Stylelint:** Strictly adhere to configured ESLint and Stylelint rules. Both must report zero errors before committing — enforced by the `scripts/pre-commit` hook (`npm run lint:all`). CI also runs lint on every push/PR to `main`.
5.  **CSS Modules & Custom Properties:** Use lower camelCase for selector names. Utilize tokens defined in `frontend/src/tokens.css` for colors, spacing, font sizes, borders, etc.
6.  **TanStack Router:** Implement all client-side routing using TanStack Router's file-based convention (`frontend/src/routes/`).
7.  **State Management:**
    - **Avoid RTK for Global State:** Minimize the use of Redux Toolkit's core store for generic global state.
    - **Prefer Cached RTK Query:** Leverage RTK Query's caching and data fetching capabilities as the primary mechanism for managing server-side data and associated UI state.
8.  **Auto-generated API Client:** **NEVER manually edit** `frontend/src/store/BrainFlexApi.ts`. Regenerate it via `npm run generate-api` after backend API changes (backend must be running).
9.  **Testing:** Use **Vitest + jsdom + React Testing Library** for all frontend tests.
    - Stack: `vitest`, `jsdom`, `@testing-library/react`, `@testing-library/user-event`, `@testing-library/jest-dom`, `msw` (for network-layer API mocks).
    - Co-locate test files with the component (`Btn.test.tsx` alongside `Btn.tsx`).
    - Query by accessible role/name first; fall back to `data-testid` only when no semantic query applies.
    - Never change a test to make it pass without addressing the underlying issue. Always fix the code or the test to ensure correctness.
10. **File Structure:** Adhere to the established frontend file structure. Avoid placing hand-written files in `frontend/src/store/` due to ESLint exclusion.
11. **Component Design:** Always use const ComponentName = ({}:ComponentNameProps) => {} ... export { ComponentName } for all React components. Avoid default exports to maintain consistency and improve readability.
    Routes are only for routing, they should refer to a RouteNamePage component in the Pages dir.
    Data such as JSON used in a component should be placed in a data.ts file in the same directory as the component
    Never use index.tsx files, instead use explicit file names
    Favor interfaces over types. Props interfaces should be named ComponentNameProps
