import styles from "./SessionBoard.module.css";

interface SessionBoardProps {
  className?: string;
}

const SessionBoard = ({ className }: SessionBoardProps) => {
  return (
    <div className={` ${styles.sessionBoard} ${className}`}>
      Session Board Control
    </div>
  );
};

export { SessionBoard };
