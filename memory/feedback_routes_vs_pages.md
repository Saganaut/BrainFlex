---
name: Routes are wrappers only; page content goes in /pages
description: TanStack Router route files must only define the route and render the page component — no page logic inline
type: feedback
---

Route files in frontend/src/routes/ must only define the route and render the page component. All page content, state, and logic goes in a dedicated component under frontend/src/pages/PageName/index.tsx (with a co-located PageName.module.css).

**Why:** Mixing routing concerns with page logic makes pages untestable and non-reusable. The project convention is clear from the existing DesignSystemPage pattern.

**How to apply:** When adding or editing a page, create frontend/src/pages/MyPage/index.tsx for the content and frontend/src/routes/my-page.tsx as a thin wrapper that just imports and renders it.
