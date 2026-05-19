# 23 — Deck editor shell polish

**Status:** Not started
**Depends on:** nothing structural; coordinate with **21** (Slide design & sidebar polish) — this chunk restructures the *shell* that hosts 21's panels.
**Unblocks:** every future right-sidebar panel (Participant settings, Sharing preferences) gets a home.

## Scope

The deck editor (`/decks/$deckId/view`) renders today through `frontend/src/components/CreateDashboard/CreateDashboard.tsx`. Three pieces of its shell are stubs or missing:

1. **Editor navbar** — has Preview and Start (showcase) buttons, but both are `console.log` placeholders. Wire them up.
2. **Left sidebar (new-deck flow)** — the slide rail is empty until the user adds an element. Render a "first slide" skeleton so the new-deck canvas isn't a blank screen.
3. **Right sidebar** — currently uses a top-tabs IA (`RightSidebarContent.tsx` flips between `EditSlidePanel`, `ThemePanel`, `DeckCategorizePanel`, `DeckReviewsPanel`, `DeckDiscussionPanel`). The product target is a **vertical icon rail** along the far-right edge with each icon opening a **drawer to its left**. Two of the four target drawers don't exist yet (Participant settings, Sharing preferences).

Chunk 21 modifies the *content* inside `EditSlidePanel` / `DeckCategorizePanel` / `DeckReviewsPanel`. This chunk modifies the *shell* those panels live in. Land 23 first (or in the same PR series) so 21's content edits drop into the new drawer skeleton.

## Part A — Editor navbar: Preview + Start

**Today** (`CreateDashboard.tsx` ~line 119, 130): both buttons call `console.log("preview deck")` / `console.log("start showcase")`.

### A.1 — Preview

- Opens a full-screen modal (use the existing `useFullScreen()` + `Modal` patterns) rendering the deck as a participant would see it, **without** writes back to the server (this is read-only preview, not a live showcase).
- Internally reuse `frontend/src/pages/GamePage/PlayPage.tsx` element renderers in a "preview mode" prop that disables network calls.
- The preview pulls live state from the `getDeck` cache — no extra fetch.
- Keyboard: `Esc` exits (the `LayoutProvider` global ESC already does this); ← / → step through slides.
- Add a top-bar in the preview with the deck title and a slide counter ("Slide 3 / 12").

### A.2 — Start (showcase)

- Opens the "Create showcase" flow. There is already a `useCreateShowcaseMutation` and a `/showcase/$showcaseId/host` route — wire the button to mint a new showcase, navigate to it, and let the existing lobby take over.
- If the deck has no elements, show a `Toast` ("Add at least one slide before starting a showcase") and don't navigate.
- Show a loading state on the button while the mutation is in flight (existing `Btn` `loading` prop).

### A.3 — Tests

- Unit test for the disabled state ("no elements → cannot start").
- Component test for "click Preview → modal opens; press Esc → modal closes".

## Part B — First-slide skeleton in the left sidebar

**Today:** `LeftSidebar.tsx` renders `deck.elements.map(...)`. A brand-new deck has `elements: []`, so the rail is empty and the canvas shows the "no slide selected" empty-state.

### B.1 — Skeleton tile

- When `deck.elements.length === 0`, render a single skeleton `SlideThumbnail` styled like the existing tile but with placeholder content (use the existing `Skeleton` component from `Common/Skeleton`).
- The skeleton is **non-interactive** (no drag handle, no right-click menu) and visually communicates "your first slide goes here".
- Clicking the skeleton opens the same `NewElementPicker` modal that the "+ New slide" button opens.

### B.2 — Empty-state in the canvas

- The center pane currently shows a generic empty state. Replace with copy that points at the picker: *"Pick a slide type from the left rail or hit ⌘N"*.
- This is a `<EmptyState>` re-use, not a new component.

### B.3 — Tests

- New-deck route test: navigate to `/decks/$id/view` with an empty deck → expect one skeleton tile.
- Click the skeleton → expect the `NewElementPicker` modal to open.

## Part C — Right sidebar: vertical icon rail + drawer-on-drawer

This is the largest piece. The target IA:

```
                                  ┌──────────────┬───┐
                                  │              │ ✎ │  ← Edit slide (pencil)
                                  │   drawer     │ 🎨│  ← Theme (palette)
                                  │   content    │ 👥│  ← Participant settings (users)
                                  │              │ ↗ │  ← Sharing preferences (share)
                                  └──────────────┴───┘
```

The rail is always visible. Clicking an icon opens its drawer to the left of the rail. Clicking the active icon (or pressing Esc) collapses the drawer. Only one drawer is open at a time.

### C.1 — New shell component

- `frontend/src/components/CreateDashboard/RightSidebar/RightSidebarRail.tsx` — the vertical icon column. ARIA: `nav aria-label="Editor panels"`, each icon is a `<button aria-pressed={...}>` with a `Tooltip`.
- `RightSidebarDrawer.tsx` — the slide-out container that hosts the active panel. Animates in/out (CSS transform; reduced-motion-friendly).
- `RightSidebarContent.tsx` becomes a thin orchestrator: tracks `openPanelId`, renders `<Rail />` + `<Drawer panelId={openPanelId} />`.
- Keep the existing module-CSS approach (`RightSidebarContent.module.css`).

