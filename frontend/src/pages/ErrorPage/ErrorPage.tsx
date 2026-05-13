import { type ReactNode } from "react";
import { Link } from "@tanstack/react-router";
import styles from "./ErrorPage.module.css";
import { Btn } from "@/components/Common/Buttons/Btn";
import LostFish from "@/assets/message/LostFish.svg";
import SleepyCeph from "@/assets/message/SleepyCeph.svg";
import OceanFloor from "@/assets/message/OceanFloor.svg";

interface ErrorPageProps {
  statusCode: number;
  title: string;
  message: string;
  image?: ReactNode;
}

const ERROR_CONFIGS: Record<
  number,
  { title: string; message: string; imgStr: string }
> = {
  404: {
    title: "Page not found",
    message: "Looks like this corner of BrainFlex doesn't exist.",
    imgStr: OceanFloor,
  },
  500: {
    title: "Internal server error",
    message: "Something broke on our end.  Give it a moment and try again.",
    imgStr: SleepyCeph,
  },
  503: {
    title: "Service unavailable",
    message: "BrainFlex is taking a quick breather.",
    imgStr: LostFish,
  },
};

const ErrorPage = ({ statusCode, title, message, image }: ErrorPageProps) => {
  return (
    <main className={styles.page}>
      <p className={styles.code}>{statusCode}</p>
      <div className={styles.mascot}>
        {image ?? (
          <img
            src={ERROR_CONFIGS[statusCode].imgStr}
            alt='Error illustration'
          />
        )}
      </div>
      <h1 className={styles.title}>{title}</h1>
      <p className={styles.message}>{message}</p>
      <div className={styles.actions}>
        <Link to='/' className={styles.homeLink} viewTransition>
          Take me home
        </Link>
        <Btn
          className={styles.backBtn}
          onClick={() => {
            history.back();
          }}>
          Go back
        </Btn>
      </div>
    </main>
  );
};

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

export { ErrorPage };
