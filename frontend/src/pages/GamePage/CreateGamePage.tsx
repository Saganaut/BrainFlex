import { useNavigate, Link } from "@tanstack/react-router";
import { useState } from "react";
import { ContentPackPicker } from "../../components/Games/ContentPackPicker/ContentPackPicker";
import { useCurrentUser } from "../../hooks/useCurrentUser";
import { useCreateGameMutation } from "../../store/BrainFlexApi";
import styles from "./Game.module.css";
const CreateGamePage = () => {
  const navigate = useNavigate();
  const { authenticated, isLoading: authLoading } = useCurrentUser();
  const [selectedPackId, setSelectedPackId] = useState<string | null>(null);
  const [totalRounds, setTotalRounds] = useState(10);
  const [timePerQuestion, setTimePerQuestion] = useState(15);
  const [speedBonus, setSpeedBonus] = useState(true);
  const [createGame, { isLoading, error }] = useCreateGameMutation();

  if (!authLoading && !authenticated) {
    return (
      <div className={styles.page}>
        <p className={styles.authMsg}>
          You must be signed in to create a game.
        </p>
        <Link to='/games' className={styles.backLink}>
          Back to hub
        </Link>
      </div>
    );
  }

  const handleSubmit = async (e: React.SubmitEvent) => {
    e.preventDefault();
    if (!selectedPackId) return;
    try {
      const session = await createGame({
        createGameRequest: {
          contentPackId: selectedPackId,
          totalRounds,
          timePerQuestion,
          speedBonus,
          gameMode: "SIMULTANEOUS",
        },
      }).unwrap();
      console.log("Session", session);
      if (session.roomCode) {
        await navigate({
          to: "/games/$roomCode/lobby",
          params: { roomCode: session.roomCode },
        });
      }
    } catch (e) {
      // error shown via `error` state
      console.error("error", e);
    }
  };

  return (
    <div className={styles.page}>
      <h1 className={styles.title}>Create Game</h1>

      <form className={styles.form} onSubmit={(e) => void handleSubmit(e)}>
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Choose a Content Pack</h2>
          <ContentPackPicker
            selectedPackId={selectedPackId}
            onSelect={setSelectedPackId}
          />
        </section>

        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Settings</h2>
          <div className={styles.settings}>
            <label className={styles.setting}>
              <span>Rounds</span>
              <input
                type='number'
                min={3}
                max={30}
                value={totalRounds}
                onChange={(e) => {
                  setTotalRounds(Number(e.target.value));
                }}
                className={styles.numberInput}
              />
            </label>
            <label className={styles.setting}>
              <span>Seconds per question</span>
              <input
                type='number'
                min={5}
                max={60}
                value={timePerQuestion}
                onChange={(e) => {
                  setTimePerQuestion(Number(e.target.value));
                }}
                className={styles.numberInput}
              />
            </label>
            <label className={styles.setting}>
              <span>Speed bonus</span>
              <input
                type='checkbox'
                checked={speedBonus}
                onChange={(e) => {
                  setSpeedBonus(e.target.checked);
                }}
              />
            </label>
          </div>
        </section>

        {error && (
          <p className={styles.errorMsg}>
            Failed to create game. Please try again.
          </p>
        )}

        <button
          type='submit'
          className={styles.createBtn}
          disabled={!selectedPackId || isLoading}>
          {isLoading ? "Creating…" : "Create Game"}
        </button>
      </form>

      <Link to='/games' className={styles.backLink}>
        Back to hub
      </Link>
    </div>
  );
};

export { CreateGamePage };