### C.2 — Existing panel migrations

Map the existing panels to the new rail entries:

- **Edit slide (pencil)** → `EditSlidePanel.tsx` (no content change here — chunk 21 handles content).
- **Theme (palette)** → `ThemePanel.tsx` (already exists). Confirm it still works when hosted in the drawer.
- **Deck categorize (`DeckCategorizePanel`) / Reviews (`DeckReviewsPanel`) / Discussion (`DeckDiscussionPanel`)** — these are deck-meta panels that don't fit the four authoring-time icons in the target IA. Move them under a separate **Deck info** icon (book / info-circle) — fifth rail entry, OR relocate to a "Deck settings" modal accessed from the editor navbar. **Decide before implementing.** Document the decision in this README before coding.

### C.3 — New panels

#### Participant settings (`users` icon)

- New component: `RightSidebar/ParticipantSettingsPanel.tsx`.
- Scope is intentionally undecided in the original `todo.md`. First-pass content (subject to product input):
  - List collaborators on the deck (chunk 06 — `DeckCollaborator`). Show role, allow owner to add/remove.
  - Toggle for "Allow collaborators to edit slides" vs "Comment only".
- If chunk 06 has not landed yet, ship a placeholder panel with copy *"Collaborator management lands with chunk 06 — Deck collaborators"* and a disabled "+ Add collaborator" button.

#### Sharing preferences (`share` icon)

- New component: `RightSidebar/SharingPreferencesPanel.tsx`.
- Surfaces the existing `Deck.publishStatus` / `Deck.license` fields (chunk 02). Toggle public/unlisted/private, license picker, copy-to-clipboard for the public URL.
- If the deck is private, show a `Banner` (chunk 22's `Alert`) explaining that the share URL won't work until publish.

### C.4 — Keyboard + accessibility

- `Esc` closes the active drawer.
- Tab order goes rail → drawer content. Active icon has `aria-pressed="true"`.
- Each drawer header has a heading (`<h2>`), and the drawer container has `role="region" aria-labelledby={headingId}`.

### C.5 — Tests

- Click each rail icon → corresponding panel opens. Click again → closes.
- Press `Esc` while a drawer is open → drawer closes; focus returns to the rail icon.
- `ParticipantSettingsPanel` renders the placeholder copy when chunk 06 isn't wired yet.

## Cross-cutting concerns

- **Coordinate with chunk 21.** That chunk modifies content inside `EditSlidePanel`, `DeckCategorizePanel`, `DeckReviewsPanel`. This chunk modifies the shell. Land 23 first so 21 plugs into the new drawer container. If 21 lands first, 23 will have to refactor 21's recently-touched files — avoid that ordering.
- **Don't introduce new state libraries.** Track `openPanelId` with `useState` inside `RightSidebarContent` — no Redux, no context.
- **Tokens** — every new piece uses `tokens.css`. The rail's hairline divider uses `--edge-*`, never a hardcoded color. No `box-shadow` for elevation — surface ladder + matching `--edge-*` hairline. See [feedback_no_box_shadow](../../../.claude/projects/-home-balooski-Repos-brainflex/memory/feedback_no_box_shadow.md) and [feedback_border_discipline](../../../.claude/projects/-home-balooski-Repos-brainflex/memory/feedback_border_discipline.md).
- **Icons** — pull from `frontend/src/assets/icons/` per [ICONS-RULES](../../rules/ICONS-RULES.md). Don't reach for heroicons on new surfaces.
- **No `window.alert` / `window.confirm`.** Use `Modal` or `ConfirmDialog`.

## Checklist

### Part A — Navbar

- [ ] Preview button opens a full-screen read-only preview modal
- [ ] Preview supports arrow-key slide navigation + Esc close
- [ ] Start button creates a showcase and navigates to the host route
- [ ] Start button disabled (or Toast on click) when deck has no elements
- [ ] Button-level tests for both buttons

### Part B — Left sidebar

- [ ] New-deck flow renders a single non-interactive skeleton slide tile
- [ ] Clicking the skeleton opens `NewElementPicker`
- [ ] Center pane empty-state copy updated to point at the picker / shortcut
- [ ] Test for empty-deck → skeleton tile present

### Part C — Right sidebar

- [ ] Decision documented for where Deck categorize / Reviews / Discussion live in the new IA
- [ ] `RightSidebarRail.tsx` + `RightSidebarDrawer.tsx`
- [ ] `RightSidebarContent.tsx` refactored to orchestrate rail + drawer
- [ ] Edit slide and Theme drawers wired to existing panels
- [ ] `ParticipantSettingsPanel.tsx` (real or placeholder)
- [ ] `SharingPreferencesPanel.tsx`
- [ ] Esc closes the active drawer + focus restoration
- [ ] Tests for rail toggling, Esc behavior, placeholder copy

### Cross-cutting

- [ ] Frontend lint + tests pass
- [ ] No new `box-shadow` rules; no hardcoded color values
- [ ] Icons sourced from `frontend/src/assets/icons/`
