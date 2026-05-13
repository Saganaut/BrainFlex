# Frontend Style Rules

Short rules. See `frontend/STYLES.md` for the full explanation, examples, and migration status.

## File & class structure

- Use CSS Modules; files end in `*.module.css`.
- Class names are lowerCamelCase. Stylelint enforces this.
- No global styles. The only exception is the modifier rules in `tokens.css` (theme classes + `data-*` selectors).
- Each module has a small number of top-level classes with sub-classes nested inside.
- Use container queries for responsive layout — not media queries on the viewport.
- Never translate a component's position on hover unless it serves a specific purpose.

## Tokens

- Colors, spacing, and typography come from tokens in `frontend/src/tokens.css`. Never hardcode values.
- Tokens cascade through theme classes (`.theme-dark`, `.theme-custom`, `.theme-custom.theme-dark`, default light). All four states must keep working — don't touch theme classes from a component module.

## Component variable manifest

Every variantizable component exposes a manifest of local CSS vars and reads from them. Modifiers in `tokens.css` flip those vars.

- **Color slots:** `--color`, `--background-color`, `--border-color` (flipped by `[data-variant]`).
- **Layout slots:** `--padding`, `--gap`, `--radius`, optionally `--height` / `--width` / `--border-width` (flipped by `[data-size]`).
- **Typography slots:** `--font-size`, optionally `--font-weight` / `--line-height` (flipped by `[data-size]`).
- **State slot:** `--opacity`.

Components opt into only the slots they need.

## The `var(--name, fallback)` rule (load-bearing)

- **Never declare a manifest var inside the component rule.** Read it with the fallback syntax:

  ```css
  /* correct */
  .btn {
    color: var(--color, var(--text-primary));
    padding: var(--padding, var(--p-md));
  }

  /* wrong — this kills every modifier silently */
  .btn {
    --color: var(--text-primary);
    color: var(--color);
  }
  ```

- Why: `.btn` and `[data-variant="error"]` have equal specificity. Component CSS imports after `tokens.css`, so a local declaration would always win. The fallback pattern means the component never declares the var — modifiers set it; otherwise the fallback applies.

## Modifier vocabulary

Apply modifiers as `data-*` attributes on the element. Type them with the shared `BtnVariant` / `BtnSize` / `BtnMode` aliases from `components/Common/Buttons/BtnTypes.ts`.

- `data-variant` — `default | error | success | warning | info | brand`. Sets the color triple from matched semantic tokens (guaranteed contrast).
- `data-size` — `xs | sm | md | lg`. Sets padding, font size, radius, gap.
- `data-mode` — `outline | ghost`. Composes with `data-variant`; flips background/border without touching color.
- Native state — use `:disabled`, `:hover`, `[aria-pressed]`, `[aria-selected]`, etc. Don't invent state classes.

Component-local concepts (shape, slot positioning, hover-color sextet) stay as module classes — they're not portable, so they don't belong in the global modifier set.

## When to do what

1. Recolor → `data-variant`.
2. Resize → `data-size`.
3. Fill / outline / ghost → `data-mode`.
4. Native interactive state → platform pseudo-class / aria attribute.
5. Component-specific layout (pill shape, custom slot) → a module class on the component.

If a change doesn't fit any of those, raise it before inventing a one-off pattern.

## Accessibility & semantics

- Use semantic HTML elements. Components must support keyboard navigation and carry the right ARIA attributes.
- No `window.alert` / `window.prompt` / `window.confirm`. Use the shared modal / popover patterns.

## Lint

- Stylelint and ESLint must pass before commit. The pre-commit hook runs `lint:all`.
