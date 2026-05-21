import { useSession } from "@/pages/SessionPage/useSession";
import styles from "./SessionRoundTracker.module.css";
const SessionRoundTracker = () => {
  const { interactiveSession, currentDeck } = useSession();
  console.log("current deck", currentDeck);
  return (
    <div className={styles.sessionRoundTracker}>
      {currentDeck.elements?.map((element) => {
        return <div key={element.id}>{element.id}</div>;
      })}
    </div>
  );
};

export { SessionRoundTracker };
