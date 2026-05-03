import { Link } from "@tanstack/react-router";
import styles from "./DesignSystem.module.css";

const colorPalette = [
  {
    label: "Azure",
    tokens: [
      "--azure-200",
      "--azure-300",
      "--azure-400",
      "--azure-500",
      "--azure-600",
      "--azure-700",
      "--azure-800",
      "--azure-900",
    ],
  },
  {
    label: "Red",
    tokens: [
      "--red-200",
      "--red-300",
      "--red-400",
      "--red-500",
      "--red-600",
      "--red-700",
      "--red-800",
      "--red-900",
    ],
  },
  {
    label: "Yellow",
    tokens: [
      "--yellow-200",
      "--yellow-300",
      "--yellow-400",
      "--yellow-500",
      "--yellow-600",
      "--yellow-700",
      "--yellow-800",
      "--yellow-900",
    ],
  },
  {
    label: "Green",
    tokens: [
      "--green-200",
      "--green-300",
      "--green-400",
      "--green-500",
      "--green-600",
      "--green-700",
      "--green-800",
      "--green-900",
    ],
  },
  {
    label: "Turqoise",
    tokens: [
      "--turqoise-200",
      "--turqoise-300",
      "--turqoise-400",
      "--turqoise-500",
      "--turqoise-600",
      "--turqoise-700",
      "--turqoise-800",
      "--turqoise-900",
    ],
  },
  {
    label: "Cerulean",
    tokens: [
      "--cerulean-200",
      "--cerulean-300",
      "--cerulean-400",
      "--cerulean-500",
      "--cerulean-600",
      "--cerulean-700",
      "--cerulean-800",
      "--cerulean-900",
    ],
  },
  {
    label: "Blue",
    tokens: [
      "--blue-200",
      "--blue-300",
      "--blue-400",
      "--blue-500",
      "--blue-600",
      "--blue-700",
      "--blue-800",
      "--blue-900",
    ],
  },
  {
    label: "Lavender",
    tokens: [
      "--lavender-200",
      "--lavender-300",
      "--lavender-400",
      "--lavender-500",
      "--lavender-600",
      "--lavender-700",
      "--lavender-800",
      "--lavender-900",
    ],
  },
  {
    label: "Fuchsia",
    tokens: [
      "--fuchsia-200",
      "--fuchsia-300",
      "--fuchsia-400",
      "--fuchsia-500",
      "--fuchsia-600",
      "--fuchsia-700",
      "--fuchsia-800",
      "--fuchsia-900",
    ],
  },
  {
    label: "Mulberry",
    tokens: [
      "--mulberry-200",
      "--mulberry-300",
      "--mulberry-400",
      "--mulberry-500",
      "--mulberry-600",
      "--mulberry-700",
      "--mulberry-800",
      "--mulberry-900",
    ],
  },
  {
    label: "Flamingo",
    tokens: [
      "--flamingo-200",
      "--flamingo-300",
      "--flamingo-400",
      "--flamingo-500",
      "--flamingo-600",
      "--flamingo-700",
      "--flamingo-800",
      "--flamingo-900",
    ],
  },
];

const semanticTokens = [
  "--bg-canvas",
  "--bg-surface",
  "--bg-surface-raised",
  "--bg-subtle",
  "--bg-brand",
  "--text-primary",
  "--text-secondary",
  "--text-muted",
  "--text-on-brand",
  "--text-error",
  "--text-success",
  "--text-warning",
  "--text-info",
  "--border-default",
  "--border-subtle",
  "--border-focus",
  "--border-brand",
  "--action-hover",
  "--action-active",
  "--action-disabled",
  "--status-success",
  "--status-warning",
  "--status-error",
  "--status-info",
];

function ColorSwatch({ token }: { token: string }) {
  return (
    <div className={styles.colorSwatch}>
      <div
        className={styles.colorBox}
        style={{ background: `var(${token})` }}
      />
      <div className={styles.colorToken}>{token}</div>
    </div>
  );
}

function TokenRow({ token }: { token: string }) {
  return (
    <div className={styles.tokenRow}>
      <div>
        <div className={styles.tokenLabel}>{token}</div>
        <div className={styles.tokenVar}>{`var(${token})`}</div>
      </div>
      <div
        className={styles.tokenSample}
        style={{ background: `var(${token})` }}>
        sample
      </div>
    </div>
  );
}

export function DesignSystem() {
  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <div className={styles.sectionTitle}>Design System</div>
        <p className={styles.sectionDescription}>
          A quick view of the app palette, semantic token set, and simple
          component examples.
        </p>
        <Link to='/' className={styles.backLink}>
          Back to home
        </Link>
      </div>

      <div className={styles.card}>
        <div className={styles.sectionTitle}>Color palette</div>
        <div className={styles.paletteContainer}>
          {colorPalette.map((group) => (
            <div key={group.label} className={styles.paletteGroup}>
              <div className={styles.paletteGroupTitle}>{group.label}</div>
              <div className={styles.colorGrid}>
                {group.tokens.map((token) => (
                  <ColorSwatch key={token} token={token} />
                ))}
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className={styles.card}>
        <div className={styles.sectionTitle}>Semantic tokens</div>
        <div className={styles.tokensContainer}>
          {semanticTokens.map((token) => (
            <TokenRow key={token} token={token} />
          ))}
        </div>
      </div>

      <div className={styles.card}>
        <div className={styles.sectionTitle}>Example components</div>
        <div className={styles.examplesContainer}>
          <div className={styles.cardExample}>
            <div className={styles.cardExampleTitle}>Card surface</div>
            <div className={styles.cardExampleDescription}>
              Use surface palettes for card backgrounds, panels, and groups.
            </div>
          </div>

          <div className={styles.buttonGroup}>
            <button className={styles.buttonPrimary}>Primary action</button>
            <button className={styles.buttonSecondary}>Secondary action</button>
            <button className={styles.buttonDisabled} disabled>
              Disabled
            </button>
          </div>

          <div className={styles.badgeGroup}>
            <span className={`${styles.badge} ${styles.badgeSuccess}`}>
              Success
            </span>
            <span className={`${styles.badge} ${styles.badgeWarning}`}>
              Warning
            </span>
            <span className={`${styles.badge} ${styles.badgeError}`}>
              Error
            </span>
            <span className={`${styles.badge} ${styles.badgeInfo}`}>Info</span>
          </div>
        </div>
      </div>
    </div>
  );
}
