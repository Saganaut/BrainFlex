import { Container } from "@/components/Containers/Container";
import styles from "./SessionBoard.module.css";

interface SessionBoardProps {
  className?: string;
}

const SessionBoard = ({ className }: SessionBoardProps) => {
  return (
    <Container
      name={"SessionBoard"}
      className={` ${styles.sessionBoard} ${className}`}></Container>
  );
};

export { SessionBoard };
