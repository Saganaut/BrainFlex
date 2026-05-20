# 22 — Common-component gaps

**Status:** Not started
**Depends on:** nothing
**Unblocks:** any future surface that needs paginated lists or inline form feedback

## Scope

Most of the `frontend/src/components/Common/` family is already populated (Avatar, Badge, Buttons, Cards, ConfirmDialog, Divider, EmptyState, GalleryPicker, Input, Loader, Modal, ProgressBar, SelectableTile, Skeleton, StarRating, Tabs, Tag, TagPicker, Toast, Tooltip). Two gaps remain from the original `todo.md`:

1. **Pagination** — there is no reusable pager. `CommentThread` rolls its own "Show more" affordance, and `Leaderboard` does manual page-state. Replace both with a shared component once it ships.
2. **Inline Alert / Banner** — `Toast` covers transient notifications and `WsErrorBanner` is interactive session-specific. There's nothing for inline form feedback (e.g. "Image is too large — please pick something under 5 MB") that should sit next to a field rather than fly across the screen.

## Part A — `Pagination`

**Component:** `frontend/src/components/Common/Pagination/Pagination.tsx`

**Props (suggested):**

- `page: number` — zero-indexed current page
- `pageCount: number`
- `onPageChange: (page: number) => void`
- `siblingCount?: number` — pages shown on either side of current (default `1`)
- `boundaryCount?: number` — pages shown at the very start/end (default `1`)
- `disabled?: boolean`
- `compact?: boolean` — render as prev/next only, no page numbers (for narrow surfaces)

**Behaviour:**

- Render `‹ 1 … 4 5 6 … 12 ›` style; collapse runs with an ellipsis token.
- Keyboard: left/right arrows when focused inside the component.
- ARIA: `nav aria-label="Pagination"`, `aria-current="page"` on the active button.
- Use design tokens for chip colors and spacing — no hardcoded values. Follow the existing `Tabs` styling palette.

**Wire-up sites (do as part of this chunk):**

- `frontend/src/components/Common/CommentThread/CommentThread.tsx` — replace the "Show more" button.
- `frontend/src/components/Leaderboard/Leaderboard.tsx` — replace whatever the leaderboard uses today.
- The Explore page (when chunk 02 lands its grid) and the deck-discussion panel should adopt this same component.

## Part B — `Alert` / `Banner`

**Component:** `frontend/src/components/Common/Alert/Alert.tsx`

This is *inline* feedback — placed where it logically belongs in the form/page flow. Toast remains for system-level transient messages.

**Props:**

- `severity: "info" | "success" | "warning" | "error"`
- `title?: string`
- `children: ReactNode` — body content
- `onDismiss?: () => void` — optional close button
- `icon?: ReactNode` — override the default severity icon
- `compact?: boolean` — single-line variant for tight layouts

**Behaviour:**

- Auto-picks an icon per severity (info `i`, success ✓, warning ⚠, error ⚠ in error color).
- ARIA: `role="alert"` for `error` / `warning`, `role="status"` for `info` / `success`.
- Uses semantic tokens (`--bg-info`, `--bg-warning`, etc.) — never palette tokens directly.
- Dismiss button (when `onDismiss` provided) is a chrome-less icon button.

**Wire-up sites (do as part of this chunk):**

- `ThemeEditor.tsx` — replace the size-exceeded `<p className={styles.error}>` strings with `<Alert severity="error">`.
- `RegisterPage.tsx` (and the guest-login form) — replace bespoke error spans.
- Anywhere a current `<p className="error">` or inline error string is rendered — sweep with grep (`rg "className=\"?\\w*error" frontend/src/components frontend/src/pages`).

## Cross-cutting concerns

- **Design system page** — both components must appear in the design-system page at `/design-system` with usage examples in every severity / variant. The page already has a `FormsSection` and other interactive sessions; add `PaginationSection` and `AlertSection`.
- **Tests** — co-located vitest + RTL: page-navigation behavior for Pagination (clicking arrows, jumping to a page number) and severity-to-role mapping for Alert.
- **Token discipline** — both components must use semantic tokens only. Reference [feedback_semantic_tokens_only](../../../.claude/projects/-home-balooski-Repos-brainflex/memory/feedback_semantic_tokens_only.md).

## Checklist

- [ ] `Pagination` component + module CSS
- [ ] `Pagination` shown in design-system page with at least three configurations (default, compact, disabled)
- [ ] `CommentThread` migrated to `Pagination`
- [ ] `Leaderboard` migrated to `Pagination`
- [ ] `Pagination` tests (page change, keyboard nav, aria-current, boundary collapse)
- [ ] `Alert` component + module CSS, all four severities
- [ ] `Alert` shown in design-system page with every severity + dismissable variant
- [ ] At least three error-span sites converted to `Alert`
- [ ] `Alert` tests (severity → role mapping, dismiss button fires `onDismiss`)
- [ ] Frontend lint + tests pass
