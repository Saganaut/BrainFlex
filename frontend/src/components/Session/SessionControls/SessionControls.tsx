import styles from "./SessionControls.module.css";

interface SessionControlsProps {
  className?: string;
}

const SessionControls = ({ className }: SessionControlsProps) => {
  return (
    <div className={` ${styles.sessionControls} ${className}`}>
      session controls
    </div>
  );
};

export { SessionControls };
