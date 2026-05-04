import { type ReactNode } from "react";
import { Link } from "@tanstack/react-router";
import { BrainMascot } from "./BrainMascot";
import styles from "./ErrorPage.module.css";

interface ErrorPageProps {
  statusCode: number;
  title: string;
  message: string;
  image?: ReactNode;
}

const ERROR_CONFIGS: Record<number, { title: string; message: string }> = {
  404: {
    title: "Page not found",
    message: "Looks like this corner of BrainFlex doesn't exist — or your brain led you somewhere it shouldn't.",
  },
  500: {
    title: "Internal server error",
    message: "Something broke on our end. Our neurons are misfiring. Give it a moment and try again.",
  },
  503: {
    title: "Service unavailable",
    message: "BrainFlex is taking a quick breather. We'll be back before you can say 'hippocampus'.",
  },
};

export function ErrorPage({ statusCode, title, message, image }: ErrorPageProps) {
  return (
    <main className={styles.page}>
      <p className={styles.code}>{statusCode}</p>
      <div className={styles.mascot}>{image ?? <BrainMascot className={styles.mascotSvg} />}</div>
      <h1 className={styles.title}>{title}</h1>
      <p className={styles.message}>{message}</p>
      <div className={styles.actions}>
        <Link to="/" className={styles.homeLink}>Take me home</Link>
        <button type="button" className={styles.backBtn} onClick={() => history.back()}>Go back</button>
      </div>
    </main>
  );
}

export function NotFoundPage() {
  const cfg = ERROR_CONFIGS[404];
  return <ErrorPage statusCode={404} title={cfg.title} message={cfg.message} />;
}

export function ServerErrorPage() {
  const cfg = ERROR_CONFIGS[500];
  return <ErrorPage statusCode={500} title={cfg.title} message={cfg.message} />;
}

export function ServiceUnavailablePage() {
  const cfg = ERROR_CONFIGS[503];
  return <ErrorPage statusCode={503} title={cfg.title} message={cfg.message} />;
}
