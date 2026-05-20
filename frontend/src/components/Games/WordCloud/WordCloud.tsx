/**
 * Renders an aggregated word cloud from a {word: count} map.
 *
 * No external dependencies — we render each word as a `<span>` whose font
 * size is derived from its count relative to the round's max. The simplest
 * legible cloud: large, frequent words on top; smaller, rarer ones below.
 * Words sort by count descending so the tallest entries always land first.
 *
 * The cloud is purely a function of `counts`; the live broadcast on
 * /topic/interactive-session/{roomCode}/wordCloud drives re-renders during SUBMIT and
 * one final time on REVEAL.
 */
import styles from "./WordCloud.module.css";

interface WordCloudProps {
  counts: Record<string, number>;
}

// Five tiers from the smallest visible size up to the largest. The tier index
// scales linearly with count / maxCount, so a unanimous word renders at the
// top tier and an outlier still gets its smallest visible chip.
const TIERS = [
  styles.tier1,
  styles.tier2,
  styles.tier3,
  styles.tier4,
  styles.tier5,
];

const WordCloud = ({ counts }: WordCloudProps) => {
  const entries = Object.entries(counts);
  if (entries.length === 0) {
    return (
      <div className={styles.empty} aria-live='polite'>
        Waiting for the first submission…
      </div>
    );
  }

  const maxCount = entries.reduce((m, [, c]) => Math.max(m, c), 0);
  const sorted = [...entries].sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]));

  return (
    <div className={styles.cloud} role='img' aria-label='Word cloud of submissions'>
      {sorted.map(([word, count]) => {
        // ratio in [0, 1]; map to tier index in [0, TIERS.length - 1].
        const ratio = maxCount > 0 ? count / maxCount : 0;
        const tierIdx = Math.min(
          TIERS.length - 1,
          Math.floor(ratio * TIERS.length),
        );
        return (
          <span
            key={word}
            className={`${styles.word} ${TIERS[tierIdx]}`}
            title={`${word} (${String(count)})`}>
            {word}
          </span>
        );
      })}
    </div>
  );
};

export { WordCloud };
