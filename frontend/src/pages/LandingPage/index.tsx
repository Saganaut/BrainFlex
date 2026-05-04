import { Link } from "@tanstack/react-router";
import styles from "./LandingPage.module.css";

const FEATURES = [
  {
    icon: "⚡",
    title: "Compete in Real Time",
    desc: "Race against players worldwide in fast-paced mental challenges. Every second counts.",
  },
  {
    icon: "🧠",
    title: "Train Your Mind",
    desc: "Daily puzzles, pattern recognition, and memory drills that adapt to your skill level.",
  },
  {
    icon: "🏆",
    title: "Climb the Ranks",
    desc: "A global leaderboard tracks your total points, streaks, and highest scores.",
  },
];

const STATS = [
  { number: "10K+", label: "Active Players" },
  { number: "50+", label: "Brain Games" },
  { number: "Daily", label: "Challenges" },
];

export function LandingPage() {
  return (
    <>
      <section className={styles.hero}>
        <div className={styles.heroGlow} />
        <p className={styles.eyebrow}>competitive brain training</p>
        <h1 className={styles.heroTitle}>BrainFlex</h1>
        <p className={styles.heroTagline}>
          Train your mind. Beat the clock. Own the leaderboard.
        </p>
        <div className={styles.heroActions}>
          <Link to='/' viewTransition className={styles.btnPrimary}>
            Play Now
          </Link>
          <Link to='/about' viewTransition className={styles.btnSecondary}>
            Learn More
          </Link>
        </div>
      </section>

      <div className={styles.stats}>
        {STATS.map((s) => (
          <div key={s.label} className={styles.statItem}>
            <span className={styles.statNumber}>{s.number}</span>
            <span className={styles.statLabel}>{s.label}</span>
          </div>
        ))}
      </div>

      <section className={styles.features}>
        <p className={styles.sectionLabel}>why brainflex</p>
        <h2 className={styles.sectionTitle}>Built for competitors</h2>
        <div className={styles.featureGrid}>
          {FEATURES.map((f) => (
            <div key={f.title} className={styles.featureCard}>
              <span className={styles.featureIcon}>{f.icon}</span>
              <h3 className={styles.featureTitle}>{f.title}</h3>
              <p className={styles.featureDesc}>{f.desc}</p>
            </div>
          ))}
        </div>
      </section>

      <section className={styles.cta}>
        <h2 className={styles.ctaTitle}>Ready to flex?</h2>
        <p className={styles.ctaSub}>
          Join thousands of players and start training today.
        </p>
        <div className={styles.ctaActions}>
          <Link to='/' viewTransition className={styles.btnPrimary}>
            Get Started — it&apos;s free
          </Link>
        </div>
      </section>
    </>
  );
}
